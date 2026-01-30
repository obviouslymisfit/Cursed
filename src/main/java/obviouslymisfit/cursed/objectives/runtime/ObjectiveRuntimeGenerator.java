package obviouslymisfit.cursed.objectives.runtime;

import net.minecraft.server.MinecraftServer;

import obviouslymisfit.cursed.objectives.data.ObjectivesDataLoader;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.objectives.data.model.GeneratorRuleFile;
import obviouslymisfit.cursed.objectives.data.model.ItemPoolFile;
import obviouslymisfit.cursed.objectives.data.model.QuantityRuleFile;
import obviouslymisfit.cursed.objectives.data.model.ObjectiveTemplateFile;

import java.util.Collections;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds the run-time objective definitions for a new run (M2).
 *
 * Invariant:
 * - This is called exactly once when a run starts (IDLE -> RUNNING) and the results are persisted.
 * - It must never run on server start/load or on player join.
 *
 * Inputs:
 * - Validated objective generation data already loaded by {@link ObjectivesDataLoader#loadAndValidate()}.
 * - Run seed stored on {@link GameState#runSeed} for reproducible random decisions.
 *
 * Output:
 * - Populates {@link GameState#objectiveDefinitions} deterministically (phase -> slot -> definition).
 * - Assigns stable, incrementing runtime IDs (starting at 1) to each definition.
 */
public final class ObjectiveRuntimeGenerator {

    private ObjectiveRuntimeGenerator() {
    }

    public static void generate(MinecraftServer server, GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("state");
        }

        // M2 safety: generation must only happen for a brand new run.
        if (!state.objectiveDefinitions.isEmpty()) {
            throw new IllegalStateException("CURSED: objectiveDefinitions already populated; refusing to regenerate.");
        }

        // Use runSeed for any random decisions (even if the first implementation is deterministic).
        Random rng = new Random(state.runSeed);

        int nextRuntimeId = 1;

        Map<Integer, GeneratorRuleFile> rulesByPhase = ObjectivesDataLoader.generatorRulesByPhase();
        Map<String, ObjectiveTemplateFile> templatesById = ObjectivesDataLoader.templatesById();
        Map<String, ItemPoolFile> poolsById = ObjectivesDataLoader.poolsById();
        Map<Integer, Map<String, QuantityRuleFile>> qtyRulesByPhase = ObjectivesDataLoader.quantityRulesByPhase();

        // Deterministic phase order (ascending).
        List<Integer> phases = new ArrayList<>(rulesByPhase.keySet());
        phases.sort(Integer::compareTo);

        for (int phase : phases) {
            GeneratorRuleFile rule = rulesByPhase.get(phase);
            if (rule == null) continue;

            // Decide how many TASK slots this run gets for this phase (seeded).
            int taskCountChosen = 0;
            if (rule.tasks != null && rule.tasks.count != null) {
                int min = rule.tasks.count.min;
                int max = rule.tasks.count.max;
                taskCountChosen = min + rng.nextInt(max - min + 1);
            }

            // Build slot list in stable order.
            List<ObjectiveSlot> slots = new ArrayList<>();
            slots.add(ObjectiveSlot.PRIMARY);

            int secondaryCount = (rule.secondary == null) ? 0 : rule.secondary.count;
            for (int i = 1; i <= secondaryCount; i++) {
                slots.add(ObjectiveSlot.valueOf("SECONDARY_" + i));
            }

            for (int i = 1; i <= taskCountChosen; i++) {
                slots.add(ObjectiveSlot.valueOf("TASK_" + i));
            }

            Map<ObjectiveSlot, ObjectiveDefinition> defsForPhase = new HashMap<>();

            for (ObjectiveSlot slot : slots) {
                // Eligible templates depend on slot type.
                List<String> eligible;
                if (slot == ObjectiveSlot.PRIMARY) {
                    if (rule.primary == null) {
                        throw new IllegalStateException("Phase " + phase + " missing primary rule");
                    }
                    eligible = rule.primary.eligible_templates;
                } else if (slot.name().startsWith("SECONDARY_")) {
                    if (rule.secondary == null) {
                        throw new IllegalStateException("Phase " + phase + " missing secondary rule");
                    }
                    eligible = rule.secondary.eligible_templates;
                } else {
                    if (rule.tasks == null) {
                        throw new IllegalStateException("Phase " + phase + " missing tasks rule");
                    }
                    eligible = rule.tasks.eligible_templates;
                }

                if (eligible == null || eligible.isEmpty()) {
                    throw new IllegalStateException("Phase " + phase + " has no eligible templates for slot " + slot);
                }

                // Deterministic pick: sort list, then rng chooses an index.
                List<String> eligibleSorted = new ArrayList<>(eligible);
                Collections.sort(eligibleSorted);
                String templateId = eligibleSorted.get(rng.nextInt(eligibleSorted.size()));

                ObjectiveTemplateFile template = templatesById.get(templateId);
                if (template.pool_refs == null || template.pool_refs.isEmpty()) {
                    throw new IllegalStateException("Template " + templateId + " has no pool_refs");
                }

                List<String> poolRefsSorted = new ArrayList<>(template.pool_refs);
                Collections.sort(poolRefsSorted);

                String poolId = poolRefsSorted.get(rng.nextInt(poolRefsSorted.size()));

                String resolvedTemplateId = (template.id != null && !template.id.isBlank())
                        ? template.id
                        : template.template_id;

                ItemPoolFile pool = poolsById.get(poolId);
                if (pool == null || pool.items == null || pool.items.isEmpty()) {
                    throw new IllegalStateException("Invalid/empty pool: " + poolId + " (template " + templateId + ")");
                }

                List<String> itemsSorted = new ArrayList<>(pool.items);
                Collections.sort(itemsSorted);
                String itemId = itemsSorted.get(rng.nextInt(itemsSorted.size()));

                // Resolve quantity rule by scanning values in the phase map (ignore map keying).
                QuantityRuleFile matched = null;
                Map<String, QuantityRuleFile> rulesInPhase = qtyRulesByPhase.get(phase);
                if (rulesInPhase != null) {
                    for (QuantityRuleFile qr : rulesInPhase.values()) {
                        if (qr == null) continue;

                        // Match by category+type (per your QuantityRuleFile definition)
                        if (qr.category != null && qr.type != null
                                && qr.category.equals(template.category)
                                && qr.type.equals(template.type)) {
                            matched = qr;
                            break;
                        }

                    }
                }

                if (matched == null) {
                    throw new IllegalStateException("No quantity rule matched for template " + templateId + " in phase " + phase);
                }

                String quantityRuleId = (matched.id != null && !matched.id.isBlank())
                        ? matched.id
                        : matched.rule_id;

                if (!"RANGE".equals(matched.roll_mode)) {
                    throw new IllegalStateException("Unsupported roll_mode: " + matched.roll_mode + " (rule " + (matched.id != null ? matched.id : matched.rule_id) + ")");
                }

                int step = Math.max(1, matched.step);
                int span = matched.max - matched.min;
                int steps = (span / step) + 1;
                int quantity = matched.min + rng.nextInt(steps) * step;

                ObjectiveDefinition def = new ObjectiveDefinition(
                        nextRuntimeId++,
                        phase,
                        slot,
                        ObjectiveDefinition.ObjectiveCategory.valueOf(template.category),
                        ObjectiveDefinition.ObjectiveAction.valueOf(template.type),
                        itemId,
                        quantity,
                        null, // cohesion not present in template files
                        resolvedTemplateId,
                        poolId,
                        quantityRuleId,
                        template.constraints
                );


                defsForPhase.put(slot, def);
            }

            state.objectiveDefinitions.put(phase, defsForPhase);
        }


    }
}
