package com.diablo931.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class MultiRedstoneArrayScreenHandler extends ScreenHandler {
    private final @Nullable MultiRedstoneArrayBlockEntity blockEntity;
//    private final String url;

//    public MultiRedstoneArrayScreenHandler(int syncId, Inventory inv, PacketByteBuf buf) {
//        this(syncId, (PlayerInventory) inv, (MultiRedstoneArrayBlockEntity) ((PlayerInventory) inv).player.getWorld().getBlockEntity(buf.readBlockPos()));
//    }

    public MultiRedstoneArrayScreenHandler(int syncId, PlayerInventory playerInventory, MultiRedstoneArrayBlockEntity entity) {
        super(ModScreenHandlers.MULTI_REDSTONE_ARRAY_HANDLER, syncId);
        this.blockEntity = entity;
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(PlayerInventory inv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public MultiRedstoneArrayBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        // No inventory slots yet, just return empty
        return ItemStack.EMPTY;
    }

    // Called on the client when it receives sync data (blockEntity not available)
    public MultiRedstoneArrayScreenHandler(int syncId, PlayerInventory inv) {
        this(syncId, inv, null);
    }

}
