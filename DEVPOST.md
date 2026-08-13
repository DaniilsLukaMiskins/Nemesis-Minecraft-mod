# Nemesis AI — Devpost draft

Draft copy for the Devpost submission page. Not published anywhere — paste the sections below
into the Devpost project form and fill in the bracketed placeholders. Add screenshots/video once
someone runs the client (see [TESTING.md](TESTING.md)).

## Inspiration

[Why an adaptive enemy? e.g. "Most Minecraft mobs repeat the same predictable pattern forever —
we wanted an enemy that actually remembers how you fight and stops falling for the same trick
twice."]

## What it does

Nemesis AI adds a hostile mob, **Nemesis**, to Minecraft Java 1.21.1 that adapts its combat
tactic based on how the player fights it:

- Blocks its melee attacks with a shield too often → Nemesis switches to a **delayed attack**,
  waiting out the guard instead of hitting the shield.
- Lands ranged hits on it too often → Nemesis **zigzags** in instead of walking straight at the
  player.
- Keeps retreating from it → Nemesis **speeds up** its chase.
- Climbs out of its reach (towers/turtles) → Nemesis switches to **shooting arrows**.

Under the hood this is a small weighted "habit profile": every observed behavior adds to that
habit's score, unused habits decay over time, and whichever habit is currently dominant decides
the tactic. A HUD shows an adaptation meter and `LEARNED:` / `TACTIC CHANGED:` messages whenever
Nemesis switches strategy, and what it's learned persists across a server restart.

## How we built it

- Fabric Loader + Fabric API on Minecraft 1.21.1 / Java 21, official Mojang mappings.
- `NemesisEntity` (a `Monster` subclass) drives both the combat AI goals and the habit-weight
  observation/decay loop, persisting its learned weights via NBT.
- A small custom-payload network channel (`LearningResultPayload`) pushes each learning event
  from server to client, where a `HudRenderCallback` overlay renders the adaptation bar and
  banner text.
- `/nemesis summon|tactic|learn|resetmemory` commands support manual testing/demoing without
  needing to fully re-fight the mob every time.

## Challenges we ran into

[Team to fill in — e.g. reconciling multiple parallel implementations of the same entity/tactic
system that got built independently before merging; picking detectable, reliable signals for
"the player is towering" without literal block-place tracking; etc.]

## Accomplishments that we're proud of

[Team to fill in]

## What we learned

[Team to fill in]

## What's next for Nemesis AI

- A literal "player is placing blocks near me" signal instead of the current height-gap/
  unreachable-path heuristic for the tower-counter tactic.
- A full "What Nemesis Learned" summary screen (the HUD currently shows live adaptation, not a
  persistent per-player summary view).
- Animated model (see the in-progress GeckoLib integration branch).

## Built with

Java, Fabric Loader, Fabric API, Gradle, Minecraft Java Edition 1.21.1
