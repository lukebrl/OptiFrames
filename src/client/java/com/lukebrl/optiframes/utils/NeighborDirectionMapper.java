package com.lukebrl.optiframes.utils;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;

public final class NeighborDirectionMapper {

    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    // pre-computed direction lookup: [facing][rotation][border] = offsetDirection
    private static final Direction[][][] DIRECTION_LOOKUP = new Direction[6][4][4];

    static {
        for (Direction facing : Direction.values()) {
            Direction rightDir, upDir;
            switch (facing) {
                case NORTH -> { rightDir = Direction.WEST; upDir = Direction.UP; }
                case SOUTH -> { rightDir = Direction.EAST; upDir = Direction.UP; }
                case WEST  -> { rightDir = Direction.SOUTH; upDir = Direction.UP; }
                case EAST  -> { rightDir = Direction.NORTH; upDir = Direction.UP; }
                case UP    -> { rightDir = Direction.EAST; upDir = Direction.NORTH; }
                case DOWN  -> { rightDir = Direction.EAST; upDir = Direction.SOUTH; }
                default   -> { rightDir = Direction.EAST; upDir = Direction.UP; }
            }

            int facingIdx = facing.ordinal();
            for (int rot = 0; rot < 4; rot++) {
                Direction currentRightDir = rightDir;
                Direction currentUpDir = upDir;
                
                // apply rotation steps
                for (int i = 0; i < rot; i++) {
                    Direction temp = currentUpDir;
                    currentUpDir = currentRightDir;
                    currentRightDir = temp.getOpposite();
                }

                // cache all 4 border directions (facing, rotation)
                DIRECTION_LOOKUP[facingIdx][rot][TOP] = currentUpDir;
                DIRECTION_LOOKUP[facingIdx][rot][BOTTOM] = currentUpDir.getOpposite();
                DIRECTION_LOOKUP[facingIdx][rot][LEFT] = currentRightDir.getOpposite();
                DIRECTION_LOOKUP[facingIdx][rot][RIGHT] = currentRightDir;
            }
        }
    }

    private NeighborDirectionMapper() {}

    public static long getNeighborPos(int x, int y, int z, Direction facing, int mapRotation, int border) {
        Direction offsetDir = DIRECTION_LOOKUP[facing.ordinal()][mapRotation % 4][border];
        
        if (offsetDir == null) return BlockPos.asLong(x, y, z);

        return BlockPos.asLong(
            x + offsetDir.getOffsetX(),
            y + offsetDir.getOffsetY(),
            z + offsetDir.getOffsetZ()
        );
    }
}