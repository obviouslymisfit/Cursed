# 🔒 CURSED — AUTHORITATIVE CHAT ANCHOR

This document defines how the CURSED project is anchored across ChatGPT sessions.
It exists to prevent drift, hallucination, or ambiguity about scope, authority, and current state.

---
## 📦 PROJECT IDENTITY (LOCKED)

- **Repository:** https://github.com/obviouslymisfit/Cursed
- **Mod name:** CURSED
- **Loader:** Fabric
- **Minecraft:** 1.21.10
- **Scope:** Server-only

This repository link is authoritative and does not change.

## 🧱 ABSOLUTE AUTHORITY RULES

### 1. Repo / ZIP Truth Rule
If something is not present in the uploaded ZIP or GitHub repository state, it does not exist.

- No invented methods
- No inferred fields
- No assumptions based on memory

Implementation reality always wins over recollection.

---

### 2. File-Anchor Rule (MANDATORY)
All discussion and work must be anchored to explicitly provided files.

- If a file is referenced, it must be uploaded or linked
- If a file is modified, only the explicitly requested changes are allowed
- For comment-only passes:
    - ZERO code changes
    - No formatting changes
    - No reordered imports
    - Only added comments and optional blank lines

---

### 3. Specification Authority Hierarchy
Conflicts are resolved using the following order (highest → lowest):

1. **Repository / ZIP state** (implementation truth)
2. **Overall Design Spec** (`docs/spec/CURSED-SPEC-v3.md`)
3. **Active Milestone Spec** (e.g. `docs/spec/M1.md`)
4. **Git Workflow Rules** (`docs/workflow/git-workflow.md`)

If a conflict is detected:
→ STOP  
→ DISCUSS  
→ DO NOT silently “fix” anything

---

### 4. Mode Discipline
- Discussion mode by default
- No code unless explicitly requested
- One file at a time
- Compile between steps
- No speculative refactors
- No design changes unless explicitly approved

---

## 📦 PROJECT IDENTITY (STATIC)

- **Project:** CURSED
- **Loader:** Fabric
- **Minecraft:** 1.21.10
- **Scope:** Server-only

---

## 📍 PROJECT STATE (PER CHAT — REQUIRED)

The following information is **NOT static** and must be explicitly declared
at the start of every new ChatGPT session:

- Active truth branch
- Active milestone

If this information is missing, outdated, or ambiguous,
the assistant must STOP and request clarification before proceeding.


## 🧭 HOW TO START A NEW CHAT (MANDATORY)

When starting a new ChatGPT session for this project:

1. Paste this entire `ANCHOR.md`
2. Provide **one** of the following:
    - Link to GitHub repo (discussion / planning)
    - ZIP snapshot of current repo state (required for code work)
3. State:
    - Current milestone
    - Active truth branch
4. Specify mode:
    - Discussion
    - Comments-only
    - Code (file + intent)

Nothing proceeds until anchoring is confirmed.

---
