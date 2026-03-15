package com.lukebrl.optiframes.interfaces;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public interface IMapState {

    void optiframes$setAtlasRenderLayer(RenderLayer renderLayer);
    RenderLayer optiframes$getAtlasRenderLayer();

    void optiframes$setUVs(float[] uvs);
    float[] optiframes$getUVs();

    void optiframes$setAtlasTextureId(Identifier textureId);
    Identifier optiframes$getAtlasTextureId();

    void optiframes$setAtlasX(int x);
    int optiframes$getAtlasX();

    void optiframes$setAtlasY(int y);
    int optiframes$getAtlasY();
}
