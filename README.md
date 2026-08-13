# Nemesis AI

**Nemesis AI** is a Minecraft Java Edition 1.21.1 Fabric mod built around an enemy that changes with the player instead of following one fixed combat script. Nemesis observes repeated combat habits, switches tactics, remembers movement regions, and predicts ambush points along familiar routes. Its parasite lifecycle adds a second gameplay loop: adults infect farm animals, babies emerge and hunt, and well-fed babies mature into full adaptive Nemeses.

Created for **The Orchestra Agent Orchestrator Hackathon**, the project combines adaptive AI, combat, persistence, networking, HUD feedback, and GeckoLib presentation in one demonstrable hostile mob.

## Key features

- **Adaptive combat learning:** detects repeated melee and projectile attacks, shield use, retreating, ranged play, and tower behavior.
- **Five live combat tactics:** normal melee, fast pursuit, delayed strikes, zigzag movement, and ranged slowing projectiles.
- **Predictive ambushes:** observes player movement from a distance, learns repeatedly visited regions, and moves ahead of the route.
- **Entity persistence:** learned counters, resistances, habit weights, selected tactic, route cells, ambush state, and lifecycle state use NBT.
- **Parasite reproduction:** adults infect only cows, horses, pigs, sheep, and chickens; infected hosts produce Baby Nemeses after about 15 seconds.
- **Baby growth:** babies prioritize valid prey, count their own supported-animal kills, and become the existing adult entity after five meals.
- **Player feedback:** server-to-client learning events drive an adaptation bar and `LEARNED:` / `TACTIC CHANGED:` messages.
- **Custom presentation:** GeckoLib model, textures, and nine adult animation clips, plus a distinct scaled juvenile.
- **Developer controls:** operator commands cover spawning, inspection, tactic selection, simulated learning, and memory reset.

## Adaptive learning system

Learning is server-authoritative and implemented in `NemesisEntity`, with small API, networking, and HUD layers around it.

| Observation | Detection | Response |
| --- | --- | --- |
| Repeated direct attacks | Three successful non-projectile hits | Enables melee adaptation, halves later player melee damage, selects `DELAYED_ATTACK` |
| Repeated projectile attacks | Three successful projectile hits | Enables ranged adaptation, reduces later player projectile damage to 65%, selects `ZIGZAG_APPROACH` |
| Shield blocking | A blocking server player is struck by Nemesis | Adds weight toward `DELAYED_ATTACK` |
| Ranged attacks | Nemesis is hit by a projectile | Adds weight toward `ZIGZAG_APPROACH` |
| Retreating | Observed distance grows by more than 1.5 blocks between habit samples | Adds weight toward `FAST_CHASE` |
| Towering | Player is over three blocks above Nemesis while navigation is stuck and melee is out of range | Adds weight toward `RANGED_ATTACK` |

Habit weights gain `1.0` per observation, are capped at `10.0`, and decay by multiplying by `0.97` on each 20-tick observation cycle. A habit can change the tactic once its weight is **greater than 3.0**. The dominant eligible habit selects its counter-tactic and produces a `LearningResult` with a tactic, human-readable reason, and aggregate adaptation level.

The latest result is also held in a per-player in-memory store for command/HUD integration. That store is session-only; durable learning lives on each Nemesis entity through NBT.

There is no separate “What Nemesis Learned” screen. The implemented inspection surfaces are the HUD/messages and `/nemesis memory`, which reports the nearest adult's hit counters, learned resistances, tactic, route confidence, and ambush point.

## Combat tactics

The learning engine and developer commands both use `setTactic(...)`.

| Tactic | Implemented behavior |
| --- | --- |
| `NORMAL` | Standard pursuit and melee attacks inside the 50-block combat zone. |
| `FAST_CHASE` | Normal melee with base movement speed raised from `0.25` to `0.38` and sprinting enabled. |
| `DELAYED_ATTACK` | Approaches normally, then waits a randomized 8–14 tick wind-up before striking. A hit applies a 30-tick tactic cooldown. |
| `ZIGZAG_APPROACH` | Alternates lateral waypoints while closing distance, then attacks in melee with a 20-tick cooldown. |
| `RANGED_ATTACK` | Retreats within 6 blocks, closes beyond 12, and fires every 40 ticks with line of sight. Blue slime projectiles deal 5 damage and apply Slowness II for 80 ticks. |

All tactics remain part of the same adult entity; adaptation does not swap in a second combat implementation.

## Adult Nemesis entity

The adult is a custom monster built on Minecraft's zombie infrastructure and registered as `nemesis_ai:nemesis`.

