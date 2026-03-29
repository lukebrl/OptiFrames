package com.lukebrl.optiframes.atlas;

import com.lukebrl.optiframes.OptiFramesClient;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class AtlasPage {
    final DynamicTexture texture;
    final Identifier textureId;
    final RenderType renderLayer;
    final IntArrayList freeSlots = new IntArrayList();
    boolean initialUploadDone = false;
    int usedCount = 0;

    AtlasPage(int pageIndex, int atlasSize) {
        this.texture = new DynamicTexture("optiframes_atlas_" + pageIndex, atlasSize, atlasSize, false);
        this.textureId = Identifier.fromNamespaceAndPath(OptiFramesClient.MOD_ID, "atlas/map_page_" + pageIndex);
        Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
        this.renderLayer = RenderTypes.text(this.textureId);

        // fill with transparent black
        NativeImage image = this.texture.getPixels();
        if (image != null) {
            image.fillRect(0, 0, atlasSize, atlasSize, 0);
        }
    }

    public void addFreeSlot(int slotId) {
        this.freeSlots.push(slotId);
    }

    public void close() {
        Minecraft.getInstance().getTextureManager().release(this.textureId);
        this.texture.close();
        System.out.println("CLOSED PAGE " + this.textureId.getPath());
    }
}
