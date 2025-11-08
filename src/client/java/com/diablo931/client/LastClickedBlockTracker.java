package com.diablo931.client;

import net.minecraft.util.math.BlockPos;

public class LastClickedBlockTracker {
    private static BlockPos lastClickedPos = null;

    public static void setLastClickedPos(BlockPos pos) {
        lastClickedPos = pos;
    }

    public static BlockPos getLastClickedPos() {
        return lastClickedPos;
    }
}
