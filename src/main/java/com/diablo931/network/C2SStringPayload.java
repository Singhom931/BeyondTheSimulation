package com.diablo931.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record C2SStringPayload(String myString) implements CustomPayload {
    public static final CustomPayload.Id<C2SStringPayload> ID = new CustomPayload.Id<>(Identifier.of("my_mod", "c2s_string"));
    public static final PacketCodec<PacketByteBuf, C2SStringPayload> CODEC = CustomPayload.codecOf(C2SStringPayload::write, C2SStringPayload::new);

    public C2SStringPayload(PacketByteBuf buf) {
        this(buf.readString());
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(this.myString);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
