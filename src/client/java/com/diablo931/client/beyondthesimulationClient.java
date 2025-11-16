package com.diablo931.client;

import com.diablo931.block.*;
import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.text.Text;

public class beyondthesimulationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlock.MULTI_REDSTONE_ARRAY, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlock.PERFUSION_SYSTEM, RenderLayer.getTranslucent());

        // Explicit generics added here
        HandledScreens.register(
                ModScreenHandlers.MULTI_REDSTONE_ARRAY_HANDLER,
                new HandledScreens.Provider<MultiRedstoneArrayScreenHandler, MultiRedstoneArrayScreen>() {
                    @Override
                    public MultiRedstoneArrayScreen create(MultiRedstoneArrayScreenHandler handler, net.minecraft.entity.player.PlayerInventory inv, net.minecraft.text.Text title) {
                        return new MultiRedstoneArrayScreen(handler, inv, title);
                    }
                }
        );

        ClientInteractionHandler.register();
        BlockEntityRendererFactories.register(ModBlockEntities.PERFUSION_SYSTEM_ENTITY, PerfusionSystemRenderer::new);


//        PayloadTypeRegistry.playC2S().register(SetCameraWebhookPayload.ID, SetCameraWebhookPayload.CODEC);
        CameraNetworkingClient.registerClient();
        CameraClientEvents.register();


    }
}
