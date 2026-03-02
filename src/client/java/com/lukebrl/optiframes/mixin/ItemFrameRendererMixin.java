package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import com.lukebrl.optiframes.utils.BorderMeshGeometry;
import com.lukebrl.optiframes.utils.NeighborDirectionMapper;


import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.texture.Sprite;


import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


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

    // cached minecraft client
    private static MinecraftClient MCInstance = null;

    @Shadow @Final
    private MapRenderer mapRenderer;

    @Shadow @Final
    private BlockRenderManager blockRenderManager;

    @Shadow
    protected abstract Vec3d getPositionOffset(ItemFrameEntityRenderState state);


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
        float f, h;
        if (direction.getAxis().isHorizontal()) {
            f = 0.0F;
            h = 180.0F - direction.getPositiveHorizontalDegrees();
        } else {
            f = (float)(-90 * direction.getDirection().offset());
            h = 180.0F;
        }
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h));

        // handle light if glowing item frame
        int light = this.mapRenderer == null ? state.light : (state.glow ? FULLBRIGHT : state.light);

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

        // draw frame if visible
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

        if (this.mapRenderer != null) {
            if (OptiFramesManager.isDecorationsRendered()) {
                this.mapRenderer.draw(state.mapRenderState, matrices, queue, true, light);
            } else {
                queue.submitCustom(matrices, RenderLayers.text(state.mapRenderState.texture), (matrix, vertexConsumer) -> {
                    vertexConsumer.vertex(matrix, 0.0F, 128.0F, -0.01F).color(-1).texture(0.0F, 1.0F).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 128.0F, -0.01F).color(-1).texture(1.0F, 1.0F).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 0.0F, -0.01F).color(-1).texture(1.0F, 0.0F).light(light);
                    vertexConsumer.vertex(matrix, 0.0F, 0.0F, -0.01F).color(-1).texture(0.0F, 0.0F).light(light);
                });
            }
        }

        matrices.pop();
    }
}