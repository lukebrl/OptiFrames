package com.lukebrl.optiframes.utils;

import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.Sprite;

import java.util.Objects;

// this class contains code borrowed from vanilla
// that i use in my render mixin 
public class VanillaRenderHelper {

    // cached decorations atlas
    private static SpriteAtlasTexture decorationsAtlas = null;
    
    public static void addDecorations(MapRenderState renderState, MapState mapState) {
        renderState.decorations.clear();
        for (MapDecoration decoration : mapState.getDecorations()) {
            // set deco atlas
            if (decorationsAtlas == null) {
                decorationsAtlas = MinecraftClient.getInstance().getAtlasManager()
                    .getAtlasTexture(Atlases.MAP_DECORATIONS);
            }
            // vanilla code for decorations
            MapRenderState.Decoration d = new MapRenderState.Decoration();
            d.sprite = decorationsAtlas.getSprite(decoration.getAssetId());
            d.x = decoration.x();
            d.z = decoration.z();
            d.rotation = decoration.rotation();
            d.name = (Text) decoration.name().orElse(null);
            d.alwaysRendered = decoration.isAlwaysRendered();
            renderState.decorations.add(d);
        }
    }

    public static void drawDecorations(MapRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, int light) {
        int i = 0;
        for(MapRenderState.Decoration decoration : renderState.decorations) {
            if (decoration.alwaysRendered) {
                matrices.push();
                matrices.translate((float)decoration.x / 2.0F + 64.0F, (float)decoration.z / 2.0F + 64.0F, -0.02F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(decoration.rotation * 360) / 16.0F));
                matrices.scale(4.0F, 4.0F, 3.0F);
                matrices.translate(-0.125F, 0.125F, 0.0F);
                Sprite sprite = decoration.sprite;
                if (sprite != null) {
                    float f = (float)i * -0.001F;
                    queue.submitCustom(matrices, RenderLayers.text(sprite.getAtlasId()), (matrix, vertexConsumer) -> {
                        vertexConsumer.vertex(matrix, -1.0F, 1.0F, f).color(-1).texture(sprite.getMinU(), sprite.getMinV()).light(light);
                        vertexConsumer.vertex(matrix, 1.0F, 1.0F, f).color(-1).texture(sprite.getMaxU(), sprite.getMinV()).light(light);
                        vertexConsumer.vertex(matrix, 1.0F, -1.0F, f).color(-1).texture(sprite.getMaxU(), sprite.getMaxV()).light(light);
                        vertexConsumer.vertex(matrix, -1.0F, -1.0F, f).color(-1).texture(sprite.getMinU(), sprite.getMaxV()).light(light);
                    });
                    matrices.pop();
                }

                if (decoration.name != null) {
                    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                    float g = (float)textRenderer.getWidth(decoration.name);
                    float var10000 = 25.0F / g;
                    Objects.requireNonNull(textRenderer);
                    float h = MathHelper.clamp(var10000, 0.0F, 6.0F / 9.0F);
                    matrices.push();
                    matrices.translate((float)decoration.x / 2.0F + 64.0F - g * h / 2.0F, (float)decoration.z / 2.0F + 64.0F + 4.0F, -0.025F);
                    matrices.scale(h, h, -1.0F);
                    matrices.translate(0.0F, 0.0F, 0.1F);
                    queue.getBatchingQueue(1).submitText(matrices, 0.0F, 0.0F, decoration.name.asOrderedText(), false, TextLayerType.NORMAL, light, -1, Integer.MIN_VALUE, 0);
                    matrices.pop();
                }

                ++i;
            }
        }
    }
}
