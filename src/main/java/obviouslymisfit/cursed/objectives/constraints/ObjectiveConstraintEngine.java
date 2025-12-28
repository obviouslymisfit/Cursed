package obviouslymisfit.cursed.objectives.constraints;

import obviouslymisfit.cursed.objectives.model.GeneratedObjective;
import obviouslymisfit.cursed.objectives.model.HardConstraintsFile;

import java.util.*;

public final class ObjectiveConstraintEngine {

    private ObjectiveConstraintEngine() {}

    /**
     * Validates all objectives generated for a single phase.
     * Return empty list = valid.
     */
    public static List<ConstraintViolation> validatePhase(
            int phase,
            HardConstraintsFile constraints,
            List<GeneratedObjective> objectivesInPhase
    ) {
        List<ConstraintViolation> violations = new ArrayList<>();
        if (constraints == null || constraints.rules() == null) return violations;

        for (HardConstraintsFile.Rule rule : constraints.rules()) {
            if (!rule.enabled()) continue;

            if ("no_item_overlap_across_tiers".equals(rule.type())) {
                violations.addAll(validateNoOverlapAcrossTiers(rule.id(), objectivesInPhase));
            }
        }

        return violations;
    }

    /**
     * Rule: no item used in PRIMARY may appear in SECONDARY in same phase,
     * and TASKS must not overlap with PRIMARY/SECONDARY.
     *
     * NOTE: right now you might only have PRIMARY generated — this will still pass.
     */
    private static List<ConstraintViolation> validateNoOverlapAcrossTiers(
            String ruleId,
            List<GeneratedObjective> objectives
    ) {
        Set<String> primaryItems = new HashSet<>();
        Set<String> secondaryItems = new HashSet<>();
        Set<String> taskItems = new HashSet<>();

        if (objectives == null) objectives = List.of();

        for (GeneratedObjective o : objectives) {
            if (o == null || o.items == null) continue;

            for (String item : o.items) {
                if (item == null) continue;
                String norm = item.trim().toLowerCase(Locale.ROOT);

                String t = (o.objectiveType == null) ? "" : o.objectiveType.trim().toUpperCase(Locale.ROOT);
                switch (t) {
                    case "PRIMARY" -> primaryItems.add(norm);
                    case "SECONDARY" -> secondaryItems.add(norm);
                    case "TASK" -> taskItems.add(norm);
                    default -> {
                        // ignore unknown types for now
                    }
                }
            }
        }

        List<ConstraintViolation> out = new ArrayList<>();

        for (String item : primaryItems) {
            if (secondaryItems.contains(item)) {
                out.add(new ConstraintViolation(ruleId,
                        "Item overlaps between PRIMARY and SECONDARY: " + item));
            }
        }

        for (String item : taskItems) {
            if (primaryItems.contains(item) || secondaryItems.contains(item)) {
                out.add(new ConstraintViolation(ruleId,
                        "Item overlaps between TASK and objective: " + item));
            }
        }

        return out;
    }
}
