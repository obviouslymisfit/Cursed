package obviouslymisfit.cursed.objectives.model;

import java.util.List;

public final class GeneratedObjective {

    public final int phase;
    public final String poolId;
    public final List<String> items;
    public final int quantity;
    public final String objectiveType; // e.g. "PRIMARY"

    public GeneratedObjective(
            int phase,
            String poolId,
            List<String> items,
            int quantity,
            String objectiveType
    ) {
        this.phase = phase;
        this.poolId = poolId;
        this.items = List.copyOf(items);
        this.quantity = quantity;
        this.objectiveType = objectiveType;
    }
}
