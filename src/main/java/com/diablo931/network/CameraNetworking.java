package com.diablo931.network;

import com.diablo931.item.Camera.CameraComponents;
import com.diablo931.item.Camera.CameraItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class CameraNetworking {

    public static void registerServerReceivers() {

        // Server receives a request for the webhook stored in the camera item
        ServerPlayNetworking.registerGlobalReceiver(
                RequestCameraWebhookPayload.ID,
                (payload, context) -> {

                    ServerPlayerEntity player = context.player();

                    boolean mainHand = payload.mainHand();
                    ItemStack stack = mainHand
                            ? player.getMainHandStack()
                            : player.getOffHandStack();

                    String webhook = CameraItem.getWebhookDecrypted(stack);

                    // Must run on server thread
                    context.server().execute(() -> {
                        // Send back the webhook to the client
                        ServerPlayNetworking.send(
                                player,
                                new SendCameraWebhookPayload(webhook)
                        );
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SetCameraWebhookPayload.ID,
                (payload, context) -> {

                    ServerPlayerEntity player = context.player();
                    String newWebhook = payload.webhook();

                    context.server().execute(() -> {
                        ItemStack stack = player.getMainHandStack();

                        if (stack.getItem() instanceof CameraItem) {
                            CameraItem.setWebhook(stack, newWebhook);
                            System.out.println("[Camera] Saved webhook: " + newWebhook);
                        }
                    });
                }
        );
    }
}
