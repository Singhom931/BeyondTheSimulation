package com.diablo931.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestCameraWebhookPayload(boolean mainHand) implements CustomPayload {
    public static final CustomPayload.Id<RequestCameraWebhookPayload> ID =
            new CustomPayload.Id<>(Identifier.of("beyondthesimulation", "request_webhook"));

    public static final PacketCodec<PacketByteBuf, RequestCameraWebhookPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBoolean(payload.mainHand),
                    buf -> new RequestCameraWebhookPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

