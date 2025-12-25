package obviouslymisfit.cursed;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import obviouslymisfit.cursed.config.ConfigManager;
import obviouslymisfit.cursed.config.DebugConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import obviouslymisfit.cursed.state.persistence.StateStorage;
import obviouslymisfit.cursed.state.GameState;
import obviouslymisfit.cursed.state.RunLifecycleState;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import obviouslymisfit.cursed.command.CursedCommands;



public class Cursed implements ModInitializer {
	public static final String MOD_ID = "cursed";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.loadAll();

		DebugConfig debug = ConfigManager.debug();

		if (debug.enabled) {
			LOGGER.warn("CURSED debug mode ENABLED");
		} else {
			LOGGER.info("CURSED debug mode disabled");
		}

		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CursedCommands.register(dispatcher));
		obviouslymisfit.cursed.objectives.io.ObjectiveContentLoader.loadAll(LOGGER, MOD_ID);


	}

	private void onServerStarted(MinecraftServer server) {
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
	}


}