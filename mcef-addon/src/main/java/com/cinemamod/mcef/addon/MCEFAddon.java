package com.cinemamod.mcef.addon;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

public class MCEFAddon implements ModInitializer {
    public static final String MOD_ID = "mcef-addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block BROWSER_BLOCK;
    public static BlockEntityType<BrowserBlockEntity> BROWSER_BLOCK_ENTITY_TYPE;
    public static Item BROWSER_ITEM;

    public static Block COMPUTER_BLOCK;
    public static BlockEntityType<BrowserComputerBlockEntity> COMPUTER_BLOCK_ENTITY_TYPE;
    public static Item COMPUTER_ITEM;

    public static Item CLICKER_ITEM;

    public static final ResourceKey<CreativeModeTab> ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));

    @Override
    public void onInitialize() {
        LOGGER.info("MCEF Addon initializing...");

        // Browser Screen
        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "browser_screen");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, blockId);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, blockId);

        BROWSER_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new BrowserScreenBlock(BlockBehaviour.Properties.of().setId(blockKey).strength(1.0f).noOcclusion())
        );
        LOGGER.info("Registered Browser Block: {}", BROWSER_BLOCK != null);

        BROWSER_BLOCK_ENTITY_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                blockId,
                FabricBlockEntityTypeBuilder.create(BrowserBlockEntity::new, BROWSER_BLOCK).build(null)
        );

        BROWSER_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(BROWSER_BLOCK, new Item.Properties().setId(itemKey))
        );

        // Browser Computer
        ResourceLocation computerId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "browser_computer");
        ResourceKey<Block> computerKey = ResourceKey.create(Registries.BLOCK, computerId);
        ResourceKey<Item> computerItemKey = ResourceKey.create(Registries.ITEM, computerId);

        COMPUTER_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                computerKey,
                new BrowserComputerBlock(BlockBehaviour.Properties.of().setId(computerKey).strength(2.0f))
        );
        LOGGER.info("Registered Computer Block: {}", COMPUTER_BLOCK != null);

        COMPUTER_BLOCK_ENTITY_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                computerId,
                FabricBlockEntityTypeBuilder.create(BrowserComputerBlockEntity::new, COMPUTER_BLOCK).build(null)
        );

        COMPUTER_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                computerItemKey,
                new BlockItem(COMPUTER_BLOCK, new Item.Properties().setId(computerItemKey))
        );
        LOGGER.info("Registered Computer Item: {}", COMPUTER_ITEM != null);

        // Clicker
        ResourceLocation clickerId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "browser_clicker");
        ResourceKey<Item> clickerKey = ResourceKey.create(Registries.ITEM, clickerId);

        CLICKER_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                clickerKey,
                new BrowserClickerItem(new Item.Properties().setId(clickerKey).stacksTo(1))
        );
        LOGGER.info("Registered Clicker Item: {}", CLICKER_ITEM != null);

        // Creative Tab
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP, FabricItemGroup.builder()
                .icon(() -> new ItemStack(BROWSER_ITEM))
                .title(Component.translatable("itemGroup.mcef-addon.main"))
                .build());

        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> {
            content.accept(BROWSER_ITEM);
            content.accept(COMPUTER_ITEM);
            content.accept(CLICKER_ITEM);
        });

        LOGGER.info("MCEF Addon initialized successfully!");
    }
}
