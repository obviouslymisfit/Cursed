package obviouslymisfit.cursed;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import obviouslymisfit.cursed.config.ConfigManager;
import obviouslymisfit.cursed.config.DebugConfig;


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
	}

}