package obviouslymisfit.cursed.message;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import obviouslymisfit.cursed.state.GameState;

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

    public static Component teamsNotConfigured() {
        return Component.literal(
                "Teams are not configured.\n" +
                        "Run /curse teams set <count> first."
        );
    }

    public static Component invalidTeamIndex(int teamIdx, int teamCount) {
        return Component.literal(
                "Invalid team index: " + teamIdx + "\n" +
                        "Valid range is 1.." + teamCount
        );
    }

    public static Component teamAssigned(ServerPlayer player, int teamIdx) {
        return Component.literal(
                "Assigned " + player.getName().getString() + " to team " + teamIdx
        );
    }

    public static Component teamAlreadyUnassigned(ServerPlayer player) {
        return Component.literal(
                player.getName().getString() + " is already unassigned"
        );
    }

    public static Component teamUnassigned(ServerPlayer player) {
        return Component.literal(
                "Unassigned " + player.getName().getString() + " from their team"
        );
    }


    public static Component status(GameState state, String teamText) {
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
                        "- schema: " + state.saveSchemaVersion;

        return Component.literal(msg);
    }

    public static Component debugDisabled() {
        return Component.literal("CURSED debug is DISABLED. Enable it in config/cursed.debug.json and run /curse debug reload.");
    }

    public static Component debugStatus(boolean enabled) {
        return Component.literal("CURSED debug is " + (enabled ? "ENABLED" : "DISABLED") + " (config/cursed.debug.json)");
    }

    public static Component debugReloaded(boolean enabled) {
        return Component.literal("CURSED debug reloaded: " + (enabled ? "ENABLED" : "DISABLED") + " (config/cursed.debug.json)");
    }

}
