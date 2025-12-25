package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.persistence.StateStorage;

import java.util.UUID;

import obviouslymisfit.cursed.state.RunLifecycleState;


public final class CursedCommands {

    private CursedCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("curse")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("start")
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
                                }))

                        .then(Commands.literal("episode")
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
                                        })))

                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    MinecraftServer server = src.getServer();

                                    GameState state = StateStorage.get(server);

                                    String runId = (state.runId == null) ? "none" : state.runId.toString();

                                    src.sendSuccess(() -> Component.literal(
                                            "CURSED status\n" +
                                                    "- lifecycle: " + state.lifecycleState + "\n" +
                                                    "- runId: " + runId + "\n" +
                                                    "- phase: " + state.phase + "\n" +
                                                    "- episode: " + state.episodeNumber + "\n" +
                                                    "- schema: " + state.saveSchemaVersion
                                    ), false);

                                    return 1;
                                }))
        );
    }
}
