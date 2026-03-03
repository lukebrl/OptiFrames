package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.atlas.MapAtlasManager;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import com.lukebrl.optiframes.utils.BorderMeshGeometry;
import com.lukebrl.optiframes.utils.NeighborDirectionMapper;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.font.TextRenderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;


@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameRendererMixin {

    private static final int FULLBRIGHT = (15 << 20) | (15 << 4);

    // get birch planks texture from atlas for frame borders
    private static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/blocks.png");
    private static final Identifier BIRCH_ID = Identifier.ofVanilla("block/birch_planks");
    private static final SpriteIdentifier BIRCH_SPRITE_ID = new SpriteIdentifier(BLOCK_ATLAS_TEXTURE, BIRCH_ID);
    private static final RenderLayer BORDER_LAYER = RenderLayers.entitySolidZOffsetForward(BLOCK_ATLAS_TEXTURE);

    // cached sprite, UVs and border color
    private static boolean lastIsTextureRendered;
    private static Sprite birchSprite = null;
    private static final float[] UVS = new float[8];
    private static final int[] BORDER_COLOR = new int[3];

    // cached decorations atlas
    private static SpriteAtlasTexture decorationsAtlas = null;

    // cached minecraft client
    private static MinecraftClient MCInstance = null;

    @Shadow @Final
    private MapRenderer mapRenderer;

    @Shadow @Final
    private BlockRenderManager blockRenderManager;

    @Shadow
    protected abstract Vec3d getPositionOffset(ItemFrameEntityRenderState state);

    @Redirect(
        method = "updateRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/MapRenderer;update(Lnet/minecraft/component/type/MapIdComponent;Lnet/minecraft/item/map/MapState;Lnet/minecraft/client/render/MapRenderState;)V")
    )
    private void optiframes$redirectMapUpdate(
        MapRenderer renderer,
        MapIdComponent mapId,
        MapState mapState,
        MapRenderState renderState
    ) {
        if (OptiFramesManager.isEnabled()) {
            // update map in our atlas
            MapAtlasManager.updateMap(mapId, mapState);

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
        } else {
            renderer.update(mapId, mapState, renderState);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void optiframes$renderMapEntity(
        ItemFrameEntityRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        // check if fix disabled or item is not a map
        if (!OptiFramesManager.isEnabled() || state.mapId == null) return;

        // cache minecraft instance
        if (MCInstance == null) {
            MCInstance = MinecraftClient.getInstance();
        }

        ci.cancel();

        matrices.push();

        // positioning
        Direction direction = state.facing;
        Vec3d vec3d = this.getPositionOffset(state);
        matrices.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
        // move it out of the block (15/32 = 0.46875)
        matrices.translate(
            (double)direction.getOffsetX() * 0.46875D, 
            (double)direction.getOffsetY() * 0.46875D, 
            (double)direction.getOffsetZ() * 0.46875D
        );
        // makes the render face outward wall/floor
        float pitch, yaw;
        if (direction.getAxis().isHorizontal()) {
            pitch = 0.0F;
            yaw = 180.0F - direction.getPositiveHorizontalDegrees();
        } else {
            pitch = (float)(-90 * direction.getDirection().offset());
            yaw = 180.0F;
        }
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));

        // handle light if glowing item frame
        int light = state.glow ? FULLBRIGHT : state.light;

        // change map pos if not visible
        matrices.translate(0.0F, 0.0F, state.invisible || !OptiFramesManager.isFrameRendered() ? 0.49F : 0.4375F);

        // map rotate logic
        // rotation = {0, 2, 4, 6} and 360/8 = 45°
        // combined it makes quarter turns
        int rotation = state.rotation % 4;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)rotation * 2 * 360.0F / 8.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));

        // map scaling logic
        float scale = 0.0078125F; // 1/128
        matrices.scale(scale, scale, scale); // scale down map to world units
        matrices.translate(-64.0F, -64.0F, 0.0F); // center map

        // draw frame border if visible
        if (!state.invisible && OptiFramesManager.isFrameRendered()) {
            // cache sprite after first run
            // recache if texture rendering is toggled
            if (birchSprite == null || lastIsTextureRendered != OptiFramesManager.isTextureRendered()) {
                birchSprite = MCInstance.getAtlasManager().getSprite(BIRCH_SPRITE_ID);
                float u0, u1, v0, v1;

                if (!OptiFramesManager.isTextureRendered()) {
                    float ux = birchSprite.getFrameU(0f);
                    float vy = birchSprite.getFrameV(0f);
                    u0 = u1 = ux;
                    v0 = v1 = vy;
                } else {
                    u0 = birchSprite.getFrameU(15f / 16f);
                    u1 = birchSprite.getFrameU(1f);
                    v0 = birchSprite.getFrameV(0f);
                    v1 = birchSprite.getFrameV(1f);
                }

                // cache uvs
                UVS[0] = u0; UVS[1] = v0;
                UVS[2] = u0; UVS[3] = v1;
                UVS[4] = u1; UVS[5] = v1;
                UVS[6] = u1; UVS[7] = v0;
                lastIsTextureRendered = OptiFramesManager.isTextureRendered();
            }

            // item frame entity coordinate
            int bx = (int) Math.floor(state.x);
            int by = (int) Math.floor(state.y);
            int bz = (int) Math.floor(state.z);

            // check neighbor
            boolean hasTop = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, rotation, NeighborDirectionMapper.TOP), 
                direction
            );
            boolean hasBottom = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, rotation, NeighborDirectionMapper.BOTTOM), 
                direction
            );
            boolean hasLeft = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, rotation, NeighborDirectionMapper.LEFT), 
                direction
            );
            boolean hasRight = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, rotation, NeighborDirectionMapper.RIGHT), 
                direction
            );

            if (!OptiFramesManager.isTextureRendered()) {
                BORDER_COLOR[0] = 181;
                BORDER_COLOR[1] = 164;
                BORDER_COLOR[2] = 115;
            } else {
                BORDER_COLOR[0] = 255;
                BORDER_COLOR[1] = 255;
                BORDER_COLOR[2] = 255;
            }

            queue.submitCustom(matrices, BORDER_LAYER, (entry, vc) -> {
                // draw borders that don't have neighbors
                if (!hasTop) BorderMeshGeometry.drawBorder(vc, entry, BorderMeshGeometry.TOP_VERTS, light, UVS, BORDER_COLOR);
                if (!hasBottom) BorderMeshGeometry.drawBorder(vc, entry, BorderMeshGeometry.BOTTOM_VERTS, light, UVS, BORDER_COLOR);
                if (!hasLeft) BorderMeshGeometry.drawBorder(vc, entry, BorderMeshGeometry.LEFT_VERTS, light, UVS, BORDER_COLOR);
                if (!hasRight) BorderMeshGeometry.drawBorder(vc, entry, BorderMeshGeometry.RIGHT_VERTS, light, UVS, BORDER_COLOR);
            });
        }

        // map rendering
        if (this.mapRenderer != null) {
            // upload any dirty atlas pages before drawing
            MapAtlasManager.uploadDirtyPages();

            RenderLayer atlasLayer = MapAtlasManager.getRenderLayer(state.mapId);
            float[] atlasUVs = MapAtlasManager.getUVs(state.mapId);
            
            boolean drawDecorations = OptiFramesManager.isDecorationsRendered();

            if (atlasLayer != null && atlasUVs != null) {
                // draw map using atlas
                queue.submitCustom(matrices, atlasLayer, (matrix, vertexConsumer) -> {
                    vertexConsumer.vertex(matrix, 0.0F, 128.0F, -0.01F).color(-1).texture(atlasUVs[0], atlasUVs[3]).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 128.0F, -0.01F).color(-1).texture(atlasUVs[2], atlasUVs[3]).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 0.0F, -0.01F).color(-1).texture(atlasUVs[2], atlasUVs[1]).light(light);
                    vertexConsumer.vertex(matrix, 0.0F, 0.0F, -0.01F).color(-1).texture(atlasUVs[0], atlasUVs[1]).light(light);
                });

                
                
                
                if (drawDecorations) {
                    MapRenderState renderState = state.mapRenderState;

                    // vanilla decorations code
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

            } else {
                // fallback if atlas not working
                this.mapRenderer.draw(state.mapRenderState, matrices, queue, drawDecorations, light);
            }
        }

        matrices.pop();
    }
}