- Targets the nearest player with a 100-block follow range.
- Has 100 maximum health and a `0.9 × 2.75` entity size.
- Chooses direct combat or route observation according to distance.
- Supports melee, delayed, zigzag, chase, and ranged goals.
- Uses a custom blue slime projectile and item representation for ranged attacks.
- Seeks valid animal hosts to begin the parasite lifecycle.
- Uses Warden/Ravager-based hostile sounds.
- Uses custom GeckoLib geometry and texture with idle, walk, run, claw, bite, hurt, death, melee-adaptation, and ranged-adaptation clips.

## Predictive ambush system

- **0–50 blocks:** direct tactic-driven combat.`n- **50–100 blocks:** route observation and stalking.`n- **Beyond 100 blocks:** outside the configured follow/observation range.

The gameplay loop is:

1. Player position is sampled every **10 ticks** (about 0.5 seconds) in the observation band.
2. A new sample is accepted after at least **4 blocks** of movement (`distance² >= 16`).
3. Samples are grouped into **8 × 8 horizontal cells**.
4. Three visits to a cell establish enough route confidence for an ambush.
5. Nemesis derives movement direction from consecutive accepted samples and places an ambush point about **20 blocks ahead**.
6. It moves there and can wait for **600 ticks** (about 30 seconds).
7. When the player comes within **3.5 blocks**, Nemesis attacks and clears the ambush.

Before a route is confirmed, stalking maintains roughly **58–85 blocks**: Nemesis retreats toward 70 blocks if too close, approaches a 72-block watch position if too far away, and shifts sideways inside the band.

Route cell counts, confidence, active ambush point, and remaining wait time are saved in NBT. The transient last sample is not saved, so sampling restarts after reload while learned route data remains.

## Parasite lifecycle

`Adult → bite → infected animal → 300-tick incubation → Baby → five meals → Adult`

### Infection

An adult without a parasite cooldown scans **16 blocks** for the nearest living, uninfected supported host and approaches it. Valid hosts are exactly:

- Cow
- Horse
- Pig
- Sheep
- Chicken

At melee range, the adult triggers its bite animation and infects the animal without killing it. Damage-indicator/smoke particles and a low spider sound show the bite. The adult then receives a **400-tick (20-second)** cooldown. A positive per-animal incubation timer prevents duplicate infection.

### Incubation and emergence

Each host owns an independent **300-tick (approximately 15-second)** server timer. Smoke appears every 20 ticks. At expiry, the server creates a baby at the host, emits explosion/damage particles, plays a Warden emergence sound, and attributes lethal damage to the baby. The emergence host therefore **counts as the baby's first meal**.

### Baby Nemesis and growth

Baby Nemesis is a separate hostile entity with 14 health, 4 attack damage, `0.34` movement speed, and 32-block follow range. It prioritizes supported animals, then players. It uses normal navigation and fast melee bites.

The baby reuses the GeckoLib geometry and idle/run/bite animation clips, but has a distinct red-purple juvenile texture, a `0.55 × 0.8` hitbox, and renders at **42% of adult scale**.

Only kills caused by that baby count, and only the five supported animal types count as food. At **five meals**, it spawns the existing `NemesisEntity` at its location, plays explosion/soul-flame effects and a roar, then removes itself. The resulting adult has the complete existing learning, tactic, ambush, and reproduction systems.

### Lifecycle persistence

- Infected animals save remaining incubation as `NemesisParasiteTicks`.
- Adults save reproduction cooldown as `NemesisParasiteCooldown`.
- Babies save meal count as `NemesisAnimalsEaten`.

## HUD and player feedback

A server learning event is encoded as `LearningResultPayload` and sent through Fabric Networking. The client renders:

- a **NEMESIS ADAPTATION** bar from the normalized adaptation level;
- `LEARNED: <TACTIC>` for the first received tactic;
- `TACTIC CHANGED: <TACTIC>` when a later event selects a different tactic.

Fresh banner text lasts **100 client ticks**, is also sent to the action bar, and respects Minecraft's hide-GUI setting. Repeated melee/ranged learning additionally sends explicit melee/ranged learning and resistance messages.

## Commands

All `/nemesis` commands require permission level 2. Commands that locate an adult search within 100 blocks.

| Command | Behavior |
| --- | --- |
| `/nemesis summon` | Spawns an adult two blocks from the player. |
| `/nemesis memory` | Reports the nearest adult's learning counters, resistances, tactic, route confidence, and ambush point. |
| `/nemesis resetmemory` | Clears the player's session result and resets the nearest adult's learning, route, ambush, resistance, habit, and tactic state. |
| `/nemesis tactic normal` | Forces `NORMAL`. |
| `/nemesis tactic fast_chase` | Forces `FAST_CHASE`. |
| `/nemesis tactic delayed_attack` | Forces `DELAYED_ATTACK`. |
| `/nemesis tactic zigzag_approach` | Forces `ZIGZAG_APPROACH`. |
| `/nemesis tactic ranged_attack` | Forces `RANGED_ATTACK`. |
| `/nemesis learn <tactic>` | Simulates a 50%-adaptation event for any tactic name above; useful for HUD testing. |

