package com.cinemamod.mcef.addon;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BrowserClickerItem extends Item {
    public BrowserClickerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        
        if (state.getBlock() instanceof BrowserScreenBlock) {
            if (level.isClientSide) {
                Player player = context.getPlayer();
                if (player != null) {
                    player.startUsingItem(context.getHand());
                }
            }
            return InteractionResult.CONSUME;
        }
        
        return super.useOn(context);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) hitResult;
                BlockState state = level.getBlockState(hit.getBlockPos());
                
                if (state.getBlock() instanceof BrowserScreenBlock) {
                    int button = player.isShiftKeyDown() ? 2 : 0;
                    int usedDuration = getUseDuration(stack, livingEntity) - remainingUseDuration;
                    
                    if (usedDuration == 1) {
                        // 初次按下
                        BrowserScreenBlock.interactWithBrowser(state, level, hit.getBlockPos(), hit, button, false, false);
                        player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
                    } else {
                        // 持续按下 (拖拽)
                        // 我们发送 -1 作为 button 来表示只移动鼠标
                        BrowserScreenBlock.interactWithBrowser(state, level, hit.getBlockPos(), hit, -1, false, false);
                    }
                }
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) hitResult;
                BlockState state = level.getBlockState(hit.getBlockPos());
                
                if (state.getBlock() instanceof BrowserScreenBlock) {
                    int button = player.isShiftKeyDown() ? 2 : 0;
                    BrowserScreenBlock.interactWithBrowser(state, level, hit.getBlockPos(), hit, button, true);
                }
            }
        }
        return super.releaseUsing(stack, level, livingEntity, remainingUseDuration);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 允许在空气中右键点击（虽然没啥用，但保持一致性）
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // 足够长的持续时间
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW; // 使用弓的动画，看起来像是在指着屏幕
    }
}
