package com.lukebrl.optiframes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.lukebrl.optiframes.interfaces.IMapState;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

@Mixin(MapState.class)
public abstract class MapStateMixin implements IMapState {
    
    @Unique
    private RenderLayer optiframes$atlasRenderLayer;

    @Unique
    private float[] optiframes$uvs;

    @Unique
    private Identifier optiframes$atlasTextureId;

    @Unique
    private int optiframes$atlasX;
    
    @Unique
    private int optiframes$atlasY;

    @Override
    public void optiframes$setAtlasRenderLayer(RenderLayer renderLayer) {
        this.optiframes$atlasRenderLayer = renderLayer;
    }
    
    @Override
    public RenderLayer optiframes$getAtlasRenderLayer() {
        return this.optiframes$atlasRenderLayer;
    }

    @Override
    public void optiframes$setUVs(float[] uvs) {
        this.optiframes$uvs = uvs;
    }
    
    @Override
    public float[] optiframes$getUVs() {
        return this.optiframes$uvs;
    }

    @Override
    public void optiframes$setAtlasTextureId(Identifier textureId) {
        this.optiframes$atlasTextureId = textureId;
    }

    @Override
    public Identifier optiframes$getAtlasTextureId() {
        return this.optiframes$atlasTextureId;
    }

    @Override
    public void optiframes$setAtlasX(int x) {
        this.optiframes$atlasX = x;
    }

    @Override
    public int optiframes$getAtlasX() {
        return this.optiframes$atlasX;
    }

    @Override
    public void optiframes$setAtlasY(int y) {
        this.optiframes$atlasY = y;
    }

    @Override
    public int optiframes$getAtlasY() {
        return this.optiframes$atlasY;
    }
}
