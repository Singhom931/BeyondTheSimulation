package com.diablo931.client;

import com.diablo931.block.ModBlock;
import com.diablo931.block.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

public class beyondthesimulationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlock.MULTI_REDSTONE_ARRAY, RenderLayer.getTranslucent());
        HandledScreens.register(
                ModScreenHandlers.MULTI_REDSTONE_ARRAY_HANDLER,
                MultiRedstoneArrayScreen::new
        );
        ClientInteractionHandler.register();
    }
}
