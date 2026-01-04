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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

public class MCEFAddon implements ModInitializer {
    public static final String MOD_ID = "mcef-addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block BROWSER_BLOCK;
    public static BlockEntityType<BrowserBlockEntity> BROWSER_BLOCK_ENTITY_TYPE;
    public static Item BROWSER_ITEM;

    public static final ResourceKey<CreativeModeTab> ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));

    @Override
    public void onInitialize() {
        LOGGER.info("MCEF Addon initializing...");

        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "browser_screen");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, blockId);

        BROWSER_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new BrowserScreenBlock(BlockBehaviour.Properties.of().setId(blockKey).strength(1.0f).noOcclusion())
        );

        BROWSER_BLOCK_ENTITY_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                blockId,
                FabricBlockEntityTypeBuilder.create(BrowserBlockEntity::new, BROWSER_BLOCK).build(null)
        );

        BROWSER_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                ResourceKey.create(Registries.ITEM, blockId),
                new BlockItem(BROWSER_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, blockId)))
        );

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP, FabricItemGroup.builder()
                .icon(() -> new ItemStack(BROWSER_ITEM))
                .title(Component.translatable("itemGroup.mcef-addon.main"))
                .displayItems((parameters, output) -> {
                    output.accept(BROWSER_ITEM);
                }).build());
    }
}
