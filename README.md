# Nemesis

A deliberately minimal Fabric mod that provides the combat-mob foundation for the Nemesis AI hackathon project. This version contains **only** ordinary Minecraft mob behavior: Nemesis finds the nearest player, walks to them, and uses vanilla melee attacks. It has no tracking, learning, rewards, tactic weights, persistence, HUD, or adaptive behavior.

## Requirements

- Minecraft Java Edition **exactly 1.21.1**
- Fabric Loader 0.16.14 or newer
- Fabric API 0.116.6+1.21.1
- A 64-bit Java 21 JDK (Java 21 is required to build and run Minecraft 1.21.1)

## Run and test

```text
./gradlew runClient       # PowerShell: .\gradlew.bat runClient
```

Create a Creative test world, then either use the **Nemesis Spawn Egg** from the Spawn Eggs tab or run:

```mcfunction
/summon nemesis:nemesis ~ ~ ~
```

Switch to Survival and confirm that Nemesis approaches the nearest player and deals melee damage. Build the distributable JAR with `./gradlew build`; it is written to `build/libs/`.

## Scope and extension points

- `NemesisEntity` owns attributes and vanilla goals.
- `ModEntities` owns entity registration.
- `NemesisClient` owns client-only rendering registration.

Keep future experimental AI behind separate goals/services rather than coupling it to registration or rendering.

Exact build commands (with `JAVA_HOME` pointing to a Java 21 JDK):

```text
./gradlew clean build
# PowerShell: .\gradlew.bat clean build
```
