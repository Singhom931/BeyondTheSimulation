package com.diablo931.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class ClientInteractionHandler {

    public static void register() {
        // Run every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Check if the player is targeting a block
            if (client.crosshairTarget instanceof BlockHitResult hit) {

                // Check if the use key is pressed (right-click by default)
                if (MinecraftClient.getInstance().options.useKey.isPressed()) {
                    BlockPos pos = hit.getBlockPos();

                    // Update tracker
                    LastClickedBlockTracker.setLastClickedPos(pos);
                }
            }
        });
    }
}
