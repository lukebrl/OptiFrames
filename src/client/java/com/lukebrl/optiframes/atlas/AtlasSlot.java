package com.lukebrl.optiframes.atlas;

import net.minecraft.client.render.RenderLayer;

public class AtlasSlot {
    final AtlasPage page;
    final int slotIndex;
    final float[] uvs; // u0, v0, u1, v1
    long colorsHash; // CRC32 hash of last uploaded colors

    AtlasSlot(AtlasPage page, int slotIndex, int slotsPerRow, int mapSize, int atlasSize) {
        this.page = page;
        this.slotIndex = slotIndex;

        // compute UVs
        int col = slotIndex % slotsPerRow;
        int row = slotIndex / slotsPerRow;
        float u0 = (float)(col * mapSize) / atlasSize;
        float v0 = (float)(row * mapSize) / atlasSize;
        float u1 = (float)((col + 1) * mapSize) / atlasSize;
        float v1 = (float)((row + 1) * mapSize) / atlasSize;
        this.uvs = new float[]{u0, v0, u1, v1};
    }

    public RenderLayer getRenderLayer() {
        return this.page.renderLayer;
    }

    public float[] getUVs() {
        return this.uvs;
    }

    public AtlasPage getPage() {
        return this.page;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }
}
