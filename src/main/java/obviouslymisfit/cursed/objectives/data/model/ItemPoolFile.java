package obviouslymisfit.cursed.objectives.data.model;

import java.util.List;

/**
 * Represents a single item pool file under:
 *   data/cursed/objectives/item_pools/*.json
 *
 * Pools must contain ONLY exact Minecraft IDs (namespace:id).
 * No tags. No semantic logic. No quantities.
 */
public final class ItemPoolFile {

    public String id;

    /** Exact Minecraft item IDs like "minecraft:spruce_planks" */
    public List<String> items;
}