Vanilla summon IDs also work:

```mcfunction
/summon nemesis_ai:nemesis
/summon nemesis_ai:baby_nemesis
```

## Persistence summary

The following survives entity/world saves:

- adult melee/ranged hit counters and learned resistance flags;
- selected tactic and decaying habit weights;
- route cells/counts, route confidence, ambush point, and wait time;
- adult parasite cooldown;
- infected-animal incubation timers;
- baby meal counts.

The per-player `NemesisMemoryStore` and client HUD's latest event are in-memory helpers and **do not** survive server/client restart.

## Architecture

```text
com.sen2x.nemesisai
├── api/          Tactic, LearningResult, feedback bridge, session store
├── command/      Spawn, inspection, tactic, learning, and reset tools
├── entity/       Adult, Baby, and blue slime projectile
├── parasite/     Host validation, infection contract, lifecycle
├── network/      Server-to-client learning payload
├── mixin/        Persistent per-animal infection state/ticking
└── client/       GeckoLib models/renderers and adaptation HUD
```

The adult goal set coordinates combat, habits, route learning, ambushing, and reproduction. Animal and baby lifecycle concerns remain in dedicated modules, while growth deliberately returns to the single existing adult implementation.

## Technology

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.19.3**
- Fabric API **0.116.15+1.21.1**
- Java **21** target
- Fabric Loom **1.17**
- Gradle Wrapper **9.5.1**
- Official Mojang mappings
- GeckoLib **4.9.2**
- Git and GitHub
- GitHub Actions build/artifact workflow
- Agent Orchestrator and OpenAI Codex development tooling

> GitHub Actions currently runs on Ubuntu and provisions Java 25; the mod compiles for and requires Java 21 or newer.

## Running the project

### Prerequisites

- Java 21 JDK
- Git
- No system Gradle installation; use the included wrapper

### Build

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

The remapped jar is produced in `build/libs/`.

### Development client

Windows:

```powershell
.\gradlew.bat runClient
```

Linux/macOS:

```bash
./gradlew runClient
```

Create a world with cheats enabled, then use `/nemesis summon` or the **Nemesis Spawn Egg** in the creative Spawn Eggs tab.

## Testing

See [TESTING.md](TESTING.md) for the existing command, HUD, and scenario checklist. Some wording there predates completed integrations; the command table above reflects current source.

A concise hackathon demo:

1. Spawn an adult and demonstrate direct combat.
2. Repeatedly attack with melee/projectiles, or use `/nemesis learn <tactic>`, to show learning and HUD feedback.
3. Demonstrate delayed, zigzag, fast, and ranged tactics.
4. Observe Nemesis from 50–100 blocks while crossing the same area three times, then approach its predicted ambush.
5. Place a supported animal nearby and watch infection, incubation, and emergence.
6. Let the baby consume five supported animals and transform into the same adaptive adult.

## Current status

### Core, AI, and combat

- [x] Fabric 1.21.1 / Java 21 project
- [x] Adult and baby hostile entities
- [x] Five combat tactics and slowing ranged projectile
- [x] Melee/projectile adaptation and resistance
- [x] Weighted shield, ranged, flee, and tower habits
- [x] GeckoLib model, textures, renderers, and animation controllers
- [x] NBT persistence for adult learning state

### Ambush and lifecycle

- [x] Distance-based stalking and route sampling
- [x] Repeated-region learning and predicted ambush points
- [x] Persistent route/ambush state
- [x] Restricted host selection and duplicate-infection prevention
- [x] Independent persistent incubation
- [x] Server-authoritative emergence and host consumption
- [x] Baby hunting, growth persistence, and adult transformation

### HUD, testing, and presentation

- [x] Adaptation bar and learning/tactic messages
- [x] Operator inspection/test commands
- [x] GitHub Actions build workflow
- [x] Manual testing guide
- [ ] Final balancing and full multiplayer/end-to-end pass
- [ ] Screenshots and demo video
- [ ] Final Devpost/presentation polish

## Team

| Member | Focus | Contributions |
| --- | --- | --- |
| **Arsenijs (Arseniy)** | AI Learning Engine | Behavior tracking, habit detection, tactic selection, weighting/decay, and learning-state integration |
| **Luka** | Nemesis Gameplay | Adult entity, combat, tactics, movement, parasite lifecycle, Baby Nemesis, and growth |
| **Artem** | HUD, Ambush, Testing, Presentation | Adaptation HUD, learning messages, predictive route/ambush work, testing, documentation, and demo presentation |

The three-person team used Agent Orchestrator and OpenAI Codex as development workflow tools while keeping the game systems at the center of the project.

## License

Released under [LICENSE](LICENSE) (**CC0 1.0 Universal**).

Nemesis AI was created for **The Orchestra Agent Orchestrator Hackathon**.
