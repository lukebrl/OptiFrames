package com.lukebrl.optiframes.cache;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;

import java.util.concurrent.ConcurrentHashMap;

public final class MapFrameCacheManager {

    private static final ConcurrentHashMap<Long, Direction> CACHE = new ConcurrentHashMap<>(512);

    private MapFrameCacheManager() {}

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemFrameEntity frame) {
                updateFrame(frame);
            }
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ItemFrameEntity frame) {
                CACHE.remove(frame.getBlockPos().asLong());
            }
        });
    }

    public static void onFrameItemChanged(ItemFrameEntity frame) {
        updateFrame(frame);
    }

    private static void updateFrame(ItemFrameEntity frame) {
        long pos = frame.getBlockPos().asLong();
        if (frame.getHeldItemStack().isOf(Items.FILLED_MAP)) {
            CACHE.put(pos, frame.getHorizontalFacing());
        } else {
            CACHE.remove(pos);
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