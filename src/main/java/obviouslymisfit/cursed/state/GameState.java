package obviouslymisfit.cursed.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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

    public GameState() {}
}
