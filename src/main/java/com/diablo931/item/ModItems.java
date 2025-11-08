package com.diablo931.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
//import net.minecraft.item.CreativeModeTab;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.function.Function;
import com.diablo931.beyondthesimulation; // your MOD_ID holder

public class ModItems {

    public static void initialize() {
    }

    public static final Item ORGANOID_BRAIN = register("organoid_brain", Item::new, new Item.Settings());
    public static final Item TREATED_FLESH = register("treated_flesh", Item::new, new Item.Settings());
    public static final Item NUTRIENT_FLUID = register("nutrient_fluid", Item::new, new Item.Settings());

    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        // Create the item key.
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(beyondthesimulation.MOD_ID, name));

        // Create the item instance.
        Item item = itemFactory.apply(settings.registryKey(itemKey));

        // Register the item.
        Registry.register(Registries.ITEM, itemKey, item);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries->{
            entries.add(item);
        });

        return item;
    }

}