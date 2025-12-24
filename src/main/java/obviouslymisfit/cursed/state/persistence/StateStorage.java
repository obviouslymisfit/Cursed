package obviouslymisfit.cursed.state.persistence;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.util.datafix.DataFixTypes;
import obviouslymisfit.cursed.state.GameState;

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

    public static void save(MinecraftServer server, GameState state) {
        CursedSavedData data = getOrCreate(server);
        data.set(state);
    }
}
