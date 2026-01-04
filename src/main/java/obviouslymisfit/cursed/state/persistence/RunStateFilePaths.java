package obviouslymisfit.cursed.state.persistence;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/**
 * Resolves world-scoped file paths for CURSED run state persistence.
 *
 * LOCKED (M1):
 *  - Base folder: <world>/data/cursed/
 *  - Files:
 *      run_state.json
 *      run_state.json.tmp
 *      run_state.json.bak
 *
 * This class ONLY computes paths. It does not create directories or perform IO.
 */
public final class RunStateFilePaths {

    private static final String FOLDER_DATA = "data";
    private static final String FOLDER_CURSED = "cursed";

    private static final String FILE_RUN_STATE = "run_state.json";
    private static final String FILE_RUN_STATE_TMP = "run_state.json.tmp";
    private static final String FILE_RUN_STATE_BAK = "run_state.json.bak";

    private RunStateFilePaths() {}

    /**
     * <world>/data/cursed/
     */
    public static Path cursedDataDir(MinecraftServer server) {
        // World root (the folder containing level.dat, region/, etc.)
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        return worldRoot.resolve(FOLDER_DATA).resolve(FOLDER_CURSED);
    }

    /**
     * <world>/data/cursed/run_state.json
     */
    public static Path runStateJson(MinecraftServer server) {
        return cursedDataDir(server).resolve(FILE_RUN_STATE);
    }

    /**
     * <world>/data/cursed/run_state.json.tmp
     */
    public static Path runStateTmp(MinecraftServer server) {
        return cursedDataDir(server).resolve(FILE_RUN_STATE_TMP);
    }

    /**
     * <world>/data/cursed/run_state.json.bak
     */
    public static Path runStateBak(MinecraftServer server) {
        return cursedDataDir(server).resolve(FILE_RUN_STATE_BAK);
    }
}
