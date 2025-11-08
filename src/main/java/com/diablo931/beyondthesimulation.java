package com.diablo931;

import com.diablo931.block.ModBlock;
import com.diablo931.block.ModBlockEntities;
import com.diablo931.block.ModScreenHandlers;
import com.diablo931.network.C2SStringPayload;
import com.diablo931.network.C2SUpdateUrlPayload;
import com.diablo931.network.ServerStringReceiver;
import com.diablo931.network.ServerUpdateUrlReceiver;
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

        LOGGER.info("BeyondTheSimulation now connecting to Higher Dimensions");
    }
}
