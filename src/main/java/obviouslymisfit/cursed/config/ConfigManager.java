package obviouslymisfit.cursed.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and stores server-side config files in the Fabric config directory.
 *
 * v1 rule:
 * - debug.json exists, but defaults to enabled=false.
 * - If file doesn't exist, we create it with defaults.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEBUG_FILE_NAME = "cursed.debug.json";

    private static DebugConfig debug = new DebugConfig();

    private ConfigManager() {}

    public static DebugConfig debug() {
        return debug;
    }

    public static void loadAll() {
        debug = loadOrCreate(DEBUG_FILE_NAME, DebugConfig.class, new DebugConfig());
    }

    private static <T> T loadOrCreate(String fileName, Class<T> type, T defaults) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path file = configDir.resolve(fileName);

        try {
            if (Files.notExists(file)) {
                // Create config directory if needed (Fabric usually ensures it exists, but we don't assume).
                Files.createDirectories(configDir);
                write(file, defaults);
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                T loaded = GSON.fromJson(reader, type);
                return (loaded != null) ? loaded : defaults;
            }
        } catch (Exception e) {
            // Fail-safe: keep defaults if config is malformed/unreadable.
            // We do NOT crash the server in v1 due to debug config.
            return defaults;
        }
    }

    private static void write(Path file, Object data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(data, writer);
        }
    }
}
