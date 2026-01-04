package obviouslymisfit.cursed.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import obviouslymisfit.cursed.objectives.runtime.ObjectiveDefinition;
import obviouslymisfit.cursed.objectives.runtime.ObjectiveSlot;
import obviouslymisfit.cursed.objectives.runtime.TeamObjectiveState;


/**
 * Authoritative CURSED run state (DATA ONLY).
 * No gameplay logic, no messaging, no scheduling.
 */
public final class GameState {

    // IMPORTANT: bump when persistent schema changes.
    public static final int EXPECTED_SAVE_SCHEMA_VERSION = 1;

    public int saveSchemaVersion = EXPECTED_SAVE_SCHEMA_VERSION;

    public UUID runId = null;

    public RunLifecycleState lifecycleState = RunLifecycleState.IDLE;

    public int phase = 0;          // 0 when IDLE, 1-5 when run exists
    public int episodeNumber = 0;  // 0 when IDLE

    // --- Teams (Chunk 3) ---
    public boolean teamsEnabled = false;
    public int teamCount = 0;

    /**
     * Maps player UUID -> team index (0..teamCount-1).
     * Empty when teams are not configured.
     */
    public Map<UUID, Integer> playerTeams = new HashMap<>();

    // --- Runtime Objectives (Milestone 1) ---
    /**
     * Fully resolved objective definitions for the entire run.
     *
     * Keyed by:
     *  - phase (1..5)
     *  - slotKey (PRIMARY, SECONDARY_1..N, TASK_1..M)
     *
     * Structure only in M1: generation/population happens in later steps.
     */
    public Map<Integer, Map<ObjectiveSlot, ObjectiveDefinition>> objectiveDefinitions = new HashMap<>();

    /**
     * Per-team state for every objective instance.
     *
     * Keyed by:
     *  - teamIndex (0..teamCount-1)  (M1 keeps team identity minimal)
     *  - phase
     *  - slotKey
     */
    public Map<Integer, Map<Integer, Map<ObjectiveSlot, TeamObjectiveState>>> teamObjectiveStates = new HashMap<>();



    public GameState() {}
}
