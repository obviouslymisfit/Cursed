CURSED — Git Workflow (Authoritative)

This document defines the Git rules for CURSED development.
Goal: prevent drift, prevent branch confusion, keep a single reliable “truth” branch, and make auditing trivial.

============================================================
1) DEFINITIONS
   ============================================================

Truth Branch
------------
The truth branch is the authoritative branch for the current milestone.

Properties:
- Must always build
- Must match the current milestone spec
- Receives only reviewed, intentional changes

Current truth branch:
- feature/m1-runtime-engine


Task Branch
-----------
A task branch is short-lived and exists for one narrowly scoped change.

Rules:
- One task per branch
- Small scope
- Merged into the truth branch after build verification
- May be deleted after merge (optional during active WIP)

Naming convention:
- task/m1-<short-desc>
- task/m2-<short-desc>

Examples:
- task/m1-comments-objectivesdataloader
- task/m1-fix-status-output
- task/m2-run-episode-flow


Milestone Tag
-------------
A tag is a frozen “known good” checkpoint.

Use tags:
- After major stable steps
- Before risky refactors
- When a milestone is considered complete

Examples:
- v0.9-m1-step4
- v0.9-m1-pre-m2


============================================================
2) GOLDEN RULES (NON-NEGOTIABLE)
   ============================================================

1. One Truth Branch per milestone
    - All authoritative work lands here
    - Task branches are disposable

2. Build Gate
    - Every task branch MUST pass:
      ./gradlew build
    - No exceptions

3. Small, Auditable Commits
    - One logical change per commit
    - For comment-only passes:
      ONE FILE = ONE COMMIT

4. Spec-First
    - If code and spec conflict:
      STOP and DISCUSS
    - No silent fixes

5. No Drive-by Refactors
    - No cleanup while doing other tasks
    - No “while I’m here” changes


============================================================
3) DAILY WORKFLOW
   ============================================================

A) Start Work
-------------

Switch to the truth branch and update it:

git checkout feature/m1-runtime-engine
git pull

Create a task branch:

git checkout -b task/m1-<short-desc>


B) Before Committing (MANDATORY CHECKLIST)
------------------------------------------

Always run:

git branch --show-current
git status --porcelain
git diff

For comment-only work:
- Verify ONLY comments and blank lines changed
- No code tokens
- No import reordering
- No formatting changes


C) Commit
---------

Stage only relevant files:

git add path/to/file1 path/to/file2

Commit message format:
- M1: <description>
- M2: <description>

Examples:
- M1: comments only - ObjectivesDataLoader
- M1: tighten run_state restart safety
- M2: episode start/end lifecycle wiring

Commit:

git commit -m "M1: <message>"


D) Build Gate
-------------

./gradlew build


E) Merge Back to Truth Branch (Preferred: Fast-Forward)
-------------------------------------------------------

Switch back:

git checkout feature/m1-runtime-engine
git pull

Fast-forward merge:

git merge --ff-only task/m1-<short-desc>

If fast-forward fails, rebase task branch:

git checkout task/m1-<short-desc>
git rebase feature/m1-runtime-engine
git checkout feature/m1-runtime-engine
git merge --ff-only task/m1-<short-desc>


F) Push
-------

Push truth branch:

git push

Optionally push task branch (backup):

git push -u origin task/m1-<short-desc>


============================================================
4) TAGGING
   ============================================================

Tag stable checkpoints or pre-risk points:

git tag -a v0.9-m1-step4 -m "M1 Step 4 stable checkpoint"
git push origin v0.9-m1-step4


============================================================
5) COMMENT-ONLY PASS RULES
   ============================================================

When doing documentation / comment passes:

- One file at a time
- Comments and optional blank lines ONLY
- No code changes
- No formatting
- No import reordering
- No renamed variables

Process:
1) Create a task branch:
   task/m1-comments-<file>
2) Edit exactly one file
3) Build
4) Commit
5) Merge fast-forward into truth branch


============================================================
6) EMERGENCY SAFETY MOVES
   ============================================================

Create a backup pointer (no checkout):

git branch backup/<name>

Discard ALL uncommitted changes (DANGEROUS):

git reset --hard

Return to a specific commit (DANGEROUS):

git reset --hard <commit-hash>


============================================================
7) BRANCH HYGIENE (ACTIVE WIP)
   ============================================================

- Do NOT delete branches impulsively
- Keep old branches as history markers if unsure
- Rely on:
    - Truth branch
    - Tags
    - Small task branches

Clarity beats cleverness.
