package com.diablo931.block.MultiRedstoneArray;


import com.diablo931.util.TickableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

public class MultiRedstoneArrayBlock extends BlockWithEntity {

    public static final IntProperty STAGE = IntProperty.of("stage", 0, 1);


    public MultiRedstoneArrayBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(MultiRedstoneArrayBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MultiRedstoneArrayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return TickableBlockEntity.getTicker(world);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockState blockState = super.onBreak(world, pos, state, player);

        return  blockState;
    }


    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) { // only open GUI on server
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MultiRedstoneArrayBlockEntity mraBE) {
                player.openHandledScreen(mraBE); // <-- passes block entity to ScreenHandler
            }
        }
        return ActionResult.SUCCESS;
    }

    // Custom shape — half-height block
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 6, 16);

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }


    @Override
    public boolean emitsRedstonePower(BlockState state) {
        // Must return true to be considered a redstone source
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction side) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiRedstoneArrayBlockEntity mraBE) {
            return mraBE.getPowerForDirection(side);
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
//        return 0;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);

        if (world.isClient) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiRedstoneArrayBlockEntity mraBE) {
            mraBE.updateSignalsFromWorld();
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return false; // Change to true if you ever want comparator logic
    }

}
