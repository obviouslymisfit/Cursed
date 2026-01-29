# CURSED
## Full Design & Implementation Specification
### Authoritative Narrative Document

---

## 1. What CURSED Is Trying to Be

CURSED is not a mini-game, and it is not a casual challenge mode.  
It is a long-form competitive survival experience designed to be played over multiple real-world sessions, where pressure accumulates not through artificial difficulty spikes, but through *persistent uncertainty*.

The core idea of CURSED is a deliberate asymmetry of information.

Players must **always understand what they are trying to achieve**. Objectives are explicit. Quantities are known. Actions required to progress are visible, inspectable, and unambiguous.

At the same time, players must **never be told why the world is punishing them**.

The curse system is not a mechanic to be solved through UI. It is an environmental force that must be learned through experience, failure, pattern recognition, and adaptation. If players can point to a command, tooltip, or timer that explains a curse, the design has failed.

This split — clarity of goals versus opacity of danger — defines CURSED at every level.

---

## 2. The Shape of a CURSED Campaign

A CURSED game is not a single uninterrupted play session. It is a **campaign**, intentionally structured to survive restarts, interruptions, and real-world schedules.

The campaign exists as a **game run**.

A run is the authoritative container for everything that matters:
the current phase, the objectives that exist, the score of each team, the active curses, and the accumulated consequences of past decisions.

A run is persistent. It is saved to disk. It is loaded when the server starts.  
There is only ever **one run** at a time.

However, a run is not always active.

Real people play at specific times, for limited durations. To accommodate this, CURSED introduces the concept of an **episode**.

An episode represents a real-world play window. When an episode is active, the run is considered *live* and gameplay systems are allowed to operate. When no episode is active, the run is paused. Nothing progresses, nothing decays, and nothing silently advances in the background.

Episodes exist to protect fairness, to enforce clean breaks, and to prevent accidental progress during restarts or downtime.

---

## 3. Lifecycle, Intent, and Restart Safety

A fundamental design principle of CURSED is that **nothing meaningful happens without explicit intent**.

For this reason, the run lifecycle is strictly controlled.

A run may exist in one of several high-level states. When the run is paused, gameplay is frozen. When it is running, an episode is active. When it reaches a terminal state, the campaign is over.

After a server restart, a run must **never automatically resume**.  
Instead, it always loads into a paused state.

This rule is absolute. Automatic resumption would allow progress to occur without players present and would undermine the idea of episodes as deliberate play sessions.

Only an administrator can resume a run. Only an administrator can start a new run. The game does not guess intent.

---

## 4. Teams as the Primary Actor

CURSED is not designed around individual players.  
It is designed around **teams**.

Players are transient. They may disconnect, reconnect, join late, or leave early. Teams persist. Teams score points. Teams advance objectives. Teams suffer penalties.

Every meaningful unit of progress in CURSED is tracked at the team level.

This has several consequences:

First, no player ever “wins” alone. Victory is collective.

Second, punishment is shared. A single mistake can harm the entire team, reinforcing coordination and accountability.

Third, the system must tolerate player churn without breaking. Objectives do not depend on a specific player being present; they depend on the team as a whole.

---

## 5. The Spawn Platform as the Center of Gravity

The spawn platform is not a cosmetic feature.  
It is the physical and psychological center of the game.

Every episode begins with players returning to this platform.  
Every run ends with players returning to this platform.

Mechanically, the platform serves as the delivery anchor for primary objectives. Conceptually, it is the place where teams regroup, argue, coordinate, and feel pressure as deadlines approach.

By forcing teams to return to a shared location to make progress, CURSED creates risk. Travel becomes meaningful. Death far from the platform carries real consequences. Success requires planning routes, staging resources, and deciding *when* to commit.

The spawn platform also hosts team-specific chests. These chests are visible symbols of progress, but they are not trusted sources of truth. The mod tracks deliveries explicitly, ensuring correctness even across restarts.

---

## 6. Phases and Escalation

CURSED is structured into **five phases**, each representing a global escalation of danger and commitment.

All teams share the same phase number. There is no per-team phasing. This is intentional. The game is competitive, but it is also shared.

Each phase introduces new objectives and increases the intensity of penalties and curses. Early phases allow exploration and setup. Later phases force commitment, compression, and risk.

Phase five is intentionally different.

It contains only a single primary objective. There are no secondary objectives, no tasks, and no side paths. By this point, the game is no longer about optimization or flexibility. It is about execution under pressure.

Phase advancement is global, but credit is local.

