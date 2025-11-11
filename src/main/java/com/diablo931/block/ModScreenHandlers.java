package com.diablo931.block;

import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;


import com.diablo931.beyondthesimulation;

public class ModScreenHandlers {

    public static ScreenHandlerType<MultiRedstoneArrayScreenHandler> MULTI_REDSTONE_ARRAY_HANDLER;

    public static void initialize() {
        MULTI_REDSTONE_ARRAY_HANDLER =
                Registry.register(
                        Registries.SCREEN_HANDLER,
                        Identifier.of(beyondthesimulation.MOD_ID, "multi_redstone_array"),
                        new ScreenHandlerType<MultiRedstoneArrayScreenHandler>(
                                (int syncId, PlayerInventory inv) -> new MultiRedstoneArrayScreenHandler(syncId, (PlayerInventory) inv),
                                FeatureSet.empty()
                        )
                );
    }
//
//    private static <T extends ScreenHandler> ScreenHandlerType<T> register(
//            String name,
//            ScreenHandlerType.Factory<T> factory
//    ) {
//        Identifier id = Identifier.of(beyondthesimulation.MOD_ID, name);
//        return Registry.register(
//                Registries.SCREEN_HANDLER,
//                id,
//                new ScreenHandlerType<>(factory, FeatureSet.empty())
//        );
//    }
}
