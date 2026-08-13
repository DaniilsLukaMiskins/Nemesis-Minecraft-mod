# Nemesis AI

Nemesis AI is a minimal hostile-mob prototype for Minecraft Java Edition 1.21.1.

Nemesis targets the nearest player, walks toward them, and uses Minecraft's normal melee combat. It intentionally contains no tracking, learning, rewards, tactic weights, persistent memory, HUD, or adaptive tactics.

## Requirements

- Java 21
- Fabric Loader 0.19.3
- Fabric API 0.116.15+1.21.1

## Run and test

1. Run `./gradlew runClient` (`.\\gradlew.bat runClient` on Windows).
2. Create a Creative test world with cheats enabled.
3. Find **Nemesis Spawn Egg** in the Spawn Eggs tab and use it, or run `/summon nemesis_ai:nemesis`.
4. Switch to Survival and confirm Nemesis selects the nearest player, approaches, and performs melee attacks.

Build the distributable mod with `./gradlew build`. The remapped jar is written to `build/libs/`.

## Status

The entity registration, attributes, AI goals, spawn egg, translations, and client renderer are separated so later behavior can be added without changing bootstrap code. The renderer deliberately reuses the vanilla zombie model and texture.

## Team

Built as a three-person hackathon project.

## License

See LICENSE.

Nemesis AI is created for the "The Orchestra" Agent Orchestrator Hackathon.
