package com.lukebrl.optiframes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.lukebrl.optiframes.atlas.MapAtlasManager;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptiFramesClient implements ClientModInitializer {
	public static final String MOD_ID = "optiframes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static boolean atlasInitialized = false;
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("optiframe initialized");
		
		OptiFramesManager.loadConfig();
		MapFrameCacheManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!atlasInitialized) {
				MapAtlasManager.init();
				atlasInitialized = true;
			}
		});

		// upload any dirty atlas pages
		WorldRenderEvents.END_MAIN.register(context -> {
			MapAtlasManager.uploadDirtyPages();
		});

		// clear atlas when disconnecting from a world
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MapAtlasManager.clear();
		});
	}
}