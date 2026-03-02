package com.lukebrl.optiframes;

import net.fabricmc.api.ClientModInitializer;
import com.lukebrl.optiframes.keybind.KeybindRegistry;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptiFramesClient implements ClientModInitializer {
	public static final String MOD_ID = "optiframes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("optiframe initialized");
		
		OptiFramesManager.loadConfig();
		KeybindRegistry.register();
		MapFrameCacheManager.init();
	}
}