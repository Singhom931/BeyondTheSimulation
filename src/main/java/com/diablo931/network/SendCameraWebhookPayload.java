package com.diablo931.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SendCameraWebhookPayload(String webhook) implements CustomPayload {

    public static final CustomPayload.Id<SendCameraWebhookPayload> ID =
            new CustomPayload.Id<>(Identifier.of("beyondthesimulation", "send_webhook"));

    public static final PacketCodec<PacketByteBuf, SendCameraWebhookPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeString(payload.webhook()),
                    buf -> new SendCameraWebhookPayload(buf.readString())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
