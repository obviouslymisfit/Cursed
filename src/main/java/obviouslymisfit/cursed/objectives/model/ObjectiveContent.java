package obviouslymisfit.cursed.objectives.model;

import java.util.Map;

public record ObjectiveContent(
        Map<String, PoolFile> poolsById,
        Map<String, QuantityRuleFile> quantityRulesById,
        Map<Integer, GeneratorPhaseRuleFile> generatorByPhase,
        HardConstraintsFile hardConstraints
) {}
