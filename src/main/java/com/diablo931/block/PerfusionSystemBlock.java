package com.diablo931.block;

import com.diablo931.item.ModItems;
import com.diablo931.util.TickableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.block.AbstractBlock.createCodec;


public class PerfusionSystemBlock extends BlockWithEntity {

    public static final MapCodec<PerfusionSystemBlock> CODEC = createCodec(PerfusionSystemBlock::new);

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    public PerfusionSystemBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PerfusionSystemBlockEntity(pos, state);
    }

//    @Override
//    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
//        super.onBlockAdded(state, world, pos, oldState, notify);
//        if (!world.isClient) world.scheduleBlockTick(pos, this, 1);
//    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return TickableBlockEntity.getTicker(world);
    }


    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        System.out.println("[DEBUG] PerfusionSystemBlock onUse called!");

        if (world.isClient) return ActionResult.SUCCESS;

        PerfusionSystemBlockEntity be = (PerfusionSystemBlockEntity) world.getBlockEntity(pos);
        if (be == null) return ActionResult.PASS;

        // Player is empty-handed → try to *remove* grown item
        if (be.hasItem()) {
            ItemStack result = be.removeItem();
            if (!player.giveItemStack(result)) player.dropItem(result, false);
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1f);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        System.out.println("[DEBUG] PerfusionSystemBlock onUseWithItem called!");

        if (world.isClient) return ActionResult.SUCCESS;

        PerfusionSystemBlockEntity be = (PerfusionSystemBlockEntity) world.getBlockEntity(pos);
        if (be == null) return ActionResult.PASS;

        // Player is holding treated flesh → insert
        if (!be.hasItem() && stack.isOf(ModItems.TREATED_FLESH)) {
            be.addItem(stack.split(1)); // consume one
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.5f, 1f);
            return ActionResult.SUCCESS;
        }

        // If not handled, fall back to default block use (like placing blocks)
        return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

}
