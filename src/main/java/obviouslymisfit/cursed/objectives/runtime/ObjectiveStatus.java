package obviouslymisfit.cursed.objectives.runtime;

/**
 * Runtime status for a team's objective instance.
 *
 * M1 scope:
 * - AVAILABLE: objective exists and can be progressed (even if not "active" yet in later milestones)
 * - COMPLETED: objective has been completed by the team
 */
public enum ObjectiveStatus {
    AVAILABLE,
    COMPLETED
}
