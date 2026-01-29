package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import obviouslymisfit.cursed.config.ConfigManager;
import obviouslymisfit.cursed.message.CursedMessages;
import obviouslymisfit.cursed.objectives.runtime.ObjectiveDefinition;
import obviouslymisfit.cursed.objectives.runtime.ObjectiveSlot;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.state.persistence.StateStorage;
import obviouslymisfit.cursed.team.TeamScoreboardSync;


import java.util.*;

public final class CursedCommands {

    private CursedCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        var root = Commands.literal("curse")
                .requires(src -> src.hasPermission(2));

        // /curse start
        root.then(Commands.literal("start")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    MinecraftServer server = src.getServer();

                    GameState state = StateStorage.get(server);

                    if (state.lifecycleState != RunLifecycleState.IDLE) {
                        src.sendFailure(CursedMessages.alreadyRunningOrPaused());
                        return 0;
                    }

                    state.runId = UUID.randomUUID();
                    state.lifecycleState = RunLifecycleState.RUNNING;
                    state.phase = 1;
                    state.episodeNumber = 1;

                    StateStorage.save(server, state);

                    src.sendSuccess(() -> CursedMessages.runStarted(state.runId, state.phase, state.episodeNumber), true);

                    return 1;
                }));

        // /curse episode start|end
        root.then(Commands.literal("episode")
                .then(Commands.literal("start")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();

                            GameState state = StateStorage.get(server);

                            if (state.lifecycleState != RunLifecycleState.PAUSED) {
                                src.sendFailure(CursedMessages.episodeCannotStartNotPaused());
                                return 0;
                            }

                            state.lifecycleState = RunLifecycleState.RUNNING;
                            StateStorage.save(server, state);

                            src.sendSuccess(() -> CursedMessages.episodeStartedNowRunning(), true);

                            return 1;
                        }))
                .then(Commands.literal("end")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();

                            GameState state = StateStorage.get(server);

                            if (state.lifecycleState != RunLifecycleState.RUNNING) {
                                src.sendFailure(CursedMessages.episodeCannotEndNotRunning());
                                return 0;
                            }

                            state.lifecycleState = RunLifecycleState.PAUSED;
                            StateStorage.save(server, state);

                            src.sendSuccess(() -> CursedMessages.episodeEndedNowPaused(), true);

                            return 1;
                        }))
        );


        // /curse reset confirm
        root.then(Commands.literal("reset")
                .executes(ctx -> {
                    ctx.getSource().sendFailure(CursedMessages.resetWarnConfirmRequired());
                    return 0;
                })
                .then(Commands.literal("confirm")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();

                            GameState state = StateStorage.get(server);

                            state.runId = null;
                            state.lifecycleState = RunLifecycleState.IDLE;
                            state.phase = 0;
                            state.episodeNumber = 0;

                            StateStorage.save(server, state);

                            src.sendSuccess(() -> CursedMessages.resetDoneBackToIdle(), true);

                            return 1;
                        }))
        );


        // /curse teams set <count>
        // /curse teams assign <player> <team>
        // /curse teams unassign <player>
        root.then(Commands.literal("teams")
                        // /curse teams set <count>
                        .then(Commands.literal("set")
                                .then(Commands.argument("count", IntegerArgumentType.integer(2, 8))
                                        .executes(ctx -> {
                                            CommandSourceStack src = ctx.getSource();
                                            MinecraftServer server = src.getServer();

                                            int count = IntegerArgumentType.getInteger(ctx, "count");

                                            GameState state = StateStorage.get(server);
                                            state.teamsEnabled = true;
                                            state.teamCount = count;
                                            state.playerTeams.clear();

                                            StateStorage.save(server, state);
                                            TeamScoreboardSync.sync(server, state);

                                            src.sendSuccess(() -> CursedMessages.teamsConfigured(count), true);
                                            return 1;
                                        })))

                        // /curse teams assign <player> <team>
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 8))
                                                .executes(ctx -> {
                                                    CommandSourceStack src = ctx.getSource();
                                                    MinecraftServer server = src.getServer();

                                                    GameState state = StateStorage.get(server);
                                                    if (!state.teamsEnabled) {
                                                        src.sendFailure(CursedMessages.teamsNotConfigured());
                                                        return 0;
                                                    }

                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    int teamIdx = IntegerArgumentType.getInteger(ctx, "team");

                                                    if (teamIdx < 1 || teamIdx > state.teamCount) {
                                                        src.sendFailure(CursedMessages.invalidTeamIndex(teamIdx, state.teamCount));
                                                        return 0;
                                                    }

                                                    state.playerTeams.put(player.getUUID(), teamIdx);
                                                    StateStorage.save(server, state);
                                                    TeamScoreboardSync.sync(server, state);

                                                    src.sendSuccess(() -> CursedMessages.teamAssigned(player, teamIdx), true);
                                                    return 1;
                                                }))))

                // /curse teams unassign <player>
                .then(Commands.literal("unassign")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    MinecraftServer server = src.getServer();

                                    GameState state = StateStorage.get(server);
                                    if (!state.teamsEnabled) {
                                        src.sendFailure(CursedMessages.teamsNotConfigured());
                                        return 0;
                                    }

                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                    boolean removed = (state.playerTeams.remove(player.getUUID()) != null);

                                    StateStorage.save(server, state);
                                    TeamScoreboardSync.sync(server, state);

                                    if (!removed) {
                                        src.sendSuccess(() -> CursedMessages.teamAlreadyUnassigned(player), false);
                                        return 1;
                                    }

                                    src.sendSuccess(() -> CursedMessages.teamUnassigned(player), true);
                                    return 1;
                                })))
                );


        // /curse status
        root.then(Commands.literal("status")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    MinecraftServer server = src.getServer();

                    GameState state = StateStorage.get(server);

                    // Step 1: compute value
                    String computedTeamText = "n/a";
                    if (state.teamsEnabled) {
                        computedTeamText = "unassigned";
                        if (src.getEntity() instanceof ServerPlayer player) {
                            Integer teamIdx = state.playerTeams.get(player.getUUID());
                            if (teamIdx != null) {
                                computedTeamText = "team " + teamIdx;
                            }
                        }
                    }

                    // Step 2: freeze it
                    final String teamText = computedTeamText;

                    // Step 3: safe to use in lambda
                    src.sendSuccess(() -> CursedMessages.status(state, teamText), false);

                    return 1;
                })
        );

        // /curse debug ...
        //
        // Intentionally always registered so operators can *see* the command exists.
        // Execution of debug-only behavior is gated by cursed.debug.json (ConfigManager.debug().enabled).
        // This avoids "ghost commands" and makes it obvious why a debug action is blocked.
        root.then(Commands.literal("debug")
                // /curse debug
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    boolean enabled = ConfigManager.debug().enabled;

                    // Keep the message canonical ("config/...") even in dev, because run/config is just Loom's run dir.
                    src.sendSuccess(() -> CursedMessages.debugStatus(enabled), false);
                    return 1;
                })

                // /curse debug reload
                //
                // Always allowed (even when debug is OFF) so admins can enable debug without restarting the server.
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();

                            // Reload all configs (currently only debug exists, but this stays future-proof).
                            ConfigManager.loadAll();
                            boolean enabled = ConfigManager.debug().enabled;

                            src.sendSuccess(() -> CursedMessages.debugReloaded(enabled), false);
                            return 1;
                        })
                )

                // /curse debug objectives ...
                .then(Commands.literal("objectives")
                        // /curse debug objectives list [phase]
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    return executeObjectivesList(ctx.getSource(), null);
                                })
                                .then(Commands.argument("phase", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> {
                                            int phase = IntegerArgumentType.getInteger(ctx, "phase");
                                            return executeObjectivesList(ctx.getSource(), phase);
                                        })
                                )
                        )
                )

                // /curse debug teams
                .then(Commands.literal("teams")
                        .executes(ctx -> {
                            return executeDebugTeams(ctx.getSource());
                        })
                )

        );


        dispatcher.register(root);
    }

    /**
     * Debug-gated introspection command that prints resolved ObjectiveDefinitions.
     * <p>
     * Intentionally:
     * - blocked (with a clear message) when debug is disabled
     * - stable ordering for repeatable output
     * - read-only: does not mutate run state
     */
    private static int executeObjectivesList(CommandSourceStack src, Integer phaseFilterOrNull) {
        if (!ConfigManager.debug().enabled) {
            src.sendFailure(CursedMessages.debugDisabled());
            return 0;
        }

        MinecraftServer server = src.getServer();
        GameState state = StateStorage.get(server);

        Map<Integer, Map<ObjectiveSlot, ObjectiveDefinition>> defsByPhase = state.objectiveDefinitions;

        if (defsByPhase.isEmpty()) {
            src.sendSuccess(() -> Component.literal("ObjectiveDefinitions: 0 (none)"), false);
            return 1;
        }

        // Sort phases deterministically (ascending).
        List<Integer> phases = new ArrayList<>(defsByPhase.keySet());
        phases.sort(Integer::compareTo);

        int total = 0;

        for (int phase : phases) {
            if (phaseFilterOrNull != null && phase != phaseFilterOrNull) continue;

            Map<ObjectiveSlot, ObjectiveDefinition> defsInPhase = defsByPhase.get(phase);
            if (defsInPhase == null || defsInPhase.isEmpty()) continue;

            // Stable slot order: PRIMARY first, then SECONDARY_n, then TASK_n.
            List<ObjectiveSlot> slots = new ArrayList<>(defsInPhase.keySet());
            slots.sort(Comparator.comparingInt(CursedCommands::slotSortKey));

            src.sendSuccess(() -> Component.literal("Phase " + phase + " (" + defsInPhase.size() + "):"), false);

            for (ObjectiveSlot slot : slots) {
                ObjectiveDefinition def = defsInPhase.get(slot);
                if (def == null) continue;

                total++;

                // Keep output compact but complete per spec: identity + requirement + cohesion + provenance.
                String cohesionText = (def.getCohesion() == null)
                        ? "none"
                        : (def.getCohesion().getMode() + " r=" + def.getCohesion().getRadiusBlocks());

                String line =
                        "- " + def.getSlotKey()
                                + " | " + def.getCategory()
                                + " | " + def.getAction()
                                + " | " + def.getItemId()
                                + " x" + def.getQuantityRequired()
                                + " | cohesion=" + cohesionText
                                + " | template=" + def.getTemplateId()
                                + " | pool=" + def.getPoolId()
                                + " | qty_rule=" + def.getQuantityRuleId()
                                + " | constraints=" + def.getConstraintIdsApplied();

                src.sendSuccess(() -> Component.literal(line), false);
            }
        }

        final int totalFinal = total;
        src.sendSuccess(() -> Component.literal("ObjectiveDefinitions total: " + totalFinal), false);
        return 1;
    }

    /**
     * Deterministic ordering for ObjectiveSlot output.
     * This avoids random iteration order in maps and makes debug output diff-friendly.
     */
    private static int slotSortKey(ObjectiveSlot slot) {
        String name = slot.name();
        if (name.equals("PRIMARY")) return 0;

        if (name.startsWith("SECONDARY_")) {
            return 100 + parseTrailingInt(name, "SECONDARY_");
        }

        if (name.startsWith("TASK_")) {
            return 200 + parseTrailingInt(name, "TASK_");
        }

        // Fallback: keep unknown slots last but stable.
        return 1000;
    }

    private static int parseTrailingInt(String text, String prefix) {
        try {
            return Integer.parseInt(text.substring(prefix.length()));
        } catch (Exception ignored) {
            return 999;
        }
    }

    /**
     * Debug-gated inspection of team state vs scoreboard state.
     *
     * Prints:
     * - GameState team assignment for the executing player (UUID -> teamIdx)
     * - Player scoreboard name key used for membership
     * - Current scoreboard team for that name (if any)
     * - All existing CURSED scoreboard teams and their configured colors
     *
     * Intentionally read-only: does not mutate state or scoreboard.
     */
    private static int executeDebugTeams(CommandSourceStack src) {
        if (!ConfigManager.debug().enabled) {
            src.sendFailure(CursedMessages.debugDisabled());
            return 0;
        }

        MinecraftServer server = src.getServer();
        GameState state = StateStorage.get(server);

        var scoreboard = server.getScoreboard();

        // Report basic state first.
        src.sendSuccess(() -> Component.literal(
                "CURSED debug teams\n" +
                        "- teamsEnabled: " + state.teamsEnabled + "\n" +
                        "- teamCount: " + state.teamCount + "\n" +
                        "- assignments: " + state.playerTeams.size()
        ), false);

        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendSuccess(() -> Component.literal("Player context: n/a (not executed by a player)"), false);
            return 1;
        }

        UUID uuid = player.getUUID();
        Integer teamIdx = state.playerTeams.get(uuid);

        String scoreboardName = player.getScoreboardName();
        var scoreboardTeam = scoreboard.getPlayersTeam(scoreboardName);

        src.sendSuccess(() -> Component.literal(
                "Player context\n" +
                        "- name: " + player.getName().getString() + "\n" +
                        "- uuid: " + uuid + "\n" +
                        "- state teamIdx: " + (teamIdx == null ? "unassigned" : teamIdx) + "\n" +
                        "- scoreboardName key: " + scoreboardName + "\n" +
                        "- scoreboard team: " + (scoreboardTeam == null ? "none" : scoreboardTeam.getName()) + "\n" +
                        "- scoreboard color: " + (scoreboardTeam == null ? "n/a" : String.valueOf(scoreboardTeam.getColor()))
        ), false);

        // List CURSED teams present in the scoreboard.
        int cursedTeams = 0;
        for (var team : scoreboard.getPlayerTeams()) {
            if (team == null) continue;
            String id = team.getName();
            if (id != null && id.startsWith("curse_team_")) {
                cursedTeams++;
                src.sendSuccess(() -> Component.literal(
                        "- " + id + " color=" + String.valueOf(team.getColor())
                ), false);
            }
        }

        final int cursedTeamsFinal = cursedTeams;
        src.sendSuccess(() -> Component.literal("CURSED scoreboard teams present: " + cursedTeamsFinal), false);

        return 1;
    }


}
