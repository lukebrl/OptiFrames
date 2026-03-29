package com.lukebrl.optiframes.atlas;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.lukebrl.optiframes.interfaces.IMapState;
import com.mojang.blaze3d.platform.NativeImage;
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
    private static final int MAP_SIZE = 128;
    private static int SLOTS_PER_ROW = ATLAS_SIZE / MAP_SIZE;
    private static int SLOTS_PER_ATLAS = SLOTS_PER_ROW * SLOTS_PER_ROW;

    // precomputed color lookup table (256 color bytes -> ARGB)
    private static final int[] COLOR_LUT = new int[256];
    
    // reusable CRC32 instance
    private static final CRC32 crc = new CRC32();

    private static final List<AtlasPage> pages = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<AtlasSlot> slotMap = new Int2ObjectOpenHashMap<>();

    static {
        // build lookup table for MapColor byte -> ARGB conversion
        for (int i = 0; i < 256; i++) {
            COLOR_LUT[i] = MapColor.getColorFromPackedId(i);
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

    public static AtlasSlot updateMap(MapId mapId, MapItemSavedData mapState) {
        int id = mapId.id();

        AtlasSlot slot = slotMap.get(id);

        long hash = hashColors(mapState.colors);

        if (slot == null) {
            // assign a slot for first time
            slot = allocateSlot(id);
            slotMap.put(id, slot);
            renderMapToAtlas(slot, mapState.colors);
            slot.colorsHash = hash;
        } else if (hash != slot.colorsHash) {
            // only rasterize if colors actually changed
            renderMapToAtlas(slot, mapState.colors);
            slot.colorsHash = hash;
        }
        ((IMapState) mapState).optiframes$setAtlasRenderLayer(slot.getRenderLayer());
        ((IMapState) mapState).optiframes$setUVs(slot.getUVs());
        ((IMapState) mapState).optiframes$setAtlasTextureId(slot.page.textureId);
        ((IMapState) mapState).optiframes$setAtlasX((slot.slotIndex % SLOTS_PER_ROW) * MAP_SIZE);
        ((IMapState) mapState).optiframes$setAtlasY((slot.slotIndex / SLOTS_PER_ROW) * MAP_SIZE);

        return slot;
    }


    public static void clear() { 
        for (AtlasPage page : pages) {
            page.close();
        }
        pages.clear();
        slotMap.clear();
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
        NativeImage image = slot.page.texture.getPixels();
        if (image == null) return;

        int baseX = (slot.slotIndex % SLOTS_PER_ROW) * MAP_SIZE;
        int baseY = (slot.slotIndex / SLOTS_PER_ROW) * MAP_SIZE;

        for (int z = 0; z < MAP_SIZE; z++) {
            int rowOffset = z * MAP_SIZE;
            int imgY = baseY + z;
            for (int x = 0; x < MAP_SIZE; x++) {
                image.setPixel(baseX + x, imgY, COLOR_LUT[colors[rowOffset + x] & 0xFF]);
            }
        }

        if (!slot.page.initialUploadDone) {
            // full upload to initialize texture
            slot.page.texture.upload();
            slot.page.initialUploadDone = true;
        } else {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                slot.page.texture.getTexture(), image, 0, 0, 
                baseX, baseY, MAP_SIZE, MAP_SIZE, baseX, baseY
            );
        }
    }

    static public AtlasSlot geAtlasSlot(MapId mapId) {
        int id = mapId.id();
        AtlasSlot slot = slotMap.get(id);
        return slot;
    }

    static public int getAtlasSize() {
        return ATLAS_SIZE;
    }

    static public int getMapSize() {
        return MAP_SIZE;
    }
}
