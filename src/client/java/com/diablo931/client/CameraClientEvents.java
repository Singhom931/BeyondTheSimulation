package com.diablo931.client;

import com.diablo931.item.Camera.CameraItem;
import com.diablo931.item.Camera.CameraComponents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class CameraClientEvents {

    private static boolean wasAttackPressed = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean attack = client.options.attackKey.isPressed();

            // detect rising edge (when key goes from released -> pressed)
            if (attack && !wasAttackPressed) {
                ItemStack stack = client.player.getMainHandStack();

                if (stack.getItem() instanceof CameraItem) {
                    openWebhookInput(client, stack);
                }
            }

            wasAttackPressed = attack;
        });
    }

    private static void openWebhookInput(MinecraftClient client, ItemStack stack) {
        client.setScreen(new CameraSettingsScreen(value -> {
            if (value != null && !value.isEmpty()) {
                stack.set(CameraComponents.WEBHOOK, value);
            }
        }));
    }
}
