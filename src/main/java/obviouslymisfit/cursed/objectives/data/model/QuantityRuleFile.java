package obviouslymisfit.cursed.objectives.data.model;

public final class QuantityRuleFile {

    /**
     * Support both keys to avoid churn:
     * - "rule_id" (what you used)
     * - "id" (consistent with pools)
     */
    public String rule_id;
    public String id;

    public int phase;

    public String category; // PRIMARY | SECONDARY | TASK
    public String type;     // DELIVER | TEAM_GATHER | CRAFT | SMELT

    public String roll_mode; // for now we only allow "RANGE"

    public int min;
    public int max;
    public int step;
}
