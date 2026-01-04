package obviouslymisfit.cursed.state.persistence;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.util.datafix.DataFixTypes;
import obviouslymisfit.cursed.state.GameState;

import java.io.IOException;
import java.util.Optional;

public final class StateStorage {

    private StateStorage() {}

    // SavedDataType required by DimensionDataStorage
    public static final SavedDataType<CursedSavedData> TYPE =
            new SavedDataType<>(
                    CursedSavedData.STORAGE_ID,
                    (ctx) -> new CursedSavedData(),
                    (ctx) -> CursedSavedData.CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE // safe general-purpose choice
            );

    public static CursedSavedData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static GameState get(MinecraftServer server) {
        return getOrCreate(server).get();
    }

    /**
     * M1: Attempt to load run state from the locked file location:
     *   <world>/data/cursed/run_state.json  (fallback: .bak)
     *
     * If present and valid, it becomes the authoritative in-memory state by setting CursedSavedData.
     * If no file exists, returns Optional.empty().
     *
     * LOCKED: RunStateFileIO forces PAUSED on load.
     *
     * NOTE: This method throws RuntimeException on IO failure to avoid changing existing call sites
     * to handle checked exceptions. That keeps M1 changes contained.
     */
    public static Optional<GameState> loadFromFileIfPresent(MinecraftServer server) {
        try {
            Optional<GameState> loaded = RunStateFileIO.load(server);
            if (loaded.isPresent()) {
                CursedSavedData data = getOrCreate(server);
                data.set(loaded.get());
            }
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException("CURSED: failed to load run_state.json (and .bak fallback).", e);
        }
    }

    /**
     * Existing API: save the authoritative state.
     *
     * M1 addition: also writes the locked JSON file persistence (atomic + .bak).
     *
     * No checked exceptions allowed here because many call sites (commands) already depend on this signature.
     * Failures become RuntimeException so we do not silently continue with an unsaved run.
     */
    public static void save(MinecraftServer server, GameState state) {
        // 1) Locked milestone persistence (immediate, atomic)
        try {
            RunStateFileIO.save(server, state);
        } catch (IOException e) {
            throw new RuntimeException("CURSED: failed to save run_state.json (atomic write).", e);
        }

        // 2) Keep existing behavior (authoritative in-memory source for the rest of the codebase)
        CursedSavedData data = getOrCreate(server);
        data.set(state);
    }
}
