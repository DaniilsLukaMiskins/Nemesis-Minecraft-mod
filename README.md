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

There is one consolidated Nemesis entity (`dev.nemesis.entity.NemesisEntity`) and one shared
`Tactic` enum driving both its combat AI and the HUD. It automatically switches tactic based on
real player behavior — shield blocks, ranged hits, and retreating — and persists what it's
learned across restarts. HUD, learning-event networking, and test/debug commands (`/nemesis
summon`, `/nemesis tactic`, `/nemesis learn`, `/nemesis resetmemory`) are wired to it. See
[TESTING.md](TESTING.md) for how to exercise it and what's still a manual-only tactic
(`RANGED_ATTACK` has no automatic trigger yet).

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
- [x] Consolidated the competing `NemesisEntity`/`Tactic` implementations from parallel branches
      into one, and wired real gameplay (shield blocks, ranged hits, retreating) into the
      learning/HUD pipeline
- [ ] In-game scenario pass on the consolidated build
- [ ] Screenshots, Devpost page, demo video

## License

See LICENSE.

Nemesis AI is created for the "The Orchestra" Agent Orchestrator Hackathon.
