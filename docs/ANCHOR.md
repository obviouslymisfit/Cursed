# CURSED — CHAT ANCHOR FILE
## Authoritative Context Bootstrap for New Chats

This file exists to guarantee **continuity, correctness, and zero hallucination** when starting a new chat related to the CURSED project.

Any new chat **must be initialized by pasting this file verbatim** before discussion or implementation begins.

---

## 📦 PROJECT IDENTITY (LOCKED & SINGLE SOURCE OF TRUTH)

- **Project name:** CURSED
- **Type:** Server-side Fabric mod (Minecraft 1.21.x)
- **Client mods:** None
- **Resource packs:** None
- **UI:** Vanilla only (scoreboard, chat, titles, commands)
- **Command root:** `/curse` (LOCKED — do not use `/cursed`)
- **Repository:** Single authoritative GitHub repository
- **ZIP uploads:** Exact snapshots of repository state at a point in time

There is exactly **one CURSED project**.  
No forks, variants, or reinterpretations exist unless explicitly approved.

---

## 🧭 AUTHORITY HIERARCHY (MANDATORY)

When answering questions, giving guidance, or proposing changes, the assistant must respect the following hierarchy **in strict order**:

1. **Repository state / uploaded ZIP**  
   The actual code and files currently provided are the highest authority.

2. **Authoritative Design Specification**  
   File: `CURSED-SPEC.md`  
   This document defines the **conceptual design, terminology, intent, and philosophy** of the game.

3. **Milestone Specifications (M1–M12)**  
   These documents define **implementation slicing and order**, not design intent.

4. **This anchor file**

### Conflict rule
- If code and spec disagree → **code is wrong**
- If milestone and design spec disagree → **milestone explains implementation, spec explains intent**
- No document may silently override a higher authority

---

## 🚫 NO INFERENCE / NO MEMORY RULE (CRITICAL)

The assistant must **never** infer, extrapolate, or “fill in gaps”.

- Do **not** rely on prior chats, summaries, or memory
- Do **not** assume missing mechanics
- Do **not** invent systems, behaviors, or rules

If something is unclear:
1. **Scan the provided repository / ZIP first**
2. **Scan the design spec**
3. **Scan milestone documents**
4. Only then, ask for clarification

“I don’t know” is acceptable **only after files are reviewed**.

---

## 🔒 MILESTONE LOCK RULE (MANDATORY)

Approved milestone documents (M1–M12) are **LOCKED**.

- They define **what must exist**
- They are **not prompts or suggestions**
- They are **not to be reinterpreted**

The assistant must **not**:
- re-scope milestones
- re-order milestones
- question design decisions already locked in milestones
- introduce alternative approaches without explicit user approval

Any change requires **explicit confirmation** from the user.

---

## 🧠 MODE DISCIPLINE

- **Default mode:** Discussion / analysis
- **Code is NEVER written unless explicitly requested**
- **No mixed-mode responses**
    - (e.g. discussion + code in the same reply is forbidden)

If unsure which mode is active, **ask before proceeding**.

---

## 📁 FILE & CODE HANDLING RULES

- If a file is uploaded and the request is to:
    - “add comments”
    - “review”
    - “verify”

  → **Do not change code logic**
  → **Do not rename**
  → **Do not refactor**
  → **Only do what is explicitly requested**

- If code is written:
    - it must include **clear, explanatory comments at creation time**
    - “we’ll comment later” is invalid

---

## 🚀 HOW TO START A NEW CHAT (MANDATORY PROCEDURE)

To start a new CURSED-related chat:

1. Paste this `ANCHOR.md`
2. Upload the **current repository ZIP** or confirm no changes since last ZIP
3. State the intended mode:
    - discussion
    - review
    - implementation
4. State the target milestone (if applicable)

Only after this procedure may work begin.

---

## 🧱 DESIGN INTEGRITY RULE

If at any point:
- implementation becomes unclear
- behavior seems contradictory
- or assumptions conflict

→ **Stop coding**
→ **Update the design spec first**
→ **Then proceed**

This project is **spec-first by design**.

---

## ✅ FINAL REMINDER

CURSED is:
- explicit in goals
- opaque in danger
- data-driven in content
- strict in lifecycle
- hostile to inference
- intolerant of silent changes

If something is not in the files, **it does not exist**.
