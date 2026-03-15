package com.lukebrl.optiframes.mixin;

import net.minecraft.client.texture.MapTextureManager;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.atlas.AtlasSlot;
import com.lukebrl.optiframes.atlas.MapAtlasManager;

import org.spongepowered.asm.mixin.injection.At;




@Mixin(targets = "net.minecraft.client.texture.MapTextureManager$MapTexture")
public class MapTextureMixin {

    @Shadow
    private MapState state;
    
    @Shadow
    private boolean needsUpdate;

    @Shadow
    @Final
    @Mutable
    private NativeImageBackedTexture texture;

    @Shadow
    @Final
    @Mutable
    Identifier textureId;

    @Unique
    private MapIdComponent optiframes$mapId;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void optiframes$initAtlasTexture(MapTextureManager manager, int id, MapState state, CallbackInfo ci) {
        this.optiframes$mapId = new MapIdComponent(id);
        MapAtlasManager.updateMap(optiframes$mapId, state);
    }


    @Inject(method = "updateTexture", at = @At("HEAD"), cancellable = true)
    private void optiframes$UpdateMapTexture(CallbackInfo ci) {
        if (OptiFramesManager.isEnabled() && this.needsUpdate) {
            ci.cancel();

            MapAtlasManager.updateMap(this.optiframes$mapId, this.state);
            this.needsUpdate = false;
        }
    }


    @Inject(method = "close", at = @At("HEAD"))
    private void optiframes$freeSlot(CallbackInfo ci) {
        if (OptiFramesManager.isEnabled()) {
            AtlasSlot slot = MapAtlasManager.geAtlasSlot(this.optiframes$mapId);
            if (slot != null) {
                slot.getPage().addFreeSlot(slot.getSlotIndex());; // set slot reusable
            }
        }
    }

}
