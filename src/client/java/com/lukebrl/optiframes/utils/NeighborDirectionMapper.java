package com.lukebrl.optiframes.utils;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;

public final class NeighborDirectionMapper {

    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;

    // pre-computed direction lookup: [facing][border] = offsetDirection
    private static final Direction[][] DIRECTION_LOOKUP = new Direction[6][4];

    static {
        for (Direction facing : Direction.values()) {
            Direction rightDir, upDir;
            switch (facing) {
                case NORTH -> { rightDir = Direction.EAST; upDir = Direction.UP; }
                case SOUTH -> { rightDir = Direction.WEST; upDir = Direction.UP; }
                case WEST  -> { rightDir = Direction.NORTH; upDir = Direction.UP; }
                case EAST  -> { rightDir = Direction.SOUTH; upDir = Direction.UP; }
                case UP    -> { rightDir = Direction.WEST; upDir = Direction.NORTH; }
                case DOWN  -> { rightDir = Direction.WEST; upDir = Direction.SOUTH; }
                default    -> { rightDir = Direction.EAST; upDir = Direction.UP; }
            }

            int facingIdx = facing.ordinal();
            DIRECTION_LOOKUP[facingIdx][TOP] = upDir;
            DIRECTION_LOOKUP[facingIdx][BOTTOM] = upDir.getOpposite();
            DIRECTION_LOOKUP[facingIdx][WEST] = rightDir.getOpposite();
            DIRECTION_LOOKUP[facingIdx][EAST] = rightDir;
        }
    }

    private NeighborDirectionMapper() {}

    public static long getNeighborPos(int x, int y, int z, Direction facing, int border) {
        Direction offsetDir = DIRECTION_LOOKUP[facing.ordinal()][border];
        return BlockPos.asLong(
            x + offsetDir.getOffsetX(),
            y + offsetDir.getOffsetY(),
            z + offsetDir.getOffsetZ()
        );
    }
}