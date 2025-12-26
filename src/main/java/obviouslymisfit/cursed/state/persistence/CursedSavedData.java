package obviouslymisfit.cursed.state.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedData;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;

import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

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
                    Codec.INT.fieldOf("episodeNumber").forGetter(d -> d.state.episodeNumber),

                    // --- Teams ---
                    Codec.BOOL.optionalFieldOf("teamsEnabled", false).forGetter(d -> d.state.teamsEnabled),
                    Codec.INT.optionalFieldOf("teamCount", 0).forGetter(d -> d.state.teamCount),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("playerTeams", new HashMap<>())
                            .forGetter(d -> {
                                Map<String, Integer> out = new HashMap<>();
                                d.state.playerTeams.forEach((uuid, teamIdx) -> out.put(uuid.toString(), teamIdx));
                                return out;
                            }),
                    // --- Objective (generated) ---
                    Codec.INT.optionalFieldOf("objectivePhase", 0).forGetter(d -> d.state.objectivePhase),
                    Codec.STRING.optionalFieldOf("objectiveType", "").forGetter(d -> d.state.objectiveType == null ? "" : d.state.objectiveType),
                    Codec.STRING.optionalFieldOf("objectivePoolId", "").forGetter(d -> d.state.objectivePoolId == null ? "" : d.state.objectivePoolId),
                    Codec.list(Codec.STRING).optionalFieldOf("objectiveItems", java.util.List.of()).forGetter(d -> d.state.objectiveItems),
                    Codec.INT.optionalFieldOf("objectiveQuantity", 0).forGetter(d -> d.state.objectiveQuantity)



            ).apply(instance, (saveSchemaVersion, runIdOpt, lifecycleStateName, phase, episodeNumber,
                               teamsEnabled, teamCount, playerTeamsStrMap,
                               objectivePhase, objectiveType, objectivePoolId, objectiveItems, objectiveQuantity) -> {


                CursedSavedData data = new CursedSavedData();
                GameState s = new GameState();

                s.saveSchemaVersion = saveSchemaVersion;
                s.runId = runIdOpt.map(UUID::fromString).orElse(null);

                try {
                    s.lifecycleState = RunLifecycleState.valueOf(lifecycleStateName);
                } catch (Exception ignored) {
                    s.lifecycleState = RunLifecycleState.IDLE;
                }

                s.phase = phase;
                s.episodeNumber = episodeNumber;

                // Teams hydrate
                s.teamsEnabled = teamsEnabled;
                s.teamCount = teamCount;

                s.playerTeams.clear();
                if (playerTeamsStrMap != null) {
                    playerTeamsStrMap.forEach((uuidStr, teamIdx) -> {
                        try {
                            s.playerTeams.put(UUID.fromString(uuidStr), teamIdx);
                        } catch (Exception ignored) {
                            // ignore malformed entries
                        }
                    });
                }

                s.objectivePhase = objectivePhase;
                s.objectiveType = (objectiveType == null || objectiveType.isEmpty()) ? null : objectiveType;
                s.objectivePoolId = (objectivePoolId == null || objectivePoolId.isEmpty()) ? null : objectivePoolId;
                s.objectiveItems = java.util.List.copyOf(objectiveItems == null ? java.util.List.of() : objectiveItems);
                s.objectiveQuantity = objectiveQuantity;

                data.state = s;
                return data;
            })
    );


    private GameState state = new GameState();

    public CursedSavedData() {
    }

    public GameState get() {
        return state;
    }

    public void set(GameState newState) {
        this.state = newState;
        this.setDirty();
    }
}
