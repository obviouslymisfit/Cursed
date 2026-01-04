package obviouslymisfit.cursed.objectives.runtime;

/**
 * Stable slot identifier for runtime objectives.
 *
 * LOCKED (M1):
 * - Slot identity is NOT positional
 * - Slot identity is persisted
 * - Slot identity survives restarts and phase advancement
 *
 * Naming convention:
 *  PRIMARY
 *  SECONDARY_1 .. SECONDARY_N
 *  TASK_1 .. TASK_M
 */
public enum ObjectiveSlot {

    PRIMARY,

    SECONDARY_1,
    SECONDARY_2,
    SECONDARY_3,
    SECONDARY_4,
    SECONDARY_5,

    TASK_1,
    TASK_2,
    TASK_3,
    TASK_4,
    TASK_5;

    /**
     * Returns true if this slot represents a secondary objective.
     */
    public boolean isSecondary() {
        return name().startsWith("SECONDARY_");
    }

    /**
     * Returns true if this slot represents a task objective.
     */
    public boolean isTask() {
        return name().startsWith("TASK_");
    }
}
