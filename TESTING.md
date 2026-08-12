# Testing Nemesis AI (HUD / commands slice)

This covers the pieces owned by the HUD, testing, and presentation role. The AI
learning logic and the real Nemesis mob are separate modules and are simulated
here with stub data so this slice can be tested independently.

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
| `/nemesis summon` | Spawns the real Nemesis entity a couple blocks away. |
| `/nemesis learn <tactic>` | Simulates a learning event for one of: `default`, `delayed_attack`, `zigzag_approach`, `rush_pursuit`, `ranged_tower_attack`. Triggers the same HUD/message path the real AI module will use. |
| `/nemesis resetmemory` | Clears the stub per-player memory store. |
| `/nemesis tactic <name>` | Teammate 2's command: forces the nearest Nemesis entity's combat tactic (`normal`, `fast_chase`, `delayed_attack`, `zigzag_approach`, `ranged_attack`) — separate from the HUD/learning `Tactic` enum above, see note below. |

## Scenarios to verify

- [ ] `/nemesis learn delayed_attack` on a fresh session shows **"LEARNED: DELAYED ATTACK"** in the action bar and on the HUD banner, and the adaptation bar fills to 50%.
- [ ] Running `/nemesis learn zigzag_approach` right after shows **"TACTIC CHANGED: ZIGZAG APPROACH"** (different tactic than last time).
- [ ] Running `/nemesis learn zigzag_approach` again (same tactic as current) shows no new banner text, but the adaptation bar still reflects the latest value.
- [ ] `/nemesis resetmemory` clears the stored state for that player (verify via a fresh `/nemesis learn ...` behaving like the first-ever event).
- [ ] `/nemesis summon` places a visibly named test mob near the player, usable for demo footage before the real entity is ready.
- [ ] HUD does not draw anything before the first learning event (no bar/message clutter on a clean HUD).
- [ ] F1 (hide GUI) also hides the Nemesis HUD.

## Integration checklist

- [x] `/nemesis summon` now spawns the real Nemesis entity (`dev.nemesis.entity.ModEntities.NEMESIS`).
- [ ] Have the AI module call `NemesisFeedback.broadcastLearning(ServerPlayer, LearningResult)`
  (`src/main/java/com/sen2x/nemesisai/api/NemesisFeedback.java`) instead of the `/nemesis learn`
  test command — the HUD path is already wired to it.
- [ ] Replace `NemesisMemoryStore` (`src/main/java/com/sen2x/nemesisai/api/NemesisMemoryStore.java`)
  with the AI module's persistent memory once available; keep the same method names or update
  `NemesisCommands#resetMemory` accordingly.

## Known duplication to resolve as a team

The mob module ended up with its own `dev.nemesis.entity.Tactic` enum (`normal`, `fast_chase`,
`delayed_attack`, `zigzag_approach`, `ranged_attack`) driving `NemesisEntity`'s combat goals,
separate from this branch's `com.sen2x.nemesisai.api.Tactic` (used for the HUD/learning
messages). They currently don't talk to each other — picking a tactic via `/nemesis tactic`
does not update the HUD, and `/nemesis learn` does not change the entity's behavior. Once the
AI module (Arseniy) is ready to drive `NemesisEntity#setTactic`, the team should agree on a
single shared `Tactic` enum instead of keeping both.

There are also two unused/duplicate mod entrypoints from the merge (`dev.nemesis.NemesisMod`
and `dev.nemesis.client.NemesisClient`) that aren't registered in `fabric.mod.json` — dead code
left over from the mob module's branch, worth deleting once confirmed unneeded.
