# CURSED — FULL DESIGN & SYSTEM SPECIFICATION (v3)

This document defines the long-term design, vision, and invariant rules of the CURSED mod.

It describes **what the mod ultimately is**, not what is currently implemented.

---

## 🎯 Purpose
CURSED is a deterministic, server-side Minecraft mod that structures cooperative survival gameplay
around long-form “runs” composed of objectives, constraints, and escalating pressure.

The design prioritizes:
- Determinism over randomness
- Persistence over session-based logic
- Explicit state over implicit assumptions
- Engine-first architecture

---

## 🧠 Core Design Invariants (NEVER BROKEN)

- There is exactly one active run at a time
- Run state is persisted to disk and survives restarts
- Objectives are data-driven, not hardcoded
- Progression is deterministic and reproducible
- Debug tooling is explicit and gated
- No system may “infer” player intent

If an implementation violates these, it is incorrect.

---

## 🧩 Major Systems (High-Level)

- Run lifecycle & persistence
- Objective engine (definitions, slots, phases)
- Constraints system
- Curse layers (future milestone)
- Scoring & outcomes (future milestone)
- UI & player feedback (future milestone)

This document defines **what these systems must be**, not when they are implemented.

---

## 🚧 Milestones
Implementation is divided into milestones (M1, M2, …).

Each milestone:
- Has its own spec file
- Defines scope and exclusions
- May temporarily override parts of this document for implementation purposes

Milestone specs **do not change the long-term design** — they define execution order.

---

## 📌 Change Policy
- Changes here require explicit discussion and approval
- Milestone behavior is NOT patched into this document
- This file evolves slowly and intentionally

---
