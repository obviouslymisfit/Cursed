package obviouslymisfit.cursed.message;

import net.minecraft.network.chat.Component;

import obviouslymisfit.cursed.state.GameState;

public final class CursedMessages {
    private CursedMessages() {}

    public static Component status(GameState state, String teamText, String objectiveLine) {
        String runId = (state.runId == null) ? "none" : state.runId.toString();

        String msg =
                "CURSED status\n" +
                        "- lifecycle: " + state.lifecycleState + "\n" +
                        "- runId: " + runId + "\n" +
                        "- phase: " + state.phase + "\n" +
                        "- episode: " + state.episodeNumber + "\n" +
                        "- teamsEnabled: " + state.teamsEnabled + "\n" +
                        "- teamCount: " + state.teamCount + "\n" +
                        "- team: " + teamText + "\n" +
                        objectiveLine + "\n" +
                        "- schema: " + state.saveSchemaVersion;

        return Component.literal(msg);
    }
}
