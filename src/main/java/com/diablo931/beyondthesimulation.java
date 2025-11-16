package com.diablo931;

import com.diablo931.block.ModBlock;
import com.diablo931.block.ModBlockEntities;
import com.diablo931.block.ModScreenHandlers;
import com.diablo931.item.Camera.CameraComponents;
import com.diablo931.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.diablo931.item.ModItems;

public class beyondthesimulation implements ModInitializer {

    public static final String MOD_ID = "beyondthesimulation";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlock.initialize();
        ModBlockEntities.initialize();
        ModScreenHandlers.initialize();
        PayloadTypeRegistry.playC2S().register(C2SStringPayload.ID, C2SStringPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(C2SStringPayload.ID, new ServerStringReceiver());
        PayloadTypeRegistry.playC2S().register(C2SUpdateUrlPayload.ID, C2SUpdateUrlPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(C2SUpdateUrlPayload.ID, new ServerUpdateUrlReceiver());
        PayloadTypeRegistry.playC2S().register(RequestCameraWebhookPayload.ID, RequestCameraWebhookPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendCameraWebhookPayload.ID, SendCameraWebhookPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetCameraWebhookPayload.ID, SetCameraWebhookPayload.CODEC);
        CameraNetworking.registerServerReceivers();
        CameraComponents.register();

        LOGGER.info("BeyondTheSimulation now connecting to Higher Dimensions");
    }
}
