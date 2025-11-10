package com.diablo931.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import com.diablo931.beyondthesimulation;

public class ModBlockEntities {

    public static final BlockEntityType<MultiRedstoneArrayBlockEntity> MULTI_REDSTONE_ARRAY_ENTITY =
            register("multi_redstone_array", MultiRedstoneArrayBlockEntity::new, ModBlock.MULTI_REDSTONE_ARRAY);

    public static final BlockEntityType<PerfusionSystemBlockEntity> PERFUSION_SYSTEM_ENTITY =
            register("perfusion_system", PerfusionSystemBlockEntity::new, ModBlock.PERFUSION_SYSTEM);


    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.of(beyondthesimulation.MOD_ID, name);
        return Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                id,
                FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build()
        );
    }

    public static void initialize() {
        // ensures the class is loaded and static fields initialized
    }
}
