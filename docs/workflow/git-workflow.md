# 🔧 CURSED — GIT WORKFLOW (AUTHORITATIVE)

This document defines how development is done on the CURSED project.
It governs **process**, not gameplay behavior.

---

## 🌿 Branching Model

- One **truth branch per milestone**
    - Example: `feature/m1-runtime-engine`
- All work happens on short-lived `task/*` branches
- Truth branches are fast-forwarded only

---

## 🔁 Workflow

1. Checkout truth branch
2. Create task branch
3. Make minimal, scoped changes
4. Compile and verify
5. Merge fast-forward into truth branch
6. Push immediately

---

## 🧪 Safety Rules

- No bulk changes
- No silent refactors
- No commits without build success
- Documentation-only commits allowed and encouraged

---

## 🏷️ Tags
When a milestone reaches a playable or stable state:
- Tag the commit
- Never move tags

---

## 🚫 Forbidden

- Working directly on `main`
- Mixing features in one commit
- “Fixing while here” changes

---
