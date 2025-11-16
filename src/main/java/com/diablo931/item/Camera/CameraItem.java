package com.diablo931.item.Camera;

import com.diablo931.item.Camera.CameraComponents;
import com.diablo931.network.SendCameraWebhookPayload;
import com.diablo931.util.CryptoUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Camera item - stores webhook in NBT and, on use (server-side), sends S2C to the player
 * telling them the webhook. Client then screenshots and sends the image.
 *
 * NOTE: This class intentionally does not import client-only classes.
 */
public class CameraItem extends Item {
    private static final String WEBHOOK_KEY = "CameraWebhook";

    public CameraItem(Settings settings) {
        super(settings);
    }

    /** Called when player uses the item (right-click). We handle on server side to avoid client/server import issues. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return super.useOnBlock(context);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPYGLASS;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity player) {
        return 72000; // same as bows, spyglass, shields
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        player.playSound(SoundEvents.ITEM_SPYGLASS_USE, 1.0F, 1.0F);
        player.setCurrentHand(hand);

        // Run server-side logic only
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack stack = player.getStackInHand(hand);
            System.out.println("[DEBUG] Components on item: " + stack.getComponents());
            String webhook = getWebhookDecrypted(stack);
            // send S2C payload telling the client to capture & send screenshot to webhook
            SendCameraWebhookPayload send = new SendCameraWebhookPayload(webhook);
            // use context.responseSender on server side if you had the context; here we directly send to player via networking util
            // ServerPlayNetworking has send methods historically but mappings vary — to be safe we use player's network handler sendPacket fallback:
            ServerPlayNetworking.send(serverPlayer, send);
        }
        // always succeed on client too (this method will run on client as well, but server already handled sending)
        return ItemUsage.consumeHeldItem(world, player, hand);
        //return ActionResult.SUCCESS;
    }

    // --- convenience NBT helpers (store webhook inside stack NBT) ---

    public static void setWebhook(ItemStack stack, String webhook) {
        String encrypted = CryptoUtil.encrypt(webhook);
        stack.set(CameraComponents.WEBHOOK, encrypted);
        System.out.println("[DEBUG] webhook_set: " + webhook);
    }

    public static String getWebhookDecrypted(ItemStack stack) {
        return CryptoUtil.decrypt(stack.getComponents().getOrDefault(CameraComponents.WEBHOOK, ""));
    }
}
