# CURSED — Git Workflow
## Authoritative Development & Version Control Rules

This document defines the **only valid Git workflow** for the CURSED project.

Its purpose is to:
- prevent design drift
- enforce milestone discipline
- keep history readable and auditable
- ensure the repository always reflects intentional, approved decisions

This workflow is optimized for a **solo developer working on a long-lived, design-heavy project**.

---

## 1. Branch Structure (LOCKED)

### Main branch
`main` always represents the **latest stable, verified state**.

No experimental or in-progress work is allowed on `main`.  
`main` is updated only via fast-forward merges from milestone truth branches.

---

### Milestone truth branches

Each milestone has **exactly one truth branch**:

    milestone/M1
    milestone/M2
    milestone/M3
    ...

Rules:

A truth branch represents the **authoritative implementation** of that milestone.  
Once merged into `main`, a truth branch is frozen.  
Truth branches are never rebased after merge.

This guarantees a clean one-to-one mapping between milestones and code, and supports rollback and auditing.

---

### Task / working branches

Short-lived task branches are created from the active milestone branch:

    task/M2-objective-generation
    task/M4-phase-engine

Rules:

Task branches are implementation-only.  
Task branches are deleted after merge.  
No task branch may outlive its milestone.

---

## 2. Merge Policy (STRICT)

Allowed merges:
- Task branch → milestone truth branch
- Milestone truth branch → `main`

Forbidden merges:
- Direct commits to `main`
- Merging `main` into milestone branches
- Merging between milestones

Merge style:
- **Fast-forward only**
- No merge commits
- No squash merges on truth branches

This keeps history linear and readable.

---

## 3. Milestone Scope Rule (MANDATORY)

Each milestone truth branch may implement **one milestone only**.

No features from future milestones may be introduced early.  
No “while we’re here” additions.  
No refactors unrelated to the milestone.

If a dependency is discovered:
- stub it
- isolate it
- or stop and update the design spec

Milestones are implementation slices, not opportunities for redesign.

---

## 4. Spec-First Rule (CRITICAL)

CURSED is a **spec-first project**.

No code may be written unless the behavior is:
- defined in `CURSED-SPEC.md`
- and scoped to the active milestone

If implementation reveals ambiguity:
- **stop coding**
- update the spec
- then continue

Code must never silently redefine game behavior.

---

## 5. Commenting Requirement (NON-NEGOTIABLE)

All non-trivial code must be **commented at creation time**.

Rules:

Comments must explain:
- what the code does
- why it exists
- how it fits into the system

Commits that introduce uncommented logic are invalid.  
“We will comment later” is not allowed.

If code cannot be clearly commented, implementation must pause.

---

## 6. Commit Rules

### Commit size
Commits should be small and focused.  
One conceptual change per commit.

### Commit messages
Commit messages must describe **intent**, not just mechanics.

Prefer:

    Add objective slot generation for phase start

over:

    Fix objective code

---

## 7. Tags (Milestone Completion Markers)

Tags are used to mark **milestone completion points**.

Rules:
- Tags correspond to milestones (example: `M2-complete`)
- Tags are **historical markers**, not release candidates
- A tag means:
    - the milestone is implemented
    - the code matches the spec
    - the state is stable and testable

---

## 8. Forbidden Actions

The following are explicitly forbidden:

- Hotfixing unrelated issues while working on a milestone
- Refactoring code “because it’s nearby”
- Changing behavior without updating the spec
- Merging unfinished milestone work into `main`
- Leaving TODOs for core logic
- Relying on memory instead of files

---

## 9. Debugging & Recovery

If something goes wrong:

Identify the last known-good milestone tag.  
Check out the corresponding truth branch.  
Compare spec vs code.  
Do not patch blindly.

Git history is meant to be navigable, not decorative.

---

## 10. Final Principle

CURSED is a long-term project.

Clarity beats speed.  
Correctness beats cleverness.  
Design integrity beats convenience.

If unsure:
- stop
- read the spec
- then proceed
