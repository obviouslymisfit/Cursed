package obviouslymisfit.cursed;

import net.fabricmc.api.ModInitializer;

import obviouslymisfit.cursed.command.CursedCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import obviouslymisfit.cursed.config.ConfigManager;
import obviouslymisfit.cursed.config.DebugConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import obviouslymisfit.cursed.state.persistence.StateStorage;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;
import obviouslymisfit.cursed.team.TeamScoreboardSync;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import obviouslymisfit.cursed.objectives.data.ObjectivesDataLoader;



public class Cursed implements ModInitializer {
	public static final String MOD_ID = "cursed";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ObjectivesDataLoader.loadAndValidate();
		ConfigManager.loadAll();

		DebugConfig debug = ConfigManager.debug();

		if (debug.enabled) {
			LOGGER.warn("CURSED debug mode ENABLED");
		} else {
			LOGGER.info("CURSED debug mode disabled");
		}

		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			GameState state = StateStorage.get(server);

			// First, ensure all CURSED teams exist (safe to call repeatedly).
			TeamScoreboardSync.sync(server, state);

			// Then, explicitly place the joining player into their team if assigned.
			ServerPlayer player = handler.player;
			Integer teamIdx = state.playerTeams.get(player.getUUID());

			if (teamIdx != null && teamIdx >= 1 && teamIdx <= state.teamCount) {
				var scoreboard = server.getScoreboard();
				var team = scoreboard.getPlayerTeam("curse_team_" + teamIdx);
				if (team != null) {
					scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CursedCommands.register(dispatcher));


	}

	private void onServerStarted(MinecraftServer server) {
		// M1: load run_state.json (fallback .bak) into authoritative in-memory state if present.
		StateStorage.loadFromFileIfPresent(server);

		GameState state = StateStorage.get(server);


		// Schema enforcement: fail loudly if mismatch.
		if (state.saveSchemaVersion != GameState.EXPECTED_SAVE_SCHEMA_VERSION) {
			throw new IllegalStateException(
					"CURSED save schema mismatch. Expected "
							+ GameState.EXPECTED_SAVE_SCHEMA_VERSION
							+ " but found "
							+ state.saveSchemaVersion
			);
		}

		// Restart safety rule: if run exists, force PAUSED (never auto-resume RUNNING).
		if (state.runId != null && state.lifecycleState == RunLifecycleState.RUNNING) {
			state.lifecycleState = RunLifecycleState.PAUSED;
			StateStorage.save(server, state);
			LOGGER.warn("CURSED: forced PAUSED on server restart safety");
		}

		// M2: scoreboard teams are a projection of GameState.
		// Re-apply them on server start so team colors persist across restarts.
		TeamScoreboardSync.sync(server, StateStorage.get(server));

	}


}