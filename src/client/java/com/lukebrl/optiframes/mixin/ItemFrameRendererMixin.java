package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import com.lukebrl.optiframes.utils.BorderMeshGeometry;
import com.lukebrl.optiframes.utils.NeighborDirectionMapper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Quaternionf;


@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

    private static final int FULLBRIGHT = (15 << 20) | (15 << 4);

    // pre-computed facing-only rotations
    private static final Quaternionf[] FACING_CACHE = new Quaternionf[6];
    // pre-computed map Z rotations 
    // combines map rotation + 180 flip:
    private static final Quaternionf[] MAP_Z_CACHE = new Quaternionf[4];

    static {
        for (Direction dir : Direction.values()) {
            float pitch, yaw;
            if (dir.getAxis().isHorizontal()) {
                pitch = 0.0F;
                yaw = 180.0F - dir.toYRot();
            } else {
                pitch = (float)(-90 * dir.getAxisDirection().getStep());
                yaw = 180.0F;
            }
            Quaternionf facing = new Quaternionf(Axis.XP.rotationDegrees(pitch));
            facing.mul(Axis.YP.rotationDegrees(yaw));
            FACING_CACHE[dir.ordinal()] = facing;
        }
        for (int rot = 0; rot < 4; rot++) {
            MAP_Z_CACHE[rot] = Axis.ZP.rotationDegrees(rot * 90.0F + 180.0F);
        }
    }

    // get birch planks texture from atlas for frame borders
    private static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    private static final RenderType BORDER_LAYER = RenderTypes.entitySolidZOffsetForward(BLOCK_ATLAS_TEXTURE);

    private static final Material BIRCH_SPRITE_ID = new Material(BLOCK_ATLAS_TEXTURE, Identifier.withDefaultNamespace("block/birch_planks"));
    private static final Material FRAME_SPRITE_ID = new Material(BLOCK_ATLAS_TEXTURE, Identifier.withDefaultNamespace("block/item_frame"));
    private static final Material GLOW_FRAME_SPRITE_ID = new Material(BLOCK_ATLAS_TEXTURE, Identifier.withDefaultNamespace("block/glow_item_frame"));

    // cached sprites
    private static boolean spritesInitialized = false;
    private static TextureAtlasSprite birchSprite = null;
    private static TextureAtlasSprite frameSprite = null;
    private static TextureAtlasSprite glowFrameSprite = null;

    // cached minecraft client
    private static Minecraft MCInstance = null;

    @Shadow 
    @Final
    private MapRenderer mapRenderer;

    @Shadow 
    @Final
    private BlockRenderDispatcher blockRenderer;

    @Shadow
    protected abstract Vec3 getRenderOffset(ItemFrameRenderState state);

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void optiframes$renderMapEntity(
        ItemFrameRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        // check if fix disabled or item is not a map
        if (!OptiFramesManager.isEnabled() || state.mapId == null) return;

        // cache minecraft instance
        if (MCInstance == null) {
            MCInstance = Minecraft.getInstance();
        }

        ci.cancel();

        matrices.pushPose();

        // positioning
        Direction direction = state.direction;
        Vec3 vec3d = this.getRenderOffset(state);
        matrices.translate(
            -vec3d.x() + direction.getStepX() * 0.46875D, 
            -vec3d.y() + (double)direction.getStepY() * 0.46875D, 
            -vec3d.z() + (double)direction.getStepZ() * 0.46875D
        );

        int rotation = state.rotation % 4;
        int light = state.isGlowFrame ? FULLBRIGHT : state.lightCoords;
        boolean useDefault = OptiFramesManager.useDefaultModel();
        boolean renderFrame = OptiFramesManager.isFrameRendered();

        // if vanilla frame model is used
        if (useDefault) {
            // facing rotation only as vanilla model renders before map rotation
            matrices.mulPose(FACING_CACHE[direction.ordinal()]);

            // render vanilla frame model
            if (!state.isInvisible) {
                BlockState blockState = BlockStateDefinitions.getItemFrameFakeState(state.isGlowFrame, true);
                BlockStateModel model = this.blockRenderer.getBlockModel(blockState);
                matrices.pushPose();
                matrices.translate(-0.5F, -0.5F, -0.5F);
                queue.submitBlockModel(matrices, BORDER_LAYER, model, 1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                matrices.popPose();
            }

            matrices.translate(0.0F, 0.0F, state.isInvisible ? 0.5F : 0.4375F);
            matrices.mulPose(MAP_Z_CACHE[rotation]);
        } else {
            matrices.mulPose(FACING_CACHE[direction.ordinal()]);
            matrices.translate(0.0F, 0.0F, state.isInvisible || !renderFrame ? 0.49F : 0.4375F);
        }

        // map scaling logic
        float scale = 0.0078125F; // 1/128
        matrices.scale(scale, scale, scale); // scale down map to world units
        matrices.translate(-64.0F, -64.0F, 0.0F); // center map

        // custom frame borders
        if (!useDefault && !state.isInvisible && renderFrame) {
            if (!spritesInitialized) {
                birchSprite = MCInstance.getAtlasManager().get(BIRCH_SPRITE_ID);
                frameSprite = MCInstance.getAtlasManager().get(FRAME_SPRITE_ID);
                glowFrameSprite = MCInstance.getAtlasManager().get(GLOW_FRAME_SPRITE_ID);
                spritesInitialized = true;
            }

            // item frame entity coordinate
            int bx = Mth.floor(state.x);
            int by = Mth.floor(state.y);
            int bz = Mth.floor(state.z);

            // check neighbor
            boolean hasTop = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, NeighborDirectionMapper.TOP), 
                direction
            );
            boolean hasBottom = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, NeighborDirectionMapper.BOTTOM), 
                direction
            );
            boolean hasLeft = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, NeighborDirectionMapper.WEST), 
                direction
            );
            boolean hasRight = MapFrameCacheManager.hasNeighbor(
                NeighborDirectionMapper.getNeighborPos(bx, by, bz, direction, NeighborDirectionMapper.EAST), 
                direction
            );

            // draw needed borders
            queue.submitCustomGeometry(matrices, BORDER_LAYER, (entry, vc) -> {
                if (!hasTop) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.TOP_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.TOP_UV, BorderMeshGeometry.TOP_NORMAL);
                if (!hasBottom) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BOTTOM_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.BOTTOM_UV, BorderMeshGeometry.BOTTOM_NORMAL);
                if (!hasLeft) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.WEST_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.WEST_UV, BorderMeshGeometry.WEST_NORMAL);
                if (!hasRight) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.EAST_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.EAST_UV, BorderMeshGeometry.EAST_NORMAL);
            
                // back face rendering
                if (OptiFramesManager.isBackRendered()) {
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_INNER_VERTS, state.lightCoords, state.isGlowFrame ? glowFrameSprite : frameSprite, BorderMeshGeometry.BACK_INNER_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_TOP_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.BACK_BORDER_TOP_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_EAST_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.BACK_BORDER_EAST_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_BOTTOM_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.BACK_BORDER_BOTTOM_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_WEST_VERTS, state.lightCoords, birchSprite, BorderMeshGeometry.BACK_BORDER_WEST_UV, BorderMeshGeometry.BACK_NORMAL);
                }
            });
        }

        // this is the rotation logic for the map
        // it's now here to avoid rotating the frame model
        if (!useDefault) {
            matrices.translate(64.0F, 64.0F, 0.0F);
            matrices.mulPose(MAP_Z_CACHE[rotation]);
            matrices.translate(-64.0F, -64.0F, 0.0F);
        }

        // map rendering
        if (this.mapRenderer != null) {
            this.mapRenderer.render(state.mapRenderState, matrices, queue, true, light);
        }

        matrices.popPose();
    }
}