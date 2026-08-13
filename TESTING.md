# Testing Nemesis AI

There is now a single, real Nemesis (`dev.nemesis.entity.NemesisEntity`) and a single shared
`dev.nemesis.entity.Tactic` enum. It observes real combat (shield blocks, ranged hits, player
retreats) and auto-switches tactic; the HUD reacts to those real events over the network. The
`/nemesis learn` and `/nemesis tactic` commands are still there as manual overrides for demos
and debugging.

## Running a test world

```bash
./gradlew runClient
```

1. Create a new **Creative** world (Superflat or any flat-ish area is easiest to see the HUD against).
2. Enable cheats so the `/nemesis` commands (operator-only) work.
3. Open **Options -> Controls** and confirm the HUD isn't hidden (F1 toggles it off — leave it on).

## Commands

All commands live under `/nemesis` and require operator permission (level 2).

| Command | What it does |
| --- | --- |
| `/nemesis summon` | Spawns a real Nemesis entity a couple blocks away. |
| `/nemesis tactic <name>` | Forces the nearest Nemesis entity's combat tactic (`normal`, `fast_chase`, `delayed_attack`, `zigzag_approach`, `ranged_attack`) without touching its habit weights. |
| `/nemesis learn <name>` | Simulates a learning event straight to the HUD (server broadcasts a `LearningResult` without touching any entity) — useful for testing the HUD/message path in isolation. |
| `/nemesis resetmemory` | Clears the calling player's stub record *and* resets the nearest live Nemesis entity's habit weights/tactic back to `normal`. |

## How Nemesis actually learns (weighted habit profile)

`NemesisEntity` keeps a small habit profile: four float weights (shield-blocking,
ranged-attacking, retreating, building up out of reach), each starting at 0.

- Every time a behavior is observed, its weight gets **+1 reward** (capped at 10).
- Every second, all four weights **decay by 3%** — a habit the player stops repeating fades out
  instead of permanently locking Nemesis into a counter-tactic.
- After every reward, Nemesis re-evaluates: whichever habit has the **highest weight above 3.0**
  wins, and Nemesis switches to that habit's counter-tactic (if it isn't already using it) and
  tells the HUD why (`NemesisFeedback.broadcastLearning`).

| Player behavior | Detected in | Counter-tactic |
| --- | --- | --- |
| Blocks Nemesis's melee hits with a shield | `NemesisEntity#doHurtTarget` | `DELAYED_ATTACK` |
| Hits Nemesis with a ranged weapon (arrow, etc.) | `NemesisEntity#hurt` | `ZIGZAG_APPROACH` |
| Increases distance from Nemesis while being chased | `NemesisEntity#customServerAiStep` (every second) | `FAST_CHASE` |
| Is 3+ blocks above Nemesis with no reachable path (towering/turtling) | `NemesisEntity#customServerAiStep` (every second) | `RANGED_ATTACK` |

Because it's a weighted profile rather than a one-shot counter, a player who mixes habits (e.g.
blocks sometimes, shoots sometimes) makes Nemesis track whichever behavior is currently
*dominant*, and old habits genuinely fade if the player stops using them — closer to "learning"
than a simple tally.

All four weights and the current tactic are persisted via
`addAdditionalSaveData`/`readAdditionalSaveData`, so a Nemesis remembers a player's habits
across a server restart (as long as the chunk/entity isn't unloaded and discarded).

## Scenarios to verify

- [ ] Fresh-spawn a Nemesis, block 3 of its melee hits with a shield → HUD shows
      **"LEARNED: DELAYED ATTACK"**, Nemesis starts waiting out your guard instead of spamming hits.
- [ ] Shoot the same Nemesis with 3 arrows → HUD shows **"TACTIC CHANGED: ZIGZAG APPROACH"**,
      Nemesis starts approaching at an angle instead of walking straight at you.
- [ ] Let it chase you and back away 3 times (roughly a second apart, moving away each time) →
      **"TACTIC CHANGED: FAST CHASE"**, movement speed visibly increases.
- [ ] Retreat onto a pillar/tower 3+ blocks above Nemesis and stay out of its reach for a few
      seconds → **"TACTIC CHANGED: RANGED ATTACK"**, Nemesis starts shooting arrows at you.
- [ ] Stop repeating a habit (e.g. stop blocking) and keep fighting normally for ~30+ seconds →
      that habit's weight decays below the dominant one and Nemesis can switch away from it.
- [ ] `/nemesis resetmemory` near that Nemesis → its tactic goes back to `normal` and all habit
      weights reset to 0 (next shield block should trigger "LEARNED" again, not "TACTIC CHANGED").
- [ ] `/nemesis learn <name>` shows the HUD banner/action-bar message without needing to fight
      anything — useful to sanity-check the HUD alone.
- [ ] HUD does not draw anything before the first learning event; F1 (hide GUI) hides it too.

## Headless smoke test (verified)

`./gradlew runServer` was run once with `run/eula.txt` accepted and a flat/low-view-distance
`run/server.properties` (both are gitignored, throwaway dev files). Result: the mod initializes
cleanly and the world reaches full startup with no crash:

```
[main/INFO] (nemesis_ai) Nemesis AI initialized!
...
[Server thread/INFO] (Minecraft) Done (1.026s)! For help, type "help"
```

Two log lines are expected noise, not bugs:
- `No data fixer registered for nemesis:nemesis` — standard Minecraft warning for any modded
  entity ID with no registered NBT schema-migration entry; harmless for a mod that isn't shipping
  cross-version save migrations.
- The `Yggdrasil`/SSL certificate errors are this dev machine's proxy failing to reach Mojang's
  auth servers (`online-mode=true` default) — unrelated to the mod; set `online-mode=false` in
  `server.properties` for local testing to avoid the noise.

This only proves the mod *loads and the world boots* — it does not exercise combat, the HUD, or
the commands, since that needs a real connected player. The scenarios above still need an actual
in-game pass.

## Known follow-ups

- The tower-detection heuristic (height gap + unreachable path) is a proxy for "player built up
  out of reach," not literal block-place tracking — it can also fire if a player is simply stuck
  on natural terrain above Nemesis, which is arguably fine (still "can't reach me" from Nemesis's
  point of view) but worth knowing about.
- `NemesisMemoryStore` (`src/main/java/com/sen2x/nemesisai/api/NemesisMemoryStore.java`) is
  still just a stub per-player "last simulated result" cache used by `/nemesis learn`; the real
  memory now lives on the entity itself via NBT, which is the more correct place for it.
- `dev.nemesis.client.NemesisClient` (an unused duplicate `ClientModInitializer`, never
  registered in `fabric.mod.json`) has been deleted. `dev.nemesis.NemesisMod` looked like the
  same kind of dead entrypoint but isn't — `ModEntities` reads its `MOD_ID` constant ("nemesis")
  for the entity/spawn-egg registry IDs, so it's kept; only its unused `onInitialize()` is dead.
- A separate open PR (`agent/geckolib-nemesis-integration`) adds an animated GeckoLib model and
  its own third `NemesisEntity`/`ModEntities` variant in yet another package
  (`com.sen2x.nemesisai.entity`) — merging it will need the same kind of reconciliation this
  branch just did, pointing it at `dev.nemesis.entity.NemesisEntity`/`Tactic` instead of
  reintroducing a duplicate.
