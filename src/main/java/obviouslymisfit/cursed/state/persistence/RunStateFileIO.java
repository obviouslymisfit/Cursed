package obviouslymisfit.cursed.state.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.MinecraftServer;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;

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

        // Ensure in-memory schema version matches envelope.
        env.state.saveSchemaVersion = env.schemaVersion;

        return env.state;
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
     *   "state": { ... GameState fields ... }
     * }
     */
    private static final class FileEnvelope {
        @SerializedName("schema_version")
        int schemaVersion;

        @SerializedName("state")
        GameState state;

        static FileEnvelope from(GameState state) {
            FileEnvelope e = new FileEnvelope();
            e.schemaVersion = GameState.EXPECTED_SAVE_SCHEMA_VERSION;
            e.state = state;
            return e;
        }
    }
}
