package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.state.persistence.StateStorage;

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
        var episode = Commands.literal("episode");

        episode.then(Commands.literal("start")
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
                }));

        episode.then(Commands.literal("end")
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
                }));

        root.then(episode);

        // /curse reset confirm
        var reset = Commands.literal("reset")
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal(
                            "Reset is destructive. Use: /curse reset confirm"
                    ));
                    return 0;
                });

        reset.then(Commands.literal("confirm")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    MinecraftServer server = src.getServer();

                    GameState state = StateStorage.get(server);

                    state.runId = null;
                    state.lifecycleState = RunLifecycleState.IDLE;
                    state.phase = 0;
                    state.episodeNumber = 0;

                    StateStorage.save(server, state);

                    src.sendSuccess(() -> Component.literal(
                            "CURSED state reset. Back to IDLE."
                    ), true);

                    return 1;
                }));

        root.then(reset);

        // /curse teams set <count>
        var teams = Commands.literal("teams");

        teams.then(Commands.literal("set")
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
                        })));

        root.then(teams);

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

                    src.sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }));

        // Register root
        dispatcher.register(root);
    }
}
