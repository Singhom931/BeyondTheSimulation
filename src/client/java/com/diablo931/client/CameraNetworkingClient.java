package com.diablo931.client;

import com.diablo931.network.SendCameraWebhookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class CameraNetworkingClient {

    public static void registerClient() {

        ClientPlayNetworking.registerGlobalReceiver(
                SendCameraWebhookPayload.ID,
                (payload, context) -> {

                    MinecraftClient client = MinecraftClient.getInstance();
                    String webhook = payload.webhook();

                    client.execute(() -> {
                        System.out.println(webhook);
                        if (webhook != null && !webhook.isEmpty()) {
                            ScreenshotUtil.captureAndSend(webhook);
                        }
                    });
                }
        );
    }
}
