package com.lukebrl.optiframes.utils;

import org.joml.Vector3f;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;


public final class BorderMeshGeometry {

    // pre computed vertex arrays for each border
    public static final Vector3f[] TOP_VERTS = {
        new Vector3f(0f, 0f, 0f),
        new Vector3f(128f, 0f, 0f),
        new Vector3f(128f, 0f, 8f),
        new Vector3f(0f, 0f, 8f)
    };

    public static final Vector3f[] BOTTOM_VERTS = {
        new Vector3f(0f, 128f, 8f),
        new Vector3f(128f, 128f, 8f),
        new Vector3f(128f, 128f, 0f),
        new Vector3f(0f, 128f, 0f)
    };

    public static final Vector3f[] LEFT_VERTS = {
        new Vector3f(0f, 0f, 8f),
        new Vector3f(0f, 128f, 8f),
        new Vector3f(0f, 128f, 0f),
        new Vector3f(0f, 0f, 0f)
    };

    public static final Vector3f[] RIGHT_VERTS = {
        new Vector3f(128f, 0f, 0f),
        new Vector3f(128f, 128f, 0f),
        new Vector3f(128f, 128f, 8f),
        new Vector3f(128f, 0f, 8f)
    };

    private BorderMeshGeometry() {}

    public static void drawBorder(
        VertexConsumer vc,
        MatrixStack.Entry entry,
        Vector3f[] verts,
        int light,
        float[] uvs,
        int[] rgb
    ) {
        int r = rgb[0], g = rgb[1], b = rgb[2];

        vc.vertex(entry, verts[0].x, verts[0].y, verts[0].z)
          .color(r, g, b, 255).texture(uvs[0], uvs[1])
          .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
        vc.vertex(entry, verts[1].x, verts[1].y, verts[1].z)
          .color(r, g, b, 255).texture(uvs[2], uvs[3])
          .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
        vc.vertex(entry, verts[2].x, verts[2].y, verts[2].z)
          .color(r, g, b, 255).texture(uvs[4], uvs[5])
          .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
        vc.vertex(entry, verts[3].x, verts[3].y, verts[3].z)
          .color(r, g, b, 255).texture(uvs[6], uvs[7])
          .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
    }

    public static void drawBorder(
        VertexConsumer vc,
        MatrixStack.Entry entry,
        Vector3f[] verts,
        int light,
        float[] uvs
    ) {
        drawBorder(vc, entry, verts, light, uvs, new int[]{255, 255, 255});
    }
}
