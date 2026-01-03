package obviouslymisfit.cursed.objectives.data.model;

import java.util.List;

public final class ObjectiveTemplateFile {

    /**
     * We support BOTH keys to avoid unnecessary churn:
     * - "template_id" (currently used in your template JSONs)
     * - "id" (more consistent with pools)
     *
     * Validation will require exactly one resolved ID and it must match the filename.
     */
    public String template_id;
    public String id;

    public String category; // PRIMARY | SECONDARY | TASK
    public String type;     // DELIVER | TEAM_GATHER | CRAFT | SMELT

    public List<String> pool_refs;

    public PickRange pick;

    public List<String> constraints;

    public static final class PickRange {
        public int min;
        public int max;
    }
}
