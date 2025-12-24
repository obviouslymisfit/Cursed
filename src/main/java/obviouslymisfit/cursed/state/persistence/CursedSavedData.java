package obviouslymisfit.cursed.state.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedData;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;

import java.util.Optional;
import java.util.UUID;

/**
 * World-attached persistent storage for CURSED (1.21.10 Mojang mappings).
 * Uses SavedDataType + Codec via DimensionDataStorage.
 */
public final class CursedSavedData extends SavedData {

    public static final String STORAGE_ID = "cursed_state_v1";

    /**
     * NOTE: We store UUID as Optional<String> for codec simplicity.
     * We'll validate/parse in code when needed.
     */
    public static final Codec<CursedSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("saveSchemaVersion").forGetter(d -> d.state.saveSchemaVersion),
                    Codec.STRING.optionalFieldOf("runId").forGetter(d ->
                            Optional.ofNullable(d.state.runId).map(UUID::toString)
                    ),
                    Codec.STRING.fieldOf("lifecycleState").forGetter(d -> d.state.lifecycleState.name()),
                    Codec.INT.fieldOf("phase").forGetter(d -> d.state.phase),
                    Codec.INT.fieldOf("episodeNumber").forGetter(d -> d.state.episodeNumber)
            ).apply(instance, (saveSchemaVersion, runIdOpt, lifecycleStateName, phase, episodeNumber) -> {
                CursedSavedData data = new CursedSavedData();
                GameState s = new GameState();

                s.saveSchemaVersion = saveSchemaVersion;

                s.runId = runIdOpt.map(UUID::fromString).orElse(null);

                // Safe parse for enum
                try {
                    s.lifecycleState = RunLifecycleState.valueOf(lifecycleStateName);
                } catch (Exception ignored) {
                    s.lifecycleState = RunLifecycleState.IDLE;
                }

                s.phase = phase;
                s.episodeNumber = episodeNumber;

                data.state = s;
                return data;
            })
    );

    private GameState state = new GameState();

    public CursedSavedData() {}

    public GameState get() {
        return state;
    }

    public void set(GameState newState) {
        this.state = newState;
        this.setDirty();
    }
}
