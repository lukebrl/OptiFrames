package com.lukebrl.optiframes.mixin;

import com.lukebrl.optiframes.OptiFramesManager;
import com.lukebrl.optiframes.atlas.MapAtlasManager;
import com.lukebrl.optiframes.atlas.MapAtlasManager.MapSlotData;
import com.lukebrl.optiframes.cache.MapFrameCacheManager;
import com.lukebrl.optiframes.utils.BorderMeshGeometry;
import com.lukebrl.optiframes.utils.NeighborDirectionMapper;
import com.lukebrl.optiframes.utils.VanillaRenderHelper;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BlockStateManagers;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Quaternionf;


@Mixin(ItemFrameEntityRenderer.class)
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
                yaw = 180.0F - dir.getPositiveHorizontalDegrees();
            } else {
                pitch = (float)(-90 * dir.getDirection().offset());
                yaw = 180.0F;
            }
            Quaternionf facing = new Quaternionf(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            facing.mul(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            FACING_CACHE[dir.ordinal()] = facing;
        }
        for (int rot = 0; rot < 4; rot++) {
            MAP_Z_CACHE[rot] = RotationAxis.POSITIVE_Z.rotationDegrees(rot * 90.0F + 180.0F);
        }
    }

    // get birch planks texture from atlas for frame borders
    private static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/blocks.png");
    private static final RenderLayer BORDER_LAYER = RenderLayers.entitySolidZOffsetForward(BLOCK_ATLAS_TEXTURE);

    private static final SpriteIdentifier BIRCH_SPRITE_ID = new SpriteIdentifier(BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/birch_planks"));
    private static final SpriteIdentifier FRAME_SPRITE_ID = new SpriteIdentifier(BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/item_frame"));
    private static final SpriteIdentifier GLOW_FRAME_SPRITE_ID = new SpriteIdentifier(BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/glow_item_frame"));

    // cached sprites
    private static boolean spritesInitialized = false;
    private static Sprite birchSprite = null;
    private static Sprite frameSprite = null;
    private static Sprite glowFrameSprite = null;

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
            VanillaRenderHelper.addDecorations(renderState, mapState);
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
        matrices.translate(
            -vec3d.getX() + direction.getOffsetX() * 0.46875D, 
            -vec3d.getY() + (double)direction.getOffsetY() * 0.46875D, 
            -vec3d.getZ() + (double)direction.getOffsetZ() * 0.46875D
        );

        int rotation = state.rotation % 4;
        int light = state.glow ? FULLBRIGHT : state.light;
        boolean useDefault = OptiFramesManager.useDefaultModel();
        boolean renderFrame = OptiFramesManager.isFrameRendered();

        // if vanilla frame model is used
        if (useDefault) {
            // facing rotation only as vanilla model renders before map rotation
            matrices.multiply(FACING_CACHE[direction.ordinal()]);

            // render vanilla frame model
            if (!state.invisible) {
                BlockState blockState = BlockStateManagers.getStateForItemFrame(state.glow, true);
                BlockStateModel model = this.blockRenderManager.getModel(blockState);
                matrices.push();
                matrices.translate(-0.5F, -0.5F, -0.5F);
                queue.submitBlockStateModel(matrices, BORDER_LAYER, model, 1.0F, 1.0F, 1.0F, state.light, OverlayTexture.DEFAULT_UV, state.outlineColor);
                matrices.pop();
            }

            matrices.translate(0.0F, 0.0F, state.invisible ? 0.5F : 0.4375F);
            matrices.multiply(MAP_Z_CACHE[rotation]);
        } else {
            matrices.multiply(FACING_CACHE[direction.ordinal()]);
            matrices.translate(0.0F, 0.0F, state.invisible || !renderFrame ? 0.49F : 0.4375F);
        }

        // map scaling logic
        float scale = 0.0078125F; // 1/128
        matrices.scale(scale, scale, scale); // scale down map to world units
        matrices.translate(-64.0F, -64.0F, 0.0F); // center map

        // custom frame borders
        if (!useDefault && !state.invisible && renderFrame) {
            if (!spritesInitialized) {
                birchSprite = MCInstance.getAtlasManager().getSprite(BIRCH_SPRITE_ID);
                frameSprite = MCInstance.getAtlasManager().getSprite(FRAME_SPRITE_ID);
                glowFrameSprite = MCInstance.getAtlasManager().getSprite(GLOW_FRAME_SPRITE_ID);
                spritesInitialized = true;
            }

            // item frame entity coordinate
            int bx = MathHelper.floor(state.x);
            int by = MathHelper.floor(state.y);
            int bz = MathHelper.floor(state.z);

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
            queue.submitCustom(matrices, BORDER_LAYER, (entry, vc) -> {
                if (!hasTop) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.TOP_VERTS, state.light, birchSprite, BorderMeshGeometry.TOP_UV, BorderMeshGeometry.TOP_NORMAL);
                if (!hasBottom) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BOTTOM_VERTS, state.light, birchSprite, BorderMeshGeometry.BOTTOM_UV, BorderMeshGeometry.BOTTOM_NORMAL);
                if (!hasLeft) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.WEST_VERTS, state.light, birchSprite, BorderMeshGeometry.WEST_UV, BorderMeshGeometry.WEST_NORMAL);
                if (!hasRight) BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.EAST_VERTS, state.light, birchSprite, BorderMeshGeometry.EAST_UV, BorderMeshGeometry.EAST_NORMAL);
            
                // back face rendering
                if (OptiFramesManager.isBackRendered()) {
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_INNER_VERTS, state.light, state.glow ? glowFrameSprite : frameSprite, BorderMeshGeometry.BACK_INNER_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_TOP_VERTS, state.light, birchSprite, BorderMeshGeometry.BACK_BORDER_TOP_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_EAST_VERTS, state.light, birchSprite, BorderMeshGeometry.BACK_BORDER_EAST_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_BOTTOM_VERTS, state.light, birchSprite, BorderMeshGeometry.BACK_BORDER_BOTTOM_UV, BorderMeshGeometry.BACK_NORMAL);
                    BorderMeshGeometry.drawQuad(vc, entry, BorderMeshGeometry.BACK_BORDER_WEST_VERTS, state.light, birchSprite, BorderMeshGeometry.BACK_BORDER_WEST_UV, BorderMeshGeometry.BACK_NORMAL);
                }
            });
        }

        // this is the rotation logic for the map
        // it's now here to avoid rotating the frame model
        if (!useDefault) {
            matrices.translate(64.0F, 64.0F, 0.0F);
            matrices.multiply(MAP_Z_CACHE[rotation]);
            matrices.translate(-64.0F, -64.0F, 0.0F);
        }

        // map rendering
        if (this.mapRenderer != null) {

            MapSlotData mapData = MapAtlasManager.getMapData(state.mapId);
            
            boolean drawDecorations = OptiFramesManager.isDecorationsRendered();

            if (mapData != null) {
                // draw map using atlas
                queue.submitCustom(matrices, mapData.renderLayer, (matrix, vertexConsumer) -> {
                    vertexConsumer.vertex(matrix, 0.0F, 128.0F, -0.01F).color(-1).texture(mapData.uvs[0], mapData.uvs[3]).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 128.0F, -0.01F).color(-1).texture(mapData.uvs[2], mapData.uvs[3]).light(light);
                    vertexConsumer.vertex(matrix, 128.0F, 0.0F, -0.01F).color(-1).texture(mapData.uvs[2], mapData.uvs[1]).light(light);
                    vertexConsumer.vertex(matrix, 0.0F, 0.0F, -0.01F).color(-1).texture(mapData.uvs[0], mapData.uvs[1]).light(light);
                });

                if (drawDecorations) {
                    VanillaRenderHelper.drawDecorations(state.mapRenderState, matrices, queue, light);
                }

            } else {
                // fallback if atlas not working
                this.mapRenderer.draw(state.mapRenderState, matrices, queue, drawDecorations, light);
            }
        }

        matrices.pop();
    }
}