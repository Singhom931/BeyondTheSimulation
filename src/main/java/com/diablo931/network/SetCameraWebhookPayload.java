package com.diablo931.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetCameraWebhookPayload(String webhook) implements CustomPayload {

    public static final CustomPayload.Id<SetCameraWebhookPayload> ID =
            new CustomPayload.Id<>(Identifier.of("beyondthesimulation", "set_webhook"));

    public static final PacketCodec<PacketByteBuf, SetCameraWebhookPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeString(payload.webhook()),
                    buf -> new SetCameraWebhookPayload(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
