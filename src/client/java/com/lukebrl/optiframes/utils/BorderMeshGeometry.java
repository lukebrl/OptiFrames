package com.lukebrl.optiframes.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;


public final class BorderMeshGeometry {

    // normals for each face
    public static final Vector3f TOP_NORMAL = new Vector3f(0, 1, 0);
    public static final Vector3f BOTTOM_NORMAL = new Vector3f(0, -1, 0);
    public static final Vector3f WEST_NORMAL = new Vector3f(-1, 0, 0);
    public static final Vector3f EAST_NORMAL = new Vector3f(1, 0, 0);
    public static final Vector3f BACK_NORMAL = new Vector3f(0, 0, 1);

    // top border
    public static final Vector3f[] TOP_VERTS = {
        new Vector3f(0f, 128f, 8f),
        new Vector3f(128f, 128f, 8f),
        new Vector3f(128f, 128f, 0f),
        new Vector3f(0f, 128f, 0f)
    };
    public static final float[] TOP_UV = { 0f, 15f/16f, 1f, 1f };

    // bottom border
    public static final Vector3f[] BOTTOM_VERTS = {
        new Vector3f(0f, 0f, 0f),
        new Vector3f(128f, 0f, 0f),
        new Vector3f(128f, 0f, 8f),
        new Vector3f(0f, 0f, 8f)
    };
    public static final float[] BOTTOM_UV = { 0f, 0f, 1f, 1f/16f };

    // left border
    public static final Vector3f[] WEST_VERTS = {
        new Vector3f(0f, 0f, 0f),
        new Vector3f(0f, 0f, 8f),
        new Vector3f(0f, 128f, 8f),
        new Vector3f(0f, 128f, 0f)
    };
    public static final float[] WEST_UV = { 0f, 1f, 1f/16f, 0f };

    // right border
    public static final Vector3f[] EAST_VERTS = {
        new Vector3f(128f, 0f, 8f),
        new Vector3f(128f, 0f, 0f),
        new Vector3f(128f, 128f, 0f),
        new Vector3f(128f, 128f, 8f)
    };
    public static final float[] EAST_UV = { 15f/16f, 1f, 1f, 0f };

    // back inner face (item frame texture)
    public static final Vector3f[] BACK_INNER_VERTS = {
        new Vector3f(8f, 8f, 8f),
        new Vector3f(120f, 8f, 8f),
        new Vector3f(120f, 120f, 8f),
        new Vector3f(8f, 120f, 8f)
    };
    public static final float[] BACK_INNER_UV = { 1f/16f, 15f/16f, 15f/16f, 1f/16f };

    // back border top strip
    public static final Vector3f[] BACK_BORDER_TOP_VERTS = {
        new Vector3f(0f, 120f, 8f),
        new Vector3f(120f, 120f, 8f),
        new Vector3f(120f, 128f, 8f),
        new Vector3f(0f, 128f, 8f)
    };
    public static final float[] BACK_BORDER_TOP_UV = { 0f, 0f, 15f/16f, 1f/16f };

    // back border bottom strip
    public static final Vector3f[] BACK_BORDER_BOTTOM_VERTS = {
        new Vector3f(8f, 0f, 8f),
        new Vector3f(128f, 0f, 8f),
        new Vector3f(128f, 8f, 8f),
        new Vector3f(8f, 8f, 8f)
    };
    public static final float[] BACK_BORDER_BOTTOM_UV = { 1f/16f, 15f/16f, 1f, 1f };
    
    // back border right strip
    public static final Vector3f[] BACK_BORDER_EAST_VERTS = {
        new Vector3f(120f, 8f, 8f),
        new Vector3f(128f, 8f, 8f),
        new Vector3f(128f, 128f, 8f),
        new Vector3f(120f, 128f, 8f)
    };
    public static final float[] BACK_BORDER_EAST_UV = { 15f/16f, 15f/16f, 1f, 0f };

    // back border left strip
    public static final Vector3f[] BACK_BORDER_WEST_VERTS = {
        new Vector3f(0f, 0f, 8f),
        new Vector3f(8f, 0f, 8f),
        new Vector3f(8f, 120f, 8f),
        new Vector3f(0f, 120f, 8f)
    };
    public static final float[] BACK_BORDER_WEST_UV = { 0f, 1f, 1f/16f, 1f/16f };

    private BorderMeshGeometry() {}

    public static void drawQuad(
        VertexConsumer vc,
        PoseStack.Pose entry,
        Vector3f[] verts,
        int light,
        TextureAtlasSprite sprite,
        float[] uvNorm,
        Vector3f normal
    ) {
        // map normalized UVs to sprite atlas coordinates
        float u0 = sprite.getU(uvNorm[0]);
        float v0 = sprite.getV(uvNorm[1]);
        float u1 = sprite.getU(uvNorm[2]);
        float v1 = sprite.getV(uvNorm[3]);

        vc.addVertex(entry, verts[0].x, verts[0].y, verts[0].z)
            .setColor(255, 255, 255, 255).setUv(u0, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(entry, normal.x, normal.y, normal.z);
        vc.addVertex(entry, verts[1].x, verts[1].y, verts[1].z)
            .setColor(255, 255, 255, 255).setUv(u1, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(entry, normal.x, normal.y, normal.z);
        vc.addVertex(entry, verts[2].x, verts[2].y, verts[2].z)
            .setColor(255, 255, 255, 255).setUv(u1, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(entry, normal.x, normal.y, normal.z);
        vc.addVertex(entry, verts[3].x, verts[3].y, verts[3].z)
            .setColor(255, 255, 255, 255).setUv(u0, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(entry, normal.x, normal.y, normal.z);

    }
}
