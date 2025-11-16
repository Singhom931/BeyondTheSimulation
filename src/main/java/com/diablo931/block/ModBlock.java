package com.diablo931.block;

import com.diablo931.beyondthesimulation;
import com.diablo931.block.PerfusionSystem.PerfusionSystemBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayBlock;

import java.util.function.Function;

public class ModBlock {

    public static void initialize() {
    }

    public static final Block MULTI_REDSTONE_ARRAY =  register("multi_redstone_array", MultiRedstoneArrayBlock::new, AbstractBlock.Settings.create().strength(1.5f,4f).sounds(BlockSoundGroup.WART_BLOCK).nonOpaque(), true);
    public static final Block PERFUSION_SYSTEM =  register("perfusion_system", PerfusionSystemBlock::new, AbstractBlock.Settings.create().strength(1.5f,4f).sounds(BlockSoundGroup.HONEY), true);
    //public static final Block CCTV =  register("cctv", CctvBlock::new, AbstractBlock.Settings.create().strength(1.5f,4f).sounds(BlockSoundGroup.CHAIN), true);

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = keyOfBlock(name);
        // Create the block instance
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        // Sometimes, you may not want to register an item for the block.
        // Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
        if (shouldRegisterItem) {
            // Items need to be registered with a different type of registry key, but the ID
            // can be the same.
            RegistryKey<Item> itemKey = keyOfItem(name);

            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
            Registry.register(Registries.ITEM, itemKey, blockItem);

            ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register((entries) -> {
                entries.add(blockItem);
            });
        }
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(beyondthesimulation.MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(beyondthesimulation.MOD_ID, name));
    }

}
