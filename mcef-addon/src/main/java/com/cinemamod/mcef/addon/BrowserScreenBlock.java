package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;

import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

public class BrowserScreenBlock extends BaseEntityBlock {
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<BrowserScreenBlock> CODEC = simpleCodec(BrowserScreenBlock::new);

    public BrowserScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            updateMultiblock(level, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide) {
            updateMultiblock(level, pos, state);
        }
    }

    private void updateMultiblock(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        
        // Find boundaries
        int minX = 0, maxX = 0, minY = 0, maxY = 0;

        // Horizontal scan direction depends on facing
        Direction leftDir = facing.getClockWise();
        Direction rightDir = facing.getCounterClockWise();

        // Scan Left
        BlockPos current = pos;
        while (isValidPart(level, current.relative(leftDir), facing)) {
            current = current.relative(leftDir);
            minX--;
        }
        BlockPos bottomLeftMost = current;

        // Scan Right
        current = pos;
        while (isValidPart(level, current.relative(rightDir), facing)) {
            current = current.relative(rightDir);
            maxX++;
        }

        // Scan Down from the bottom-left-most line to find the true bottom-left
        current = bottomLeftMost;
        while (isValidPart(level, current.below(), facing)) {
            current = current.below();
            minY--;
        }
        BlockPos masterPos = current;

        // Scan Up to find height
        current = masterPos;
        while (isValidPart(level, current.above(), facing)) {
            current = current.above();
            maxY++;
        }

        // Scan Right from masterPos to find width
        current = masterPos;
        int width = 1;
        while (isValidPart(level, current.relative(rightDir), facing)) {
            current = current.relative(rightDir);
            width++;
        }
        int height = maxY + 1;

        // Verify if it's a full rectangle
        boolean isFull = true;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                BlockPos p = masterPos.relative(rightDir, x).above(y);
                if (!isValidPart(level, p, facing)) {
                    isFull = false;
                    break;
                }
            }
            if (!isFull) break;
        }

        if (isFull && width >= 1 && height >= 1) {
            // Find an adjacent computer
            BlockPos computerPos = null;
            outer: for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    BlockPos p = masterPos.relative(rightDir, x).above(y);
                    for (Direction dir : Direction.values()) {
                        BlockPos neighbor = p.relative(dir);
                        if (level.getBlockState(neighbor).getBlock() instanceof BrowserComputerBlock) {
                            computerPos = neighbor;
                            break outer;
                        }
                    }
                }
            }

            // Update all blocks in the rectangle
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    BlockPos p = masterPos.relative(rightDir, x).above(y);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof BrowserBlockEntity bbe) {
                        bbe.setMultiblock(masterPos, x, y, width, height, computerPos);
                    }
                }
            }
        }
    }

    private boolean isValidPart(Level level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.is(this) && state.getValue(FACING) == facing;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrowserBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BrowserBlockEntity browserBe) {
                // Shift + Right Click to open URL GUI - Always available on client
                if (player.isShiftKeyDown()) {
                    Minecraft.getInstance().setScreen(new BrowserUrlScreen(browserBe));
                    return InteractionResult.SUCCESS;
                }

                // Normal Right Click to interact with browser
                interactWithBrowser(state, level, pos, hit, 0, false); // Left click by default
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    public static void interactWithBrowser(BlockState state, Level level, BlockPos pos, BlockHitResult hit, int button, boolean isRelease) {
        interactWithBrowser(state, level, pos, hit, button, isRelease, true);
    }

    public static void interactWithBrowser(BlockState state, Level level, BlockPos pos, BlockHitResult hit, int button, boolean isRelease, boolean autoRelease) {
        if (!level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BrowserBlockEntity browserBe) {
            IMCEFBrowser browser = browserBe.getBrowser();
            if (browser != null) {
                Vec3 hitPos = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                
                Direction facing = state.getValue(FACING);
                double u = 0;
                double v = hitPos.y; // 0 to 1, bottom to top

                switch (facing) {
                    case NORTH -> u = 1.0 - hitPos.x;
                    case SOUTH -> u = hitPos.x;
                    case EAST -> u = 1.0 - hitPos.z;
                    case WEST -> u = hitPos.z;
                }

                // Map to global browser coordinates
                int px = (int) ((browserBe.getGridX() + u) * 512);
                int py = (int) ((browserBe.getGridHeight() - 1 - browserBe.getGridY() + (1.0 - v)) * 384);

                if (button == -1) {
                    // Mouse move only
                    browser.sendMouseMove(px, py);
                } else if (isRelease) {
                    browser.sendMouseRelease(px, py, button);
                } else {
                    browser.sendMousePress(px, py, button);
                    // For a simple click, we usually send release immediately after press
                    // unless we are doing long press.
                    if (autoRelease) {
                         browser.sendMouseRelease(px, py, button);
                    }
                }
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
