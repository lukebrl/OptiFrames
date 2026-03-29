package com.lukebrl.optiframes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.atlas.MapAtlasManager;
import com.lukebrl.optiframes.interfaces.IMapRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @ModifyArgs(
        method = "submitMapRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;submitBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
            ordinal = 0
        )
    )
    private void optiframes$modifyMapQuadArgs(Args args, MapRenderState mapState) {
        if (!OptiFramesManager.isEnabled()) {
            return;
        }

        IMapRenderState state = (IMapRenderState) mapState;
        Identifier atlasTextureId = state.optiframes$getAtlasTextureId();
        float[] uvs = state.optiframes$getUVs();

        if (atlasTextureId == null || uvs == null) {
            return;
        }

        // get atlas texture and swap texture view and sampler
        AbstractTexture atlasTexture = Minecraft.getInstance().getTextureManager().getTexture(atlasTextureId);
        if (atlasTexture == null) {
            return;
        }
        args.set(1, atlasTexture.getTextureView());
        args.set(2, atlasTexture.getSampler());

        // swap UV coordinates
        int atlasX = state.optiframes$getAtlasX();
        int atlasY = state.optiframes$getAtlasY();
        int mapSize = MapAtlasManager.getMapSize();
        int atlasSize = MapAtlasManager.getAtlasSize();
        args.set(7, (float) atlasX / atlasSize );  // u1
        args.set(9, (float) atlasY / atlasSize);  // v1
        args.set(8, (float) (atlasX + mapSize) / atlasSize );  // u2
        args.set(10, (float) (atlasY + mapSize) / atlasSize); // v2
    }
}