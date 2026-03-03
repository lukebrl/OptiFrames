package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {

    @Shadow @Final
    private static TrackedData<ItemStack> ITEM_STACK;

    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void optiframes$onTrackedDataSet(TrackedData<?> data, CallbackInfo ci) {
        if (data.equals(ITEM_STACK)) {
            MapFrameCacheManager.onFrameItemChanged((ItemFrameEntity) (Object) this);
        }
    }
}
