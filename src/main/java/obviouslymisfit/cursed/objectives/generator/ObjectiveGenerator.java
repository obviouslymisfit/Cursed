package obviouslymisfit.cursed.objectives.generator;

import obviouslymisfit.cursed.objectives.model.GeneratedObjective;
import obviouslymisfit.cursed.objectives.model.ObjectiveContent;
import obviouslymisfit.cursed.objectives.model.PoolFile;
import obviouslymisfit.cursed.objectives.model.QuantityRuleFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class ObjectiveGenerator {

    private ObjectiveGenerator() {}

    public static GeneratedObjective generatePhase1Primary(ObjectiveContent content, UUID runId) {
        if (runId == null) throw new IllegalArgumentException("runId cannot be null");

        // Deterministic RNG seed from UUID bits
        long seed = runId.getMostSignificantBits() ^ runId.getLeastSignificantBits();
        Random rng = new Random(seed);

        // --- Pick pool deterministically (stable order) ---
        List<String> poolIds = new ArrayList<>(content.poolsById().keySet());
        poolIds.sort(Comparator.naturalOrder());
        if (poolIds.isEmpty()) throw new IllegalStateException("No pools loaded");

        String poolId = poolIds.get(rng.nextInt(poolIds.size()));
        PoolFile pool = content.poolsById().get(poolId);
        if (pool == null || pool.items() == null || pool.items().isEmpty()) {
            throw new IllegalStateException("Pool is empty: " + poolId);
        }

        // --- Pick item count (for now: always 1) ---
        // We keep this minimal for Phase1 Primary MVP: one target item.
        int itemCount = 1;

        // --- Pick items deterministically (stable order) ---
        List<String> poolItems = new ArrayList<>(pool.items());
        poolItems.sort(Comparator.naturalOrder());

        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            String item = poolItems.get(rng.nextInt(poolItems.size()));
            chosen.add(item);
        }

        // --- Quantity rule (for now: use the only quantity rule file if present) ---
        int quantity = 1;
        if (!content.quantityRulesById().isEmpty()) {
            List<String> qtyIds = new ArrayList<>(content.quantityRulesById().keySet());
            qtyIds.sort(Comparator.naturalOrder());

            QuantityRuleFile rule = content.quantityRulesById().get(qtyIds.get(0));
            if (rule != null && rule.values() != null && !rule.values().isEmpty()) {
                List<Integer> values = new ArrayList<>(rule.values());
                values.sort(Comparator.naturalOrder());
                quantity = values.get(rng.nextInt(values.size()));
            }
        }

        return new GeneratedObjective(
                1,
                poolId,
                chosen,
                quantity,
                "PRIMARY"
        );
    }
}
