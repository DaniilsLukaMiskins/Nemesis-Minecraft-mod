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
`Tactic` enum driving both its combat AI and the HUD. It keeps a weighted habit profile (shield
blocks, ranged hits, retreating, being towered on) that rewards repeated behavior, decays unused
habits over time, and auto-switches to whichever counter-tactic is currently dominant — then
persists that profile across restarts via NBT. HUD, learning-event networking, and test/debug
commands (`/nemesis summon`, `/nemesis tactic`, `/nemesis learn`, `/nemesis resetmemory`) are all
wired to it. See [TESTING.md](TESTING.md) for how to exercise it.

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
      into one, and wired real gameplay (shield blocks, ranged hits, retreating, being towered
      on) into a weighted, decaying habit profile that drives tactic selection and the HUD
- [ ] In-game scenario pass on the consolidated build
- [ ] Screenshots, Devpost page, demo video

## License

See LICENSE.

Nemesis AI is created for the "The Orchestra" Agent Orchestrator Hackathon.
