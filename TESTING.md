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
| `/nemesis tactic <name>` | Forces the nearest Nemesis entity's combat tactic (`normal`, `fast_chase`, `delayed_attack`, `zigzag_approach`, `ranged_attack`) without going through the learning thresholds. |
| `/nemesis learn <name>` | Simulates a learning event straight to the HUD (server broadcasts a `LearningResult` without touching any entity) — useful for testing the HUD/message path in isolation. |
| `/nemesis resetmemory` | Clears the calling player's stub record *and* resets the nearest live Nemesis entity's learned counters/tactic back to `normal`. |

## How Nemesis actually learns (real gameplay loop)

`NemesisEntity` counts these player behaviors while it has a target and switches tactic (and
notifies the HUD via `NemesisFeedback.broadcastLearning`) once a counter crosses its threshold
(3 by default) **and** that tactic isn't already active:

| Player behavior | Detected in | Tactic Nemesis switches to |
| --- | --- | --- |
| Blocks Nemesis's melee hits with a shield | `NemesisEntity#doHurtTarget` | `DELAYED_ATTACK` |
| Hits Nemesis with a ranged weapon (arrow, etc.) | `NemesisEntity#hurt` | `ZIGZAG_APPROACH` |
| Increases distance from Nemesis while being chased (checked every second) | `NemesisEntity#customServerAiStep` | `FAST_CHASE` |

The counters and current tactic are persisted via `addAdditionalSaveData`/`readAdditionalSaveData`,
so a Nemesis remembers a player's habits across a server restart (as long as the chunk/entity
isn't unloaded and discarded).

`RANGED_ATTACK` (Nemesis shooting arrows at a fleeing/towering player) exists as a tactic and
combat goal but currently has no automatic trigger — it's reachable via `/nemesis tactic
ranged_attack` for demos. A good follow-up would be detecting the player building upward near
Nemesis and switching to it automatically.

## Scenarios to verify

- [ ] Fresh-spawn a Nemesis, block 3 of its melee hits with a shield → HUD shows
      **"LEARNED: DELAYED ATTACK"**, Nemesis starts waiting out your guard instead of spamming hits.
- [ ] Shoot the same Nemesis with 3 arrows → HUD shows **"TACTIC CHANGED: ZIGZAG APPROACH"**,
      Nemesis starts approaching at an angle instead of walking straight at you.
- [ ] Let it chase you and back away 3 times (roughly a second apart, moving away each time) →
      **"TACTIC CHANGED: FAST CHASE"**, movement speed visibly increases.
- [ ] `/nemesis resetmemory` near that Nemesis → its tactic goes back to `normal` and the
      counters restart (next shield block should trigger "LEARNED" again, not "TACTIC CHANGED").
- [ ] `/nemesis learn <name>` shows the HUD banner/action-bar message without needing to fight
      anything — useful to sanity-check the HUD alone.
- [ ] HUD does not draw anything before the first learning event; F1 (hide GUI) hides it too.

## Known follow-ups

- `RANGED_ATTACK` has no automatic learning trigger yet (see above) — the plan called for
  "ranged attack vs. a player who towers up", which needs block-place tracking near the entity.
- `NemesisMemoryStore` (`src/main/java/com/sen2x/nemesisai/api/NemesisMemoryStore.java`) is
  still just a stub per-player "last simulated result" cache used by `/nemesis learn`; the real
  memory now lives on the entity itself via NBT, which is the more correct place for it.
- There are still two unused/duplicate mod entrypoints left over from the merge
  (`dev.nemesis.NemesisMod`, `dev.nemesis.client.NemesisClient`) that aren't registered in
  `fabric.mod.json` — dead code, safe to delete once the team confirms nothing needs them.
- A separate open PR (`agent/geckolib-nemesis-integration`) adds an animated GeckoLib model and
  its own third `NemesisEntity`/`ModEntities` variant in yet another package
  (`com.sen2x.nemesisai.entity`) — merging it will need the same kind of reconciliation this
  branch just did, pointing it at `dev.nemesis.entity.NemesisEntity`/`Tactic` instead of
  reintroducing a duplicate.
