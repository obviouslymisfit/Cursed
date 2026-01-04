package obviouslymisfit.cursed.objectives.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fully resolved, persisted objective definition for a specific (phase, slot).
 *
 * Key rules (M1 structure):
 * - This is the resolved, runtime form. It must be persisted and reloaded as-is.
 * - No derivation/re-roll on load.
 * - Exact Minecraft IDs only (namespace:id) for itemId.
 */
public final class ObjectiveDefinition {

    private final int phase;
    private final ObjectiveSlot slotKey;

    private final ObjectiveCategory category;
    private final ObjectiveAction action;

    private final String itemId;
    private final int quantityRequired;

    // Nullable; tasks never use cohesion (and some objectives may not need it).
    private final Cohesion cohesion;

    // Provenance (all persisted)
    private final String templateId;
    private final String poolId;
    private final String quantityRuleId;
    private final List<String> constraintIdsApplied;

    public ObjectiveDefinition(
            int phase,
            ObjectiveSlot slotKey,
            ObjectiveCategory category,
            ObjectiveAction action,
            String itemId,
            int quantityRequired,
            Cohesion cohesion,
            String templateId,
            String poolId,
            String quantityRuleId,
            List<String> constraintIdsApplied
    ) {
        this.phase = phase;
        this.slotKey = Objects.requireNonNull(slotKey, "slotKey");
        this.category = Objects.requireNonNull(category, "category");
        this.action = Objects.requireNonNull(action, "action");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.quantityRequired = quantityRequired;
        this.cohesion = cohesion;

        this.templateId = Objects.requireNonNull(templateId, "templateId");
        this.poolId = Objects.requireNonNull(poolId, "poolId");
        this.quantityRuleId = Objects.requireNonNull(quantityRuleId, "quantityRuleId");

        if (constraintIdsApplied == null) {
            this.constraintIdsApplied = Collections.emptyList();
        } else {
            this.constraintIdsApplied = Collections.unmodifiableList(new ArrayList<>(constraintIdsApplied));
        }
    }

    public int getPhase() {
        return phase;
    }

    public ObjectiveSlot getSlotKey() {
        return slotKey;
    }

    public ObjectiveCategory getCategory() {
        return category;
    }

    public ObjectiveAction getAction() {
        return action;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantityRequired() {
        return quantityRequired;
    }

    public Cohesion getCohesion() {
        return cohesion;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getPoolId() {
        return poolId;
    }

    public String getQuantityRuleId() {
        return quantityRuleId;
    }

    public List<String> getConstraintIdsApplied() {
        return constraintIdsApplied;
    }

    /**
     * Cohesion settings for objectives that require spatial coordination.
     *
     * - DELIVERY_CHEST: deliveries must go into the team's own cohesion chest (later milestone).
     * - GATHER_CLUSTER: gathered items must be collected within a radius around a centroid (later milestone).
     */
    public static final class Cohesion {
        private final CohesionMode mode;
        private final int radiusBlocks;

        public Cohesion(CohesionMode mode, int radiusBlocks) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.radiusBlocks = radiusBlocks;
        }

        public CohesionMode getMode() {
            return mode;
        }

        public int getRadiusBlocks() {
            return radiusBlocks;
        }
    }

    public enum CohesionMode {
        DELIVERY_CHEST,
        GATHER_CLUSTER
    }

    public enum ObjectiveCategory {
        PRIMARY,
        SECONDARY,
        TASK
    }

    public enum ObjectiveAction {
        DELIVER,
        TEAM_GATHER,
        CRAFT,
        SMELT
    }
}
