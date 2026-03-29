package com.lukebrl.optiframes.mixin;

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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.injection.At;




@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public class MapInstanceMixin {

    @Shadow
    private MapItemSavedData data;
    
    @Shadow
    private boolean requiresUpload;

    @Shadow
    @Final
    @Mutable
    private DynamicTexture texture;

    @Shadow
    @Final
    @Mutable
    Identifier location;

    @Unique
    private MapId optiframes$mapId;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void optiframes$initAtlasTexture(MapTextureManager manager, int id, MapItemSavedData state, CallbackInfo ci) {
        this.optiframes$mapId = new MapId(id);
        MapAtlasManager.updateMap(optiframes$mapId, state);
    }


    @Inject(method = "updateTextureIfNeeded", at = @At("HEAD"), cancellable = true)
    private void optiframes$UpdateMapTexture(CallbackInfo ci) {
        if (OptiFramesManager.isEnabled() && this.requiresUpload) {
            ci.cancel();

            MapAtlasManager.updateMap(this.optiframes$mapId, this.data);
            this.requiresUpload = false;
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
