package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import obviouslymisfit.cursed.objectives.generator.ObjectiveGenerator;
import obviouslymisfit.cursed.objectives.model.GeneratedObjective;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.state.persistence.StateStorage;
import obviouslymisfit.cursed.objectives.io.ObjectiveContentLoader;
import obviouslymisfit.cursed.objectives.model.ObjectiveContent;


import java.util.UUID;

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
                        src.sendFailure(Component.literal(
                                "CURSED is already running or paused. Use /curse status."
                        ));
                        return 0;
                    }

                    state.runId = UUID.randomUUID();
                    state.lifecycleState = RunLifecycleState.RUNNING;
                    state.phase = 1;
                    state.episodeNumber = 1;

                    StateStorage.save(server, state);

                    src.sendSuccess(() -> Component.literal(
                            "CURSED run started\n" +
                                    "- runId: " + state.runId + "\n" +
                                    "- phase: 1\n" +
                                    "- episode: 1"
                    ), true);

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
                                src.sendFailure(Component.literal(
                                        "CURSED is not paused. Cannot start episode."
                                ));
                                return 0;
                            }

                            state.lifecycleState = RunLifecycleState.RUNNING;
                            StateStorage.save(server, state);

                            src.sendSuccess(() -> Component.literal(
                                    "CURSED episode started. State is now RUNNING."
                            ), true);

                            return 1;
                        }))
                .then(Commands.literal("end")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();

                            GameState state = StateStorage.get(server);

                            if (state.lifecycleState != RunLifecycleState.RUNNING) {
                                src.sendFailure(Component.literal(
                                        "CURSED is not running. Cannot end episode."
                                ));
                                return 0;
                            }

                            state.lifecycleState = RunLifecycleState.PAUSED;
                            StateStorage.save(server, state);

                            src.sendSuccess(() -> Component.literal(
                                    "CURSED episode ended. State is now PAUSED."
                            ), true);

                            return 1;
                        }))
        );

        // /curse reset confirm
        root.then(Commands.literal("reset")
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal(
                            "Reset is destructive. Use: /curse reset confirm"
                    ));
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
                            state.clearObjective();

                            StateStorage.save(server, state);

                            src.sendSuccess(() -> Component.literal(
                                    "CURSED state reset. Back to IDLE."
                            ), true);

                            return 1;
                        }))
        );

        // /curse teams set <count>
        root.then(Commands.literal("teams")
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

                                    src.sendSuccess(() -> Component.literal(
                                            "Teams enabled\n" +
                                                    "Team count set to " + count + "\n" +
                                                    "All team assignments cleared"
                                    ), true);

                                    return 1;
                                })))
        );

        // /curse objectives debug
        root.then(Commands.literal("objectives")
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();

                            ObjectiveContent content = ObjectiveContentLoader.getCachedOrNull();
                            if (content == null) {
                                src.sendFailure(Component.literal("Objective content not loaded."));
                                return 0;
                            }

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

                            src.sendSuccess(() -> Component.literal(
                                    "CURSED objectives debug\n" +
                                            "- pools: " + poolCount + " (e.g. " + firstPool + ")\n" +
                                            "- quantity_rules: " + qtyCount + " (e.g. " + firstQty + ")\n" +
                                            "- generator_rules: " + genCount + " (e.g. phase " + firstGen + ")\n" +
                                            "- hard_constraints: " + hardCount
                            ), false);

                            return 1;
                        }))
                .then(Commands.literal("generate")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();

                            GameState state = StateStorage.get(server);

                            if (state.runId == null) {
                                src.sendFailure(Component.literal("No runId yet. Use /curse start first."));
                                return 0;
                            }

                            ObjectiveContent content = ObjectiveContentLoader.getCachedOrNull();
                            if (content == null) {
                                src.sendFailure(Component.literal("Objective content not loaded."));
                                return 0;
                            }

                            GeneratedObjective obj = ObjectiveGenerator.generatePhase1Primary(content, state.runId);

                            state.objectivePhase = obj.phase;
                            state.objectiveType = obj.objectiveType;
                            state.objectivePoolId = obj.poolId;
                            state.objectiveItems = java.util.List.copyOf(obj.items);
                            state.objectiveQuantity = obj.quantity;

                            StateStorage.save(server, state);

                            src.sendSuccess(() -> Component.literal(
                                    "Generated objective (saved)\n" +
                                            "- phase: " + obj.phase + "\n" +
                                            "- type: " + obj.objectiveType + "\n" +
                                            "- pool: " + obj.poolId + "\n" +
                                            "- items: " + String.join(", ", obj.items) + "\n" +
                                            "- quantity: " + obj.quantity
                            ), false);

                            return 1;
                        }))

        );

        // /curse status
        root.then(Commands.literal("status")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    MinecraftServer server = src.getServer();

                    GameState state = StateStorage.get(server);

                    String runId = (state.runId == null) ? "none" : state.runId.toString();

                    final String teamText;
                    if (!state.teamsEnabled) {
                        teamText = "n/a";
                    } else if (src.getEntity() instanceof ServerPlayer player) {
                        Integer teamIdx = state.playerTeams.get(player.getUUID());
                        teamText = (teamIdx == null) ? "unassigned" : "team " + teamIdx;
                    } else {
                        teamText = "unassigned";
                    }

                    String objectiveLine;
                    if (state.objectiveType == null || state.objectiveItems == null || state.objectiveItems.isEmpty()) {
                        objectiveLine = "- objective: none\n";
                    } else {
                        objectiveLine =
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
                                    objectiveLine +
                                    "- teamsEnabled: " + state.teamsEnabled + "\n" +
                                    "- teamCount: " + state.teamCount + "\n" +
                                    "- team: " + teamText + "\n" +
                                    "- schema: " + state.saveSchemaVersion;


                    src.sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                })
        );

        dispatcher.register(root);
    }



}
