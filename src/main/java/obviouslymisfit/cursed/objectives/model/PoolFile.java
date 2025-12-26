package obviouslymisfit.cursed.objectives.model;

import java.util.List;

public record PoolFile(
        int schema,
        String id,
        String type,
        List<String> items
) {}
