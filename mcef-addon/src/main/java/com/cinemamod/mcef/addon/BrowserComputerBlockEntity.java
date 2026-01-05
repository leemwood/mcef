package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BrowserComputerBlockEntity extends BlockEntity {
    private String url = "https://www.google.com";

    public BrowserComputerBlockEntity(BlockPos pos, BlockState state) {
        super(MCEFAddon.COMPUTER_BLOCK_ENTITY_TYPE, pos, state);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            
            // Notify nearby screens to update their URL
            // We can scan a small area for screens
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos p = worldPosition.offset(x, y, z);
                        BlockEntity be = level.getBlockEntity(p);
                        if (be instanceof BrowserBlockEntity bbe && bbe.isMaster()) {
                            // If this screen is connected to THIS computer, reload it
                            // Note: getUrl() in bbe will now return the new computer url
                            IMCEFBrowser browser = bbe.getBrowser();
                            if (browser != null) {
                                browser.loadURL(url);
                            }
                        }
                    }
                }
            }
        }
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
}
