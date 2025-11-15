package com.diablo931.item.Camera;

import net.minecraft.component.ComponentType;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Simple Component registration for storing a webhook URL on an ItemStack.
 */
public final class CameraComponents {
    // register in common initializer (or static init)
    public static final ComponentType<String> WEBHOOK =
            Registry.register(
                    Registries.DATA_COMPONENT_TYPE,
                    Identifier.of("beyondthesimulation", "camera_webhook"),
                    ComponentType.<String>builder()
                            .codec(Codec.STRING)
                            .packetCodec(PacketCodecs.STRING)
                            .build()
            );

    private CameraComponents() {}
    public static void register() {
        // just loads static initializers
    }

}
