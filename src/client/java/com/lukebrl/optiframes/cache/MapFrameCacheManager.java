package com.lukebrl.optiframes.cache;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Set;
import java.util.HashSet;

public final class MapFrameCacheManager {


    private static final Long2ObjectMap<Direction> CACHE = new Long2ObjectOpenHashMap<>(512);
    private static final Set<ItemFrameEntity> LOADED_FRAMES = new HashSet<>();

    private MapFrameCacheManager() {}

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemFrameEntity frame) {
                LOADED_FRAMES.add(frame);
            }
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ItemFrameEntity frame) {
                LOADED_FRAMES.remove(frame);
                CACHE.remove(frame.getBlockPos().asLong());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(MapFrameCacheManager::tickUpdate);
    }

    private static void tickUpdate(MinecraftClient client) {
        if (client.world == null) return;

        for (ItemFrameEntity frame : LOADED_FRAMES) {
            if (frame.isRemoved()) continue;
            
            long pos = frame.getBlockPos().asLong();
            
            if (frame.getHeldItemStack().isOf(Items.FILLED_MAP)) {
                CACHE.put(pos, frame.getHorizontalFacing());
            } else {
                CACHE.remove(pos);
            }
        }
    }

    public static boolean hasNeighbor(long posLong, Direction requiredFacing) {
        Direction neighborFacing = CACHE.get(posLong);
        return neighborFacing != null && neighborFacing == requiredFacing;
    }

    public static int getCacheSize() {
        return CACHE.size();
    }
}