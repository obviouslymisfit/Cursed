package obviouslymisfit.cursed.objectives.model;

import java.util.List;

public record GeneratorPhaseRuleFile(
        int schema,
        int phase,
        Primary primary
) {
    public record Primary(
            List<String> shapes,
            List<String> itemPools,
            int itemCount,
            String quantityRule
    ) {}
}
