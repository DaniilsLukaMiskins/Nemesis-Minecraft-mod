# Nemesis AI

Nemesis AI is an adaptive enemy mod for Minecraft Java Edition 1.21.1.

The enemy observes player combat tactics, remembers repeated behavior, and changes its strategy during later encounters.

## Planned features

- Adaptive combat behavior
- Persistent learning memory
- Shield, melee, ranged, and movement analysis
- HUD adaptation messages
- Testing and memory reset commands

## Technology

- Fabric Loader
- Fabric API
- Java 21
- Gradle

## Status

Initial Fabric project configured, built, and launched successfully.

## Parasite lifecycle test

1. Start a development client with `./gradlew runClient` and create a world with cheats.
2. Spawn an adult with `/summon nemesis_ai:nemesis`, then place a cow, horse, pig, sheep, or chicken nearby.
3. The adult approaches and infects one valid host. Smoke/damage particles mark the bite and continue during the 15-second incubation.
4. A Baby Nemesis emerges, kills the host, and prioritizes hunting the five supported animal types before players.
5. After five supported-animal kills (including its emergence host), the baby is replaced by the existing adult Nemesis entity, completing the repeatable lifecycle.

Infection timers and baby food counts are stored in entity NBT, so each host/baby is independent and survives world saves. Other mob types cannot be infected and do not count toward growth.

HUD, learning-event networking, and test/debug commands (`/nemesis summon`,
`/nemesis learn <tactic>`, `/nemesis resetmemory`) are implemented against a
stub `LearningResult`/`Tactic` contract so the AI module and mob module can be
wired in independently. See [TESTING.md](TESTING.md) for how to exercise it.

## Team

Built as a three-person hackathon project.

| Member | Area | Core tasks |
| --- | --- | --- |
| Arseniy | AI learning engine | player action tracking, habit profile, tactic selection, reward/penalty, memory persistence, "What Nemesis Learned" screen |
| Teammate 2 | Mod & mob | Fabric setup, Nemesis entity, movement/attacks, shield/bow/pursuit tactics, ranged tower attack |
| Teammate 3 | HUD, testing & presentation | adaptation HUD, learning messages, test world, scenario testing, README/Devpost, demo video |

### Task checklist (Teammate 3 slice)

- [x] Adaptation HUD overlay
- [x] `LEARNED:` / `TACTIC CHANGED:` messages
- [x] `/nemesis summon` and `/nemesis resetmemory` commands
- [x] Test world instructions and scenario checklist
- [ ] Full scenario pass once the AI and mob modules are integrated
- [ ] Screenshots, Devpost page, demo video

## License

See LICENSE.

Nemesis AI is created for the "The Orchestra" Agent Orchestrator Hackathon.
