package obviouslymisfit.cursed.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.persistence.StateStorage;

public final class CursedCommands {

    private CursedCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("curse")
                        .requires(src -> src.hasPermission(2))
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
