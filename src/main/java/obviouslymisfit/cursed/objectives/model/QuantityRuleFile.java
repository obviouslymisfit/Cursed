package obviouslymisfit.cursed.objectives.model;

import java.util.List;

public record QuantityRuleFile(
        int schema,
        String id,
        int phase,
        String tier,
        String shape,
        String mode,
        List<Integer> values
) {}
