package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import obviouslymisfit.cursed.objectives.constraints.ConstraintViolation;
import obviouslymisfit.cursed.objectives.constraints.ObjectiveConstraintEngine;
import obviouslymisfit.cursed.objectives.generator.ObjectiveGenerator;
import obviouslymisfit.cursed.objectives.io.ObjectiveContentLoader;
import obviouslymisfit.cursed.objectives.model.GeneratedObjective;
import obviouslymisfit.cursed.objectives.model.ObjectiveContent;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.state.persistence.StateStorage;
import obviouslymisfit.cursed.message.CursedMessages;

import java.util.List;
import java.util.UUID;

public final class CursedCommands {

    private CursedCommands() {}

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
                            state.clearObjective();

                            StateStorage.save(server, state);

                            src.sendSuccess(() -> CursedMessages.resetDoneBackToIdle(), true);

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

                                    src.sendSuccess(
                                            () -> CursedMessages.teamsConfigured(count),
                                            true
                                    );

                                    return 1;
                                })))
        );


        // /curse objectives debug + generate
        root.then(Commands.literal("objectives")
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();

                            ObjectiveContent content = ObjectiveContentLoader.getCachedOrNull();
                            if (content == null) {
                                src.sendFailure(Component.literal("Objective content not loaded."));
                                return 0;
                            }

                            src.sendSuccess(() -> CursedMessages.objectivesDebug(content), false);
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

                            var constraints = content.hardConstraints();
                            List<ConstraintViolation> violations = ObjectiveConstraintEngine.validatePhase(
                                    obj.phase,
                                    constraints,
                                    java.util.List.of(obj)
                            );

                            // persist objective
                            state.objectivePhase = obj.phase;
                            state.objectiveType = obj.objectiveType;
                            state.objectivePoolId = obj.poolId;
                            state.objectiveItems = java.util.List.copyOf(obj.items);
                            state.objectiveQuantity = obj.quantity;

                            StateStorage.save(server, state);

                            src.sendSuccess(() -> CursedMessages.objectiveGeneratedSaved(obj, violations), false);
                            return 1;
                        }))
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


        dispatcher.register(root);
    }
}
