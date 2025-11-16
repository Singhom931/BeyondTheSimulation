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

        ClientPlayNetworking.registerGlobalReceiver(
                CctvCapturePayload.ID,
                (payload, ctx) -> {
                    var pos = payload.pos();
                    var uuid = payload.uuid();

                    ctx.client().execute(() -> {
                        var player = ctx.client().player;
                        if (player != null) {
                            player.sendMessage(
                                    Text.literal("CCTV captured player " + uuid + " at " + pos),
                                    false
                            );
                        }
                    });
                }
        );

        PayloadTypeRegistry.playS2C().register(EnterCctvPayload.ID, EnterCctvPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(EnterCctvPayload.ID, (payload, ctx) -> {
            System.out.println("CLIENT RECEIVED ENTER_CCTV PAYLOAD!");

            ctx.client().execute(() -> {
                System.out.println("Opening CCTV screen NOW");

                // Open CameraViewScreen with the received data
                com.diablo931.client.CctvViewScreen.openCamera(ctx.client(), payload.pos(), payload.yaw(), payload.pitch(), payload.fov(), payload.range(), payload.name(), payload.webhookUrl());
                System.out.println("After setScreen call");
            });
        });

        ClientInteractionHandler.register();
        BlockEntityRendererFactories.register(ModBlockEntities.PERFUSION_SYSTEM_ENTITY, PerfusionSystemRenderer::new);


//        PayloadTypeRegistry.playC2S().register(SetCameraWebhookPayload.ID, SetCameraWebhookPayload.CODEC);
        CameraNetworkingClient.registerClient();
        CameraClientEvents.register();


    }
}
