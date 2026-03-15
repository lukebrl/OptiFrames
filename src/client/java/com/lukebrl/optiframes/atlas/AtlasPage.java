package com.lukebrl.optiframes.atlas;

import com.lukebrl.optiframes.OptiFramesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class AtlasPage {
    final NativeImageBackedTexture texture;
    final Identifier textureId;
    final RenderLayer renderLayer;
    final IntArrayList freeSlots = new IntArrayList();
    boolean initialUploadDone = false;
    int usedCount = 0;

    AtlasPage(int pageIndex, int atlasSize) {
        this.texture = new NativeImageBackedTexture("optiframes_atlas_" + pageIndex, atlasSize, atlasSize, false);
        this.textureId = Identifier.of(OptiFramesClient.MOD_ID, "atlas/map_page_" + pageIndex);
        MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, this.texture);
        this.renderLayer = RenderLayers.text(this.textureId);

        // fill with transparent black
        NativeImage image = this.texture.getImage();
        if (image != null) {
            image.fillRect(0, 0, atlasSize, atlasSize, 0);
        }
    }

    public void addFreeSlot(int slotId) {
        this.freeSlots.push(slotId);
    }

    public void close() {
        MinecraftClient.getInstance().getTextureManager().destroyTexture(this.textureId);
        this.texture.close();
        System.out.println("CLOSED PAGE " + this.textureId.getPath());
    }
}
