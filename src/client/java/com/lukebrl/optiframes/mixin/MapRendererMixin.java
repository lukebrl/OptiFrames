package com.lukebrl.optiframes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.interfaces.IMapRenderState;
import com.lukebrl.optiframes.interfaces.IMapState;

import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

@Mixin(MapRenderer.class)
public class MapRendererMixin {
    

    @Inject(method = "update", at = @At("HEAD"))
    private void optiframes$updateMapRenderState(MapIdComponent mapId, MapState mapState, MapRenderState renderState, CallbackInfo ci) {
        IMapState iMapState = (IMapState) mapState;
        ((IMapRenderState) renderState).optiframes$setUVs(iMapState.optiframes$getUVs());
        ((IMapRenderState) renderState).optiframes$setAtlasRenderLayer(iMapState.optiframes$getAtlasRenderLayer());
        ((IMapRenderState) renderState).optiframes$setAtlasTextureId(iMapState.optiframes$getAtlasTextureId());
        ((IMapRenderState) renderState).optiframes$setAtlasX(iMapState.optiframes$getAtlasX());
        ((IMapRenderState) renderState).optiframes$setAtlasY(iMapState.optiframes$getAtlasY());
    }


    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayers;text(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;",
            ordinal = 0
        )
    )
    private RenderLayer optiframes$redirectRenderLayer(
            Identifier texture,
            MapRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            boolean renderDecorations,
            int light
    ) {
        if (!OptiFramesManager.isEnabled()) {
            return RenderLayers.text(texture);
        }

        RenderLayer atlasLayer = ((IMapRenderState) state).optiframes$getAtlasRenderLayer();
        if (atlasLayer != null) {
            return atlasLayer;
        }

        return RenderLayers.text(texture);
    }

    
    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V",
            ordinal = 0
        )
    )
    private void optiframes$redirectSubmitCustom(
            OrderedRenderCommandQueue queue,
            MatrixStack matrices,
            RenderLayer layer,
            OrderedRenderCommandQueue.Custom renderer,
            MapRenderState state,
            MatrixStack matrices2,
            OrderedRenderCommandQueue queue2,
            boolean renderDecorations,
            int light
    ) {
        if (!OptiFramesManager.isEnabled()) {
            queue.submitCustom(matrices, layer, renderer);
            return;
        }

        IMapRenderState iState = (IMapRenderState) state;
        float[] mapUVs = iState.optiframes$getUVs();

        if (mapUVs == null) {
            queue.submitCustom(matrices, layer, renderer);
            return;
        }

        queue.submitCustom(matrices, layer, (matrix, vc) -> {
            vc.vertex(matrix, 0.0F, 128.0F, -0.01F).color(-1).texture(mapUVs[0], mapUVs[3]).light(light);
            vc.vertex(matrix, 128.0F, 128.0F, -0.01F).color(-1).texture(mapUVs[2], mapUVs[3]).light(light);
            vc.vertex(matrix, 128.0F, 0.0F, -0.01F).color(-1).texture(mapUVs[2], mapUVs[1]).light(light);
            vc.vertex(matrix, 0.0F, 0.0F, -0.01F).color(-1).texture(mapUVs[0], mapUVs[1]).light(light);
        });
    }
}
