package com.diablo931.block;

import com.diablo931.item.ModItems;
import com.diablo931.util.TickableBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

public class PerfusionSystemBlockEntity extends BlockEntity implements TickableBlockEntity {

    private ItemStack storedItem = ItemStack.EMPTY;
    private int growthTicks = 0;
    private static final int GROWTH_TIME = 2000; // 10 seconds
    public static final int MAX_GROWTH_TICKS = GROWTH_TIME; // example value

    public int getGrowthTicks() {
        return this.growthTicks;
    }

    @Override
    @Nullable
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        // Send update packets when you call world.updateListeners(...)
        return BlockEntityUpdateS2CPacket.create(this);
    }


    public PerfusionSystemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PERFUSION_SYSTEM_ENTITY, pos, state);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        // this is what gets sent to the client when the chunk/block loads
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registries);
        return nbt;
    }

    public void sync() {
        if (world instanceof ServerWorld serverWorld) {
            BlockEntityUpdateS2CPacket packet = BlockEntityUpdateS2CPacket.create(this);
            serverWorld.getPlayers(player -> player.squaredDistanceTo(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            ) < 64 * 64).forEach(player -> player.networkHandler.sendPacket(packet));
        }
    }

    public interface TickableBlockEntity {
        void tick();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;
        System.out.println("[DEBUG] PerfusionSystem tick without item" + pos);
        if (!storedItem.isEmpty() && storedItem.isOf(ModItems.TREATED_FLESH)) {
            growthTicks++;
            if (growthTicks % 10 == 0) { // every 10 ticks (~0.5s)
                markDirty();
                sync();
            }
            if (growthTicks >= GROWTH_TIME) {
                storedItem = new ItemStack(ModItems.ORGANOID_BRAIN);
                growthTicks = 0;
                markDirty();
                sync();
            }

        }
    }

    public boolean hasItem() {
        return !storedItem.isEmpty();
    }

    public void addItem(ItemStack item) {
        this.storedItem = item;
        this.growthTicks = 0;
        markDirty();
        sync();
    }


    public ItemStack removeItem() {
        ItemStack out = storedItem;
        storedItem = ItemStack.EMPTY;
        growthTicks = 0;
        markDirty();
        if (world != null && !world.isClient) {
            sync();
        }
        return out;
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        if (!storedItem.isEmpty()) {
            // Save item safely even if empty, using registry-aware codec
            NbtElement itemNbt = storedItem.toNbtAllowEmpty(registries);
            nbt.put("Item", itemNbt);
        }

        nbt.putInt("Growth", growthTicks);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        if (nbt.contains("Item")) {
            storedItem = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Item"));
        } else {
            storedItem = ItemStack.EMPTY;
        }

        growthTicks = nbt.getInt("Growth");
    }


    public ItemStack getDisplayedItem() {
        return storedItem;
    }
}
