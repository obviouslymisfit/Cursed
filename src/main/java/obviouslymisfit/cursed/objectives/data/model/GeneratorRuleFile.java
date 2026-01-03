package obviouslymisfit.cursed.objectives.data.model;

import java.util.List;

public final class GeneratorRuleFile {

    public int phase;

    public Slot primary;
    public Slot secondary;
    public Tasks tasks;
    public Generation generation;

    public static final class Slot {
        public int count;
        public List<String> eligible_templates;
    }

    public static final class Tasks {
        public Range count;
        public List<String> eligible_templates;
        public int cap_per_team;
    }

    public static final class Range {
        public int min;
        public int max;
    }

    public static final class Generation {
        public int retry_budget_total;
        public int retry_budget_per_slot;
    }
}
