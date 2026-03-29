package com.lukebrl.optiframes.cache;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class MapFrameCacheManager {

    private static final Long2ObjectMap<Direction> CACHE = Long2ObjectMaps.synchronize(
        new Long2ObjectOpenHashMap<>(512)
    );

    private MapFrameCacheManager() {}

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemFrame frame) {
                updateFrame(frame);
            }
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ItemFrame frame) {
                CACHE.remove(frame.getPos().asLong());
            }
        });
    }

    public static void onFrameItemChanged(ItemFrame frame) {
        updateFrame(frame);
    }

    private static void updateFrame(ItemFrame frame) {
        long pos = frame.blockPosition().asLong();
        if (frame.getItem().is(Items.FILLED_MAP)) {
            CACHE.put(pos, frame.getDirection());
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