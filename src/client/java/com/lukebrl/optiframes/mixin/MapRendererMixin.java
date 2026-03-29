package com.lukebrl.optiframes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.interfaces.IMapRenderState;
import com.lukebrl.optiframes.interfaces.IMapState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(MapRenderer.class)
public class MapRendererMixin {
    

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void optiframes$extractRenderState(MapId mapId, MapItemSavedData mapState, MapRenderState renderState, CallbackInfo ci) {
        IMapState iMapState = (IMapState) mapState;
        ((IMapRenderState) renderState).optiframes$setUVs(iMapState.optiframes$getUVs());
        ((IMapRenderState) renderState).optiframes$setAtlasRenderLayer(iMapState.optiframes$getAtlasRenderLayer());
        ((IMapRenderState) renderState).optiframes$setAtlasTextureId(iMapState.optiframes$getAtlasTextureId());
        ((IMapRenderState) renderState).optiframes$setAtlasX(iMapState.optiframes$getAtlasX());
        ((IMapRenderState) renderState).optiframes$setAtlasY(iMapState.optiframes$getAtlasY());
    }


    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;text(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            ordinal = 0
        )
    )
    private RenderType optiframes$redirectRenderType(
            Identifier texture,
            MapRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            boolean renderDecorations,
            int light
    ) {
        if (!OptiFramesManager.isEnabled()) {
            return RenderTypes.text(texture);
        }

        RenderType atlasLayer = ((IMapRenderState) state).optiframes$getAtlasRenderLayer();
        if (atlasLayer != null) {
            return atlasLayer;
        }

        return RenderTypes.text(texture);
    }

    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V",
            ordinal = 0
        )
    )
    private void optiframes$redirectSubmitCustomGeometry(
            SubmitNodeCollector queue,
            PoseStack matrices,
            RenderType layer,
            SubmitNodeCollector.CustomGeometryRenderer renderer,
            MapRenderState state,
            PoseStack matrices2,
            SubmitNodeCollector queue2,
            boolean renderDecorations,
            int light
    ) {
        if (!OptiFramesManager.isEnabled()) {
            queue.submitCustomGeometry(matrices, layer, renderer);
            return;
        }

        IMapRenderState iState = (IMapRenderState) state;
        float[] mapUVs = iState.optiframes$getUVs();

        if (mapUVs == null) {
            queue.submitCustomGeometry(matrices, layer, renderer);
            return;
        }

        queue.submitCustomGeometry(matrices, layer, (matrix, vc) -> {
            vc.addVertex(matrix, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(mapUVs[0], mapUVs[3]).setLight(light);
            vc.addVertex(matrix, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(mapUVs[2], mapUVs[3]).setLight(light);
            vc.addVertex(matrix, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(mapUVs[2], mapUVs[1]).setLight(light);
            vc.addVertex(matrix, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(mapUVs[0], mapUVs[1]).setLight(light);
        });
    }
}
