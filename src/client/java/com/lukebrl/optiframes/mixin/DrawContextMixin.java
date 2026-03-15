package com.lukebrl.optiframes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.atlas.MapAtlasManager;
import com.lukebrl.optiframes.interfaces.IMapRenderState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @ModifyArgs(
        method = "drawMap",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V",
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
        AbstractTexture atlasTexture = MinecraftClient.getInstance().getTextureManager().getTexture(atlasTextureId);
        if (atlasTexture == null) {
            return;
        }
        args.set(1, atlasTexture.getGlTextureView());
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