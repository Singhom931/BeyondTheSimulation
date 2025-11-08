package com.diablo931.network;

import com.diablo931.beyondthesimulation;
import com.diablo931.block.MultiRedstoneArrayBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class ServerUpdateUrlReceiver implements ServerPlayNetworking.PlayPayloadHandler<C2SUpdateUrlPayload> {
    @SuppressWarnings("resource")
    @Override
    public void receive(C2SUpdateUrlPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            BlockPos pos = payload.pos();
            String url = payload.url();

            if (context.player() != null && context.player().getEntityWorld() != null) {
                BlockEntity blockEntity = context.player().getEntityWorld().getBlockEntity(pos);
                if (blockEntity instanceof MultiRedstoneArrayBlockEntity mbe) {
                    mbe.setUrl(url);
                    // Mark the block entity as dirty to ensure the change is saved
                    mbe.markDirty();
                }
            }
        });
    }
}
