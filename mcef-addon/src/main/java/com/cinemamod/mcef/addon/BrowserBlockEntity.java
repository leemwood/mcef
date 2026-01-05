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
    private BlockPos masterPos;
    private BlockPos computerPos;
    private int gridX = 0;
    private int gridY = 0;
    private int gridWidth = 1;
    private int gridHeight = 1;

    public BrowserBlockEntity(BlockPos pos, BlockState state) {
        super(MCEFAddon.BROWSER_BLOCK_ENTITY_TYPE, pos, state);
        this.masterPos = pos;
    }

    public void setMultiblock(BlockPos masterPos, int x, int y, int w, int h, @Nullable BlockPos computerPos) {
        this.masterPos = masterPos;
        this.gridX = x;
        this.gridY = y;
        this.gridWidth = w;
        this.gridHeight = h;
        this.computerPos = computerPos;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isMaster() {
        return worldPosition.equals(masterPos);
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }

    public void setUrl(String url) {
        if (!isMaster() && level != null) {
            BlockEntity master = level.getBlockEntity(masterPos);
            if (master instanceof BrowserBlockEntity masterBe) {
                masterBe.setUrl(url);
                return;
            }
        }
        
        if (computerPos != null && level != null) {
            BlockEntity computer = level.getBlockEntity(computerPos);
            if (computer instanceof BrowserComputerBlockEntity computerBe) {
                computerBe.setUrl(url);
                // The computer will update and we'll see it in getUrl()
                return;
            }
        }

        this.url = url;
        if (browser != null) {
            browser.loadURL(url);
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getUrl() {
        if (!isMaster() && level != null) {
            BlockEntity master = level.getBlockEntity(masterPos);
            if (master instanceof BrowserBlockEntity masterBe) {
                return masterBe.getUrl();
            }
        }
        
        if (computerPos != null && level != null) {
            BlockEntity computer = level.getBlockEntity(computerPos);
            if (computer instanceof BrowserComputerBlockEntity computerBe) {
                return computerBe.getUrl();
            }
        }
        
        return url;
    }

    public IMCEFBrowser getBrowser() {
        if (!isMaster() && level != null) {
            BlockEntity master = level.getBlockEntity(masterPos);
            if (master instanceof BrowserBlockEntity masterBe) {
                return masterBe.getBrowser();
            }
            return null;
        }

        if (browser == null && level != null && level.isClientSide && MCEF.isInitialized()) {
            browser = MCEF.createBrowser(url, true);
            if (browser != null) {
                // Resize based on multiblock size
                browser.resize(gridWidth * 512, gridHeight * 384);
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
        if (nbt.contains("masterX")) {
            this.masterPos = new BlockPos(nbt.getInt("masterX"), nbt.getInt("masterY"), nbt.getInt("masterZ"));
        } else {
            this.masterPos = worldPosition;
        }
        this.gridX = nbt.getInt("gridX");
        this.gridY = nbt.getInt("gridY");
        this.gridWidth = Math.max(1, nbt.getInt("gridWidth"));
        this.gridHeight = Math.max(1, nbt.getInt("gridHeight"));
        if (nbt.contains("computerX")) {
            this.computerPos = new BlockPos(nbt.getInt("computerX"), nbt.getInt("computerY"), nbt.getInt("computerZ"));
        } else {
            this.computerPos = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putString("url", url);
        if (masterPos != null) {
            nbt.putInt("masterX", masterPos.getX());
            nbt.putInt("masterY", masterPos.getY());
            nbt.putInt("masterZ", masterPos.getZ());
        }
        if (computerPos != null) {
            nbt.putInt("computerX", computerPos.getX());
            nbt.putInt("computerY", computerPos.getY());
            nbt.putInt("computerZ", computerPos.getZ());
        }
        nbt.putInt("gridX", gridX);
        nbt.putInt("gridY", gridY);
        nbt.putInt("gridWidth", gridWidth);
        nbt.putInt("gridHeight", gridHeight);
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
