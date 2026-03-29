package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

    @Shadow @Final
    private static EntityDataAccessor<ItemStack> DATA_ITEM;

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void optiframes$onTrackedDataSet(EntityDataAccessor<?> data, CallbackInfo ci) {
        if (data.equals(DATA_ITEM)) {
            MapFrameCacheManager.onFrameItemChanged((ItemFrame) (Object) this);
        }
    }
}
