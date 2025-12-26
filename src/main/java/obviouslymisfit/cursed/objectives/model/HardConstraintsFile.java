package obviouslymisfit.cursed.objectives.model;

import java.util.List;

public record HardConstraintsFile(
        int schema,
        List<Rule> rules
) {
    public record Rule(
            String id,
            String type,
            boolean enabled
    ) {}
}
