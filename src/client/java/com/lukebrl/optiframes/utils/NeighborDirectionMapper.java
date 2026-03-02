package com.lukebrl.optiframes.utils;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;

public final class NeighborDirectionMapper {

    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private NeighborDirectionMapper() {}

    public static long getNeighborPos(int x, int y, int z, Direction facing, int mapRotation, int border) {
        Direction rightDir;
        Direction upDir;

        // set appropriate direction based on facing
        switch (facing) {
            case NORTH -> { rightDir = Direction.WEST; upDir = Direction.UP; }
            case SOUTH -> { rightDir = Direction.EAST; upDir = Direction.UP; }
            case WEST  -> { rightDir = Direction.SOUTH; upDir = Direction.UP; }
            case EAST  -> { rightDir = Direction.NORTH; upDir = Direction.UP; }
            case UP    -> { rightDir = Direction.EAST; upDir = Direction.NORTH; }
            case DOWN  -> { rightDir = Direction.EAST; upDir = Direction.SOUTH; }
            default   -> { rightDir = Direction.EAST; upDir = Direction.UP; }
        }

        // apply map rotation to local axes
        int rotationSteps = mapRotation % 4;
        for (int i = 0; i < rotationSteps; i++) {
            Direction temp = upDir;
            upDir = rightDir;
            rightDir = temp.getOpposite();
        }

        Direction offsetDir = switch (border) {
            case TOP -> upDir;
            case BOTTOM -> upDir.getOpposite();
            case LEFT -> rightDir.getOpposite();
            case RIGHT -> rightDir;
            default -> null;
        };

        if (offsetDir == null) return BlockPos.asLong(x, y, z);

        return BlockPos.asLong(
            x + offsetDir.getOffsetX(),
            y + offsetDir.getOffsetY(),
            z + offsetDir.getOffsetZ()
        );
    }
}