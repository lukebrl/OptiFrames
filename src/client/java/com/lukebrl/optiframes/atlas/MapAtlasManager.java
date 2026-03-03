package com.lukebrl.optiframes.atlas;

import com.lukebrl.optiframes.OptiFramesClient;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        // query GPU max texture size
        int glMax = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MAX_TEXTURE_SIZE);
        if (glMax > 0) {
            com.lukebrl.optiframes.OptiFramesManager.setMaxAtlasSize(glMax);
        }
        recalculateSize();
    }


    public static void recalculateSize() {
        ATLAS_SIZE = com.lukebrl.optiframes.OptiFramesManager.getAtlasSize();
        SLOTS_PER_ROW = ATLAS_SIZE / MAP_SIZE;
        SLOTS_PER_ATLAS = SLOTS_PER_ROW * SLOTS_PER_ROW;
    }

    public static void updateMap(MapIdComponent mapId, MapState mapState) {
        int id = mapId.id();

        // skip if map was already processed this frame
        if (!updatedThisFrame.add(id)) return;

        AtlasSlot slot = slotMap.get(id);

        if (slot == null) {
            // assign a slot for first time
            slot = allocateSlot(id);
            slotMap.put(id, slot);
            // force initial render
            renderMapToAtlas(slot, mapState.colors);
            slot.lastColors = mapState.colors.clone();
            slot.page.dirty = true;
        } else {
            // check if colors changed (Arrays.equals is JVM-intrinsified with SIMD)
            if (!Arrays.equals(mapState.colors, slot.lastColors)) {
                renderMapToAtlas(slot, mapState.colors);
                System.arraycopy(mapState.colors, 0, slot.lastColors, 0, mapState.colors.length);
                slot.page.dirty = true;
            }
        }
    }

    /**
     * upload dirty/updated atlas pages to GPU
    */
    public static void uploadDirtyPages() {
        // reset per-frame dedup so maps get checked again next frame
        updatedThisFrame.clear();

        for (AtlasPage page : pages) {
            if (page.dirty) {
                page.texture.upload();
                page.dirty = false;
            }
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
    }

    private static AtlasSlot allocateSlot(int mapId) {
        // find a page with free slots
        for (AtlasPage page : pages) {
            if (!page.freeSlots.isEmpty()) {
                int slotIndex = page.freeSlots.removeInt(page.freeSlots.size() - 1);
                page.usedCount++;
                return new AtlasSlot(page, slotIndex);
            }
        }

        // create new page if full
        AtlasPage newPage = new AtlasPage(pages.size());
        pages.add(newPage);
        newPage.usedCount = 1;
        for (int i = SLOTS_PER_ATLAS - 1; i >= 1; i--) {
            newPage.freeSlots.add(i);
        }
        return new AtlasSlot(newPage, 0);
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

    static class AtlasPage {
        final NativeImageBackedTexture texture;
        final Identifier textureId;
        final RenderLayer renderLayer;
        final IntArrayList freeSlots = new IntArrayList();
        boolean dirty = false;
        int usedCount = 0;

        AtlasPage(int pageIndex) {
            this.texture = new NativeImageBackedTexture("optiframes_atlas_" + pageIndex, ATLAS_SIZE, ATLAS_SIZE, false);
            this.textureId = Identifier.of(OptiFramesClient.MOD_ID, "atlas/map_page_" + pageIndex);
            MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, this.texture);
            this.renderLayer = RenderLayers.text(this.textureId);

            // fill with transparent black
            NativeImage image = this.texture.getImage();
            if (image != null) {
                image.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE, 0);
            }
        }

        void close() {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(this.textureId);
            this.texture.close();
        }
    }

    static class AtlasSlot {
        final AtlasPage page;
        final int slotIndex;
        final float[] uvs; // u0, v0, u1, v1
        byte[] lastColors;

        AtlasSlot(AtlasPage page, int slotIndex) {
            this.page = page;
            this.slotIndex = slotIndex;

            // compute UVs for this slot
            int col = slotIndex % SLOTS_PER_ROW;
            int row = slotIndex / SLOTS_PER_ROW;
            float u0 = (float)(col * MAP_SIZE) / ATLAS_SIZE;
            float v0 = (float)(row * MAP_SIZE) / ATLAS_SIZE;
            float u1 = (float)((col + 1) * MAP_SIZE) / ATLAS_SIZE;
            float v1 = (float)((row + 1) * MAP_SIZE) / ATLAS_SIZE;
            this.uvs = new float[]{u0, v0, u1, v1};
        }
    }
}
