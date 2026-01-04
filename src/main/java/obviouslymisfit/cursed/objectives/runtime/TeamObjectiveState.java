package obviouslymisfit.cursed.objectives.runtime;

import java.time.Instant;

/**
 * Per-team state for a single objective definition (phase + slot).
 *
 * M1 is structure-only:
 * - No logic for detecting progress
 * - No activation rules
 * - No validation beyond holding fields
 */
public final class TeamObjectiveState {

    private ObjectiveStatus status;
    private Progress progress;
    private Instant completedAt; // nullable

    public TeamObjectiveState() {
        this.status = ObjectiveStatus.AVAILABLE;
        this.progress = new Progress();
        this.completedAt = null;
    }

    public TeamObjectiveState(ObjectiveStatus status, Progress progress, Instant completedAt) {
        this.status = status == null ? ObjectiveStatus.AVAILABLE : status;
        this.progress = progress == null ? new Progress() : progress;
        this.completedAt = completedAt;
    }

    public ObjectiveStatus getStatus() {
        return status;
    }

    public void setStatus(ObjectiveStatus status) {
        this.status = status == null ? ObjectiveStatus.AVAILABLE : status;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress == null ? new Progress() : progress;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * Minimal progress holder for M1.
     *
     * DELIVER objectives will later use depositedCount as the authoritative progress number.
     * Other action types will evolve in later milestones.
     */
    public static final class Progress {
        private int depositedCount;

        public Progress() {
            this.depositedCount = 0;
        }

        public Progress(int depositedCount) {
            this.depositedCount = depositedCount;
        }

        public int getDepositedCount() {
            return depositedCount;
        }

        public void setDepositedCount(int depositedCount) {
            this.depositedCount = depositedCount;
        }
    }
}