The first team to complete a phase’s primary objective advances the phase for everyone. However, other teams may still complete that primary objective afterward to earn points. This prevents runaway leaders while still rewarding effort.

---

## 7. Objectives and Tasks: Explicit by Design

CURSED rejects vague objectives.

Every objective in the game must be describable in a single sentence that includes:
- what must be done,
- with which items,
- in what quantity,
- and under what constraints.

This explicitness is not accidental. It ensures that players can always make informed decisions, even when the world itself is hostile and unpredictable.

Objectives are divided into three categories.

**Primary objectives** are the backbone of the campaign. They are always delivery-based and always tied to the spawn platform. They drive phase advancement and represent the largest point awards.

**Secondary objectives** exist to add flexibility and strategic choice. They may require delivery or team-wide gathering and are generally less demanding than primaries.

**Tasks** are small, production-focused goals. They reward infrastructure, planning, and efficiency. Tasks never require delivery and never use cohesion rules.

---

## 8. Team Gather, Delivery, and Cohesion

Not all objectives require physical delivery.

In team-gather objectives, progress is counted across the combined inventories of all team members. Players may spread out, specialize, and contribute independently.

However, some objectives introduce **cohesion requirements**.

Cohesion is a constraint applied at completion time. It exists to force coordination and to prevent teams from trivializing objectives through pure distribution.

For delivery cohesion, all team members must be near the spawn platform when the final delivery completes. For gather cohesion, all team members must be near each other when completion is evaluated.

Tasks are explicitly excluded from cohesion rules. They are meant to reward preparation, not coordination under pressure.

---

## 9. Data-Driven Design Philosophy

CURSED is intentionally data-driven.

Java code defines *rules*.  
JSON defines *content*.

All objective structure, counts, quantities, and templates are defined in data files. This allows the campaign to be rebalanced, tuned, or extended without touching code.

The engine reads this data once at run start, generates all objectives and tasks up front, and persists them. No new objectives are ever generated mid-run.

Disjointness is enforced. Objectives do not repeat. Items are not reused across phases. Repetition would reduce tension and allow memorization.

---

## 10. The Curse System: Pressure Without Explanation

The curse system is the invisible antagonist of CURSED.

Curses are not announced. They are not listed. They are not hinted at.  
Players experience them through damage, death, and escalating penalties.

Curses are associated with **block types**, not positions. The world itself becomes hostile, not specific locations. As phases advance, the ways in which contact with cursed blocks causes harm expands.

There are three layers of curses:
personal, team-wide, and global.

Each layer operates independently. Nothing is shared across layers. A curse that affects one player does not necessarily affect their teammates.

Rotation and jitter ensure that patterns are discoverable but never fully predictable. Players may *suspect* which blocks are dangerous, but certainty is always risky.

---

## 11. Scoring, Death, and Redistribution

Points exist to create competition and urgency.

Completing objectives awards points to the team. Primary objectives award the most, tasks the least.

Death is not neutral. It is a meaningful setback.

In early phases, death penalties are mild. In later phases, penalties grow harsher and may redistribute lost points to other teams. This creates dynamic pressure and prevents safe turtling by leading teams.

Penalties are applied immediately. There is no post-run accounting. The scoreboard always reflects the current reality of the campaign.

---

## 12. Player Perception and Information Control

What players can see is carefully curated.

The sidebar displays only high-level state: episode number, episode timer, current phase, and team scores. It never displays objectives, tasks, or curse information.

Detailed progress is available only through explicit commands, and only for the player’s own team. There is no global inspection of other teams’ objectives or progress.

Curses are never exposed in any form. Silence is intentional.

---

## 13. Ending a Run

When the final primary objective is completed, the run ends.

Gameplay freezes. After a short delay, all players are returned to the spawn platform. This moment provides closure and reinforces the idea that the campaign has concluded.

There are no rewards. There is no meta-progression. Victory is recognition, not accumulation.

---

## 14. Starting Over

Starting a new run is an explicit administrative action.

A new run is a full reset. Nothing carries over. All objectives, curses, scores, and state are regenerated from scratch.

The new run always begins paused. An episode must be explicitly started.

This design avoids snowballing, preserves fairness, and ensures that each campaign stands on its own.

---

## 15. Architectural Principles

CURSED is built on strict separation of concerns.

Lifecycle management, objective generation, scoring, curses, UI, and persistence are independent systems that communicate through explicit interfaces.

No system silently mutates another’s state.

All non-trivial code must be commented at the moment it is written. Comments explain not only *what* the code does, but *why it exists*.

When in doubt, this document is the authority.

If the code and the spec disagree, the code is wrong.
