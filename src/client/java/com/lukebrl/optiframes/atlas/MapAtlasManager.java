package com.lukebrl.optiframes.atlas;

import net.minecraft.block.MapColor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * packs all map textures into shared atlas textures so that every map quad
 * can be drawn with the same RenderLayer, allowing the GPU to batch them
 * into a single draw call
 * 
 * creates new AtlasPage if last one is full
 */
public final class MapAtlasManager {

    // atlas dimensions
    private static int ATLAS_SIZE = 4096;
    static final int MAP_SIZE = 128;
    private static int SLOTS_PER_ROW = ATLAS_SIZE / MAP_SIZE;
    private static int SLOTS_PER_ATLAS = SLOTS_PER_ROW * SLOTS_PER_ROW;

    // precomputed color lookup table (256 color bytes -> ARGB)
    private static final int[] COLOR_LUT = new int[256];

    // reusable CRC32 instance
    private static final CRC32 crc = new CRC32();

    private static final List<AtlasPage> pages = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<AtlasSlot> slotMap = new Int2ObjectOpenHashMap<>();
    private static final IntSet updatedThisFrame = new IntOpenHashSet();

    static {
        // build lookup table for MapColor byte -> ARGB conversion
        for (int i = 0; i < 256; i++) {
            COLOR_LUT[i] = MapColor.getRenderColor(i);
        }
    }

    private MapAtlasManager() {}

    public static void init() {
        recalculateSize();
    }

    public static void recalculateSize() {
        ATLAS_SIZE = com.lukebrl.optiframes.OptiFramesManager.getAtlasSize();
        SLOTS_PER_ROW = ATLAS_SIZE / MAP_SIZE;
        SLOTS_PER_ATLAS = SLOTS_PER_ROW * SLOTS_PER_ROW;
    }

    private static long hashColors(byte[] colors) {
        crc.reset();
        crc.update(colors);
        return crc.getValue();
    }

    public static void updateMap(MapIdComponent mapId, MapState mapState) {
        int id = mapId.id();

        // skip if map was already processed this frame
        if (!updatedThisFrame.add(id)) return;

        AtlasSlot slot = slotMap.get(id);
        long hash = hashColors(mapState.colors);

        if (slot == null) {
            // assign a slot for first time
            slot = allocateSlot(id);
            slotMap.put(id, slot);
            // force initial render
            renderMapToAtlas(slot, mapState.colors);
            slot.colorsHash = hash;
            slot.page.dirtySlots.add(slot.slotIndex);
        } else {
            // check if colors changed via hash comparison
            if (hash != slot.colorsHash) {
                renderMapToAtlas(slot, mapState.colors);
                slot.colorsHash = hash;
                slot.page.dirtySlots.add(slot.slotIndex);
            }
        }
    }

    /**
     * upload dirty/updated atlas slots to GPU
     * uses sub-region writeToTexture to upload only changed 128x128 slots
     * instead of re-uploading entire atlas texture
     * this remove lag spike when updating map
    */
    public static void uploadDirtyPages() {
        // reset per-frame dedup so maps get checked again next frame
        updatedThisFrame.clear();

        for (AtlasPage page : pages) {
            if (page.dirtySlots.isEmpty()) continue;

            NativeImage image = page.texture.getImage();
            var glTexture = page.texture.getGlTexture();
            if (image == null || glTexture == null) { page.dirtySlots.clear(); continue; }

            if (!page.initialUploadDone) {
                // full upload to initialize texture
                page.texture.upload();
                page.initialUploadDone = true;
            } else {
                // only the dirty 128x128 slot regions
                var encoder = RenderSystem.getDevice().createCommandEncoder();
                for (int i = 0; i < page.dirtySlots.size(); i++) {
                    int slotIndex = page.dirtySlots.getInt(i);
                    int slotX = (slotIndex % SLOTS_PER_ROW) * MAP_SIZE;
                    int slotY = (slotIndex / SLOTS_PER_ROW) * MAP_SIZE;

                    encoder.writeToTexture(glTexture, image, 0, 0, slotX, slotY, MAP_SIZE, MAP_SIZE, slotX, slotY);
                }
            }

            page.dirtySlots.clear();
        }
    }

    /**
     * get RenderLayer for given map
    */
    public static RenderLayer getRenderLayer(MapIdComponent mapId) {
        AtlasSlot slot = slotMap.get(mapId.id());
        if (slot == null) return null;
        return slot.page.renderLayer;
    }

    /**
     * get UV coords of a map in its atlas page.
     * Returns [u0, v0, u1, v1]
    */
    public static float[] getUVs(MapIdComponent mapId) {
        AtlasSlot slot = slotMap.get(mapId.id());
        if (slot == null) return null;
        return slot.uvs;
    }

    /**
     * check if map already registered in atlas
    */
    public static boolean hasMap(MapIdComponent mapId) {
        return slotMap.containsKey(mapId.id());
    }

    /**
     * remove map from atlas
    */
    public static void removeMap(MapIdComponent mapId) {
        AtlasSlot slot = slotMap.remove(mapId.id());
        if (slot != null) {
            slot.page.freeSlots.add(slot.slotIndex);
            slot.page.usedCount--;
        }
    }

    /**
     * clear all atlas pages
    */
    public static void clear() {
        for (AtlasPage page : pages) {
            page.close();
        }
        pages.clear();
        slotMap.clear();
        updatedThisFrame.clear();
        recalculateSize();
    }

    private static AtlasSlot allocateSlot(int mapId) {
        // find a page with free slots
        for (AtlasPage page : pages) {
            if (!page.freeSlots.isEmpty()) {
                int slotIndex = page.freeSlots.removeInt(page.freeSlots.size() - 1);
                page.usedCount++;
                return new AtlasSlot(page, slotIndex, SLOTS_PER_ROW, MAP_SIZE, ATLAS_SIZE);
            }
        }

        // create new page if full
        AtlasPage newPage = new AtlasPage(pages.size(), ATLAS_SIZE);
        pages.add(newPage);
        newPage.usedCount = 1;
        for (int i = SLOTS_PER_ATLAS - 1; i >= 1; i--) {
            newPage.freeSlots.add(i);
        }
        return new AtlasSlot(newPage, 0, SLOTS_PER_ROW, MAP_SIZE, ATLAS_SIZE);
    }

    private static void renderMapToAtlas(AtlasSlot slot, byte[] colors) {
        NativeImage image = slot.page.texture.getImage();
        if (image == null) return;

        int baseX = (slot.slotIndex % SLOTS_PER_ROW) * MAP_SIZE;
        int baseY = (slot.slotIndex / SLOTS_PER_ROW) * MAP_SIZE;

        for (int z = 0; z < MAP_SIZE; z++) {
            int rowOffset = z * MAP_SIZE;
            int imgY = baseY + z;
            for (int x = 0; x < MAP_SIZE; x++) {
                image.setColorArgb(baseX + x, imgY, COLOR_LUT[colors[rowOffset + x] & 0xFF]);
            }
        }
    }
}
