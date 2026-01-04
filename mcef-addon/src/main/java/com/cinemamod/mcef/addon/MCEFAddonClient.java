package com.cinemamod.mcef.addon;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class MCEFAddonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(MCEFAddon.BROWSER_BLOCK_ENTITY_TYPE, BrowserBlockEntityRenderer::new);
    }
}
