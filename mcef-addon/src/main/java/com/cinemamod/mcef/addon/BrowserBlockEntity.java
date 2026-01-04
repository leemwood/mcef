package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import com.cinemamod.mcef.MCEF;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BrowserBlockEntity extends BlockEntity {
    private String url = "https://www.google.com";
    private IMCEFBrowser browser;
    private boolean isMaster = false;

    public BrowserBlockEntity(BlockPos pos, BlockState state) {
        super(MCEFAddon.BROWSER_BLOCK_ENTITY_TYPE, pos, state);
    }

    public void setUrl(String url) {
        this.url = url;
        if (browser != null) {
            browser.loadURL(url);
        }
        setChanged();
    }

    public String getUrl() {
        return url;
    }

    public IMCEFBrowser getBrowser() {
        if (browser == null && level != null && level.isClientSide && MCEF.isInitialized()) {
            browser = MCEF.createBrowser(url, true);
            if (browser != null) {
                browser.resize(512, 384);
            }
        }
        return browser;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains("url")) {
            this.url = nbt.getString("url");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putString("url", url);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        close();
    }

    public void close() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }
}
