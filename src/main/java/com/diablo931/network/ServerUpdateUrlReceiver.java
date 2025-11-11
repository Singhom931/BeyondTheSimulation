package com.diablo931.network;

import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class ServerUpdateUrlReceiver implements ServerPlayNetworking.PlayPayloadHandler<C2SUpdateUrlPayload> {
    @Override
    public void receive(C2SUpdateUrlPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            BlockPos pos = payload.pos();

            if (context.player() != null && context.player().getEntityWorld() != null) {
                BlockEntity blockEntity = context.player().getEntityWorld().getBlockEntity(pos);

                // <-- Add mode update here
                if (blockEntity instanceof MultiRedstoneArrayBlockEntity mbe) {
                    mbe.setUrl(payload.url());
                    mbe.setMode(payload.mode()); // <-- this ensures mode is synced
                    mbe.markDirty();
                }
            }
        });
    }
}
