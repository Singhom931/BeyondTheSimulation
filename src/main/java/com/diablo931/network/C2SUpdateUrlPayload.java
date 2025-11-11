package com.diablo931.network;

import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayBlockEntity.Mode;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record C2SUpdateUrlPayload(BlockPos pos, String url, Mode mode) implements CustomPayload {

    public static final CustomPayload.Id<C2SUpdateUrlPayload> ID =
            new CustomPayload.Id<>(Identifier.of("beyondthesimulation", "update_url"));

    public static final PacketCodec<PacketByteBuf, C2SUpdateUrlPayload> CODEC =
            CustomPayload.codecOf(C2SUpdateUrlPayload::write, C2SUpdateUrlPayload::new);

    // Deserialize from buffer
    public C2SUpdateUrlPayload(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readString(), Mode.valueOf(buf.readString()));
    }

    // Serialize to buffer
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeString(this.url);
        buf.writeString(this.mode.name());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
