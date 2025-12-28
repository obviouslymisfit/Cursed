package obviouslymisfit.cursed.message;

import net.minecraft.network.chat.Component;

import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.objectives.model.ObjectiveContent;

import obviouslymisfit.cursed.objectives.constraints.ConstraintViolation;
import obviouslymisfit.cursed.objectives.model.GeneratedObjective;

import java.util.List;
import java.util.UUID;


public final class CursedMessages {
    private CursedMessages() {}

    public static Component alreadyRunningOrPaused() {
        return Component.literal("CURSED is already running or paused. Use /curse status.");
    }

    public static Component runStarted(UUID runId, int phase, int episode) {
        return Component.literal(
                "CURSED run started\n" +
                        "- runId: " + runId + "\n" +
                        "- phase: " + phase + "\n" +
                        "- episode: " + episode
        );
    }

    public static Component episodeCannotStartNotPaused() {
        return Component.literal("CURSED is not paused. Cannot start episode.");
    }

    public static Component episodeStartedNowRunning() {
        return Component.literal("CURSED episode started. State is now RUNNING.");
    }

    public static Component episodeCannotEndNotRunning() {
        return Component.literal("CURSED is not running. Cannot end episode.");
    }

    public static Component episodeEndedNowPaused() {
        return Component.literal("CURSED episode ended. State is now PAUSED.");
    }


    public static Component resetWarnConfirmRequired() {
        return Component.literal("Reset is destructive. Use: /curse reset confirm");
    }

    public static Component resetDoneBackToIdle() {
        return Component.literal("CURSED state reset. Back to IDLE.");
    }


    public static Component teamsConfigured(int count) {
        return Component.literal(
                "Teams enabled\n" +
                        "Team count set to " + count + "\n" +
                        "All team assignments cleared"
        );
    }


    public static Component objectivesDebug(ObjectiveContent content) {
        int poolCount = content.poolsById().size();
        int qtyCount = content.quantityRulesById().size();
        int genCount = content.generatorByPhase().size();
        int hardCount = (content.hardConstraints() == null || content.hardConstraints().rules() == null)
                ? 0 : content.hardConstraints().rules().size();

        String firstPool = content.poolsById().isEmpty()
                ? "none" : content.poolsById().keySet().iterator().next();
        String firstQty = content.quantityRulesById().isEmpty()
                ? "none" : content.quantityRulesById().keySet().iterator().next();
        String firstGen = content.generatorByPhase().isEmpty()
                ? "none" : String.valueOf(content.generatorByPhase().keySet().iterator().next());

        return Component.literal(
                "CURSED objectives debug\n" +
                        "- pools: " + poolCount + " (e.g. " + firstPool + ")\n" +
                        "- quantity_rules: " + qtyCount + " (e.g. " + firstQty + ")\n" +
                        "- generator_rules: " + genCount + " (e.g. phase " + firstGen + ")\n" +
                        "- hard_constraints: " + hardCount
        );
    }

    public static Component objectiveGeneratedSaved(GeneratedObjective obj, List<ConstraintViolation> violations) {
        String constraintsLine = (violations == null || violations.isEmpty())
                ? "- constraints: OK"
                : "- constraints: FAIL (" + violations.size() + ")";

        StringBuilder violationLines = new StringBuilder();
        if (violations != null) {
            for (ConstraintViolation v : violations) {
                violationLines.append("\n  - ").append(v.ruleId()).append(": ").append(v.message());
            }
        }

        return Component.literal(
                "Generated objective (saved)\n" +
                        "- phase: " + obj.phase + "\n" +
                        "- type: " + obj.objectiveType + "\n" +
                        "- pool: " + obj.poolId + "\n" +
                        "- items: " + String.join(", ", obj.items) + "\n" +
                        "- quantity: " + obj.quantity + "\n" +
                        constraintsLine +
                        ((violations == null || violations.isEmpty()) ? "" : violationLines.toString())
        );
    }



    public static Component status(GameState state, String teamText) {
        String runId = (state.runId == null) ? "none" : state.runId.toString();

        String objectiveBlock;
        if (state.objectiveType == null || state.objectiveItems == null || state.objectiveItems.isEmpty()) {
            objectiveBlock = "- objective: none\n";
        } else {
            objectiveBlock =
                    "- objective: " + state.objectiveType + "\n" +
                            "  - phase: " + state.objectivePhase + "\n" +
                            "  - pool: " + state.objectivePoolId + "\n" +
                            "  - items: " + String.join(", ", state.objectiveItems) + "\n" +
                            "  - quantity: " + state.objectiveQuantity + "\n";
        }

        String msg =
                "CURSED status\n" +
                        "- lifecycle: " + state.lifecycleState + "\n" +
                        "- runId: " + runId + "\n" +
                        "- phase: " + state.phase + "\n" +
                        "- episode: " + state.episodeNumber + "\n" +
                        "- teamsEnabled: " + state.teamsEnabled + "\n" +
                        "- teamCount: " + state.teamCount + "\n" +
                        "- team: " + teamText + "\n" +
                        objectiveBlock +
                        "- schema: " + state.saveSchemaVersion;

        return Component.literal(msg);
    }




}
