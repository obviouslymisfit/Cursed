package obviouslymisfit.cursed.state.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.time.Instant;

import com.mojang.util.InstantTypeAdapter;
import net.minecraft.server.MinecraftServer;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.objectives.runtime.ObjectiveDefinition;
import obviouslymisfit.cursed.objectives.runtime.ObjectiveSlot;
import obviouslymisfit.cursed.objectives.runtime.TeamObjectiveState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

/**
 * File-based run state persistence for Milestone 1.
 *
 * LOCKED (M1):
 *  - Location: <world>/data/cursed/run_state.json
 *  - Atomic save: write .tmp then replace
 *  - Keep single .bak rollback
 *  - Load: try json, fallback to bak; hard-fail if both invalid
 *  - Restart safety: after load, lifecycleState MUST be forced to PAUSED
 *
 * This class does NOT create runs, does NOT invent defaults, does NOT re-evaluate JSON objective data.
 */
public final class RunStateFileIO {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()

            // Gson cannot reliably reflect into java.time.Instant on newer Java runtimes.
            // We serialize Instants as ISO-8601 strings (e.g. "2026-01-29T14:15:22.123Z").
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())

            .create();


    private RunStateFileIO() {}

    /**
     * Saves the provided state using atomic tmp->replace semantics and a single .bak rollback.
     * Throws IOException on failure (callers decide how to surface errors).
     */
    public static void save(MinecraftServer server, GameState state) throws IOException {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(state, "state");

        Path dir = RunStateFilePaths.cursedDataDir(server);
        Path json = RunStateFilePaths.runStateJson(server);
        Path tmp = RunStateFilePaths.runStateTmp(server);
        Path bak = RunStateFilePaths.runStateBak(server);

        Files.createDirectories(dir);

        // Ensure schema version is consistent with expected constant.
        state.saveSchemaVersion = GameState.EXPECTED_SAVE_SCHEMA_VERSION;

        FileEnvelope envelope = FileEnvelope.from(state);
        String payload = GSON.toJson(envelope);

        // 1) Write tmp with fsync-ish force.
        writeAtomic(tmp, payload);

        // 2) Rotate existing json -> bak (single backup).
        if (Files.exists(json)) {
            Files.move(json, bak, StandardCopyOption.REPLACE_EXISTING);
        }

        // 3) Move tmp -> json (atomic if supported).
        try {
            Files.move(tmp, json, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, json, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Loads state from <world>/data/cursed/run_state.json (fallback: .bak).
     *
     * Returns Optional.empty() if neither file exists.
     * Throws IOException if files exist but are unreadable/invalid (hard-fail behavior).
     *
     * LOCKED: after load, lifecycleState is forced to PAUSED (restart safety).
     */
    public static Optional<GameState> load(MinecraftServer server) throws IOException {
        Objects.requireNonNull(server, "server");

        Path json = RunStateFilePaths.runStateJson(server);
        Path bak = RunStateFilePaths.runStateBak(server);

        boolean jsonExists = Files.exists(json);
        boolean bakExists = Files.exists(bak);

        if (!jsonExists && !bakExists) {
            return Optional.empty();
        }

        // Try main file first, then fallback to bak.
        if (jsonExists) {
            try {
                GameState loaded = readAndValidate(json);
                forcePaused(loaded);
                return Optional.of(loaded);
            } catch (Exception ignored) {
                // Fallback handled below
            }
        }

        if (bakExists) {
            try {
                GameState loaded = readAndValidate(bak);
                forcePaused(loaded);
                return Optional.of(loaded);
            } catch (Exception e) {
                throw new IOException("CURSED run state load failed (both run_state.json and .bak invalid).", e);
            }
        }

        // If we got here, json existed but was invalid, and no bak exists.
        throw new IOException("CURSED run state load failed (run_state.json invalid and no .bak available).");
    }

    private static void forcePaused(GameState state) {
        // LOCKED restart safety: never load RUNNING
        state.lifecycleState = RunLifecycleState.PAUSED;
    }

    private static GameState readAndValidate(Path path) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);

        final FileEnvelope env;
        try {
            env = GSON.fromJson(raw, FileEnvelope.class);
        } catch (JsonParseException e) {
            throw new IOException("Invalid JSON in " + path.getFileName(), e);
        }

        if (env == null) {
            throw new IOException("Empty/invalid JSON envelope in " + path.getFileName());
        }

        if (env.schemaVersion != GameState.EXPECTED_SAVE_SCHEMA_VERSION) {
            throw new IOException("Unsupported schema_version=" + env.schemaVersion
                    + " (expected " + GameState.EXPECTED_SAVE_SCHEMA_VERSION + ") in " + path.getFileName());
        }

        if (env.state == null) {
            throw new IOException("Missing state object in " + path.getFileName());
        }

        GameState restored = env.state.toGameState();

        // Ensure in-memory schema version matches envelope.
        restored.saveSchemaVersion = env.schemaVersion;

        return restored;

    }

    private static void writeAtomic(Path tmp, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        try (FileChannel ch = FileChannel.open(
                tmp,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            while (buf.hasRemaining()) {
                ch.write(buf);
            }
            // Best-effort durability for the tmp file contents.
            ch.force(true);
        }
    }

    /**
     * JSON envelope format (LOCKED):
     *
     * {
     *   "schema_version": 1,
     *   "state": { ... persisted fields ... }
     * }
     */
    private static final class FileEnvelope {
        @SerializedName("schema_version")
        int schemaVersion;

        @SerializedName("state")
        PersistedState state;

        static FileEnvelope from(GameState state) {
            FileEnvelope e = new FileEnvelope();
            e.schemaVersion = GameState.EXPECTED_SAVE_SCHEMA_VERSION;
            e.state = PersistedState.from(state);
            return e;
        }
    }

    /**
     * Persistence DTO for run_state.json.
     * Uses JSON-friendly keys for stable output and deterministic diffs.
     *
     * NOTE: Runtime models remain in GameState. This is only the serialized shape.
     */
    private static final class PersistedState {

        @SerializedName("run_id")
        String runId; // nullable

        @SerializedName("lifecycle_state")
        String lifecycleState;

        @SerializedName("phase")
        int phase;

        @SerializedName("episode_number")
        int episodeNumber;

        @SerializedName("teams_enabled")
        boolean teamsEnabled;

        @SerializedName("team_count")
        int teamCount;

        @SerializedName("player_teams")
        Map<String, Integer> playerTeams = new HashMap<>();

        // Key: "phase:slot" (e.g. "3:SECONDARY_2")
        @SerializedName("objective_definitions")
        Map<String, ObjectiveDefinition> objectiveDefinitions = new HashMap<>();

        // teamIdx -> ("phase:slot" -> TeamObjectiveState)
        @SerializedName("team_objective_states")
        Map<Integer, Map<String, TeamObjectiveState>> teamObjectiveStates = new HashMap<>();

        static PersistedState from(GameState s) {
            PersistedState p = new PersistedState();

            p.runId = (s.runId == null) ? null : s.runId.toString();
            p.lifecycleState = (s.lifecycleState == null) ? RunLifecycleState.IDLE.name() : s.lifecycleState.name();
            p.phase = s.phase;
            p.episodeNumber = s.episodeNumber;

            p.teamsEnabled = s.teamsEnabled;
            p.teamCount = s.teamCount;

            // UUID -> String
            if (s.playerTeams != null) {
                for (Map.Entry<UUID, Integer> e : s.playerTeams.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        p.playerTeams.put(e.getKey().toString(), e.getValue());
                    }
                }
            }

            // objectiveDefinitions: Map<Integer, Map<ObjectiveSlot, ObjectiveDefinition>> -> Map<String, ObjectiveDefinition>
            if (s.objectiveDefinitions != null) {
                for (Map.Entry<Integer, Map<ObjectiveSlot, ObjectiveDefinition>> byPhase : s.objectiveDefinitions.entrySet()) {
                    Integer phase = byPhase.getKey();
                    if (phase == null || byPhase.getValue() == null) continue;

                    for (Map.Entry<ObjectiveSlot, ObjectiveDefinition> bySlot : byPhase.getValue().entrySet()) {
                        if (bySlot.getKey() == null || bySlot.getValue() == null) continue;
                        String key = phase + ":" + bySlot.getKey().name();
                        p.objectiveDefinitions.put(key, bySlot.getValue());
                    }
                }
            }

            // teamObjectiveStates: Map<Integer, Map<Integer, Map<ObjectiveSlot, TeamObjectiveState>>>
            if (s.teamObjectiveStates != null) {
                for (Map.Entry<Integer, Map<Integer, Map<ObjectiveSlot, TeamObjectiveState>>> byTeam : s.teamObjectiveStates.entrySet()) {
                    Integer teamIdx = byTeam.getKey();
                    if (teamIdx == null || byTeam.getValue() == null) continue;

                    Map<String, TeamObjectiveState> flat = new HashMap<>();
                    for (Map.Entry<Integer, Map<ObjectiveSlot, TeamObjectiveState>> byPhase : byTeam.getValue().entrySet()) {
                        Integer phase = byPhase.getKey();
                        if (phase == null || byPhase.getValue() == null) continue;

                        for (Map.Entry<ObjectiveSlot, TeamObjectiveState> bySlot : byPhase.getValue().entrySet()) {
                            if (bySlot.getKey() == null || bySlot.getValue() == null) continue;
                            String key = phase + ":" + bySlot.getKey().name();
                            flat.put(key, bySlot.getValue());
                        }
                    }

                    p.teamObjectiveStates.put(teamIdx, flat);
                }
            }

            return p;
        }

        GameState toGameState() throws IOException {
            GameState s = new GameState();

            s.runId = (runId == null || runId.isBlank()) ? null : UUID.fromString(runId);
            s.lifecycleState = (lifecycleState == null || lifecycleState.isBlank())
                    ? RunLifecycleState.IDLE
                    : RunLifecycleState.valueOf(lifecycleState);

            s.phase = phase;
            s.episodeNumber = episodeNumber;

            s.teamsEnabled = teamsEnabled;
            s.teamCount = teamCount;

            // String -> UUID
            if (playerTeams != null) {
                for (Map.Entry<String, Integer> e : playerTeams.entrySet()) {
                    if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) continue;
                    s.playerTeams.put(UUID.fromString(e.getKey()), e.getValue());
                }
            }

            // objective definitions: "phase:SLOT"
            if (objectiveDefinitions != null) {
                for (Map.Entry<String, ObjectiveDefinition> e : objectiveDefinitions.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) continue;

                    KeyParts kp = KeyParts.parse(e.getKey());
                    s.objectiveDefinitions
                            .computeIfAbsent(kp.phase, __ -> new HashMap<>())
                            .put(kp.slot, e.getValue());
                }
            }

            // team states: teamIdx -> "phase:SLOT" -> state
            if (teamObjectiveStates != null) {
                for (Map.Entry<Integer, Map<String, TeamObjectiveState>> byTeam : teamObjectiveStates.entrySet()) {
                    Integer teamIdx = byTeam.getKey();
                    if (teamIdx == null || byTeam.getValue() == null) continue;

                    Map<Integer, Map<ObjectiveSlot, TeamObjectiveState>> phaseMap = new HashMap<>();
                    for (Map.Entry<String, TeamObjectiveState> e : byTeam.getValue().entrySet()) {
                        if (e.getKey() == null || e.getValue() == null) continue;

                        KeyParts kp = KeyParts.parse(e.getKey());
                        phaseMap
                                .computeIfAbsent(kp.phase, __ -> new HashMap<>())
                                .put(kp.slot, e.getValue());
                    }

                    s.teamObjectiveStates.put(teamIdx, phaseMap);
                }
            }

            return s;
        }

        private static final class KeyParts {
            final int phase;
            final ObjectiveSlot slot;

            private KeyParts(int phase, ObjectiveSlot slot) {
                this.phase = phase;
                this.slot = slot;
            }

            static KeyParts parse(String key) throws IOException {
                String[] parts = key.split(":", 2);
                if (parts.length != 2) {
                    throw new IOException("Invalid objective key (expected phase:SLOT): " + key);
                }
                int phase;
                try {
                    phase = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid objective key phase: " + key, e);
                }
                ObjectiveSlot slot;
                try {
                    slot = ObjectiveSlot.valueOf(parts[1]);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid objective key slot: " + key, e);
                }
                return new KeyParts(phase, slot);
            }
        }
    }

    /**
     * Gson adapter for java.time.Instant.
     *
     * We persist Instants as ISO-8601 strings via Instant#toString() and restore via Instant#parse.
     * This avoids Gson reflection issues with java.time types on newer Java runtimes.
     */
    private static final class InstantTypeAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String raw = in.nextString();
            return Instant.parse(raw);
        }
    }


}
