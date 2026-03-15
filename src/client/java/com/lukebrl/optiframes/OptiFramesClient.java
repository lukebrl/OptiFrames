package com.lukebrl.optiframes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import com.lukebrl.optiframes.atlas.MapAtlasManager;
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
		MapFrameCacheManager.init();
		MapAtlasManager.init();
		
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MinecraftClient.getInstance().execute(() -> {
				MapAtlasManager.clear();
			});
		});
	}
}