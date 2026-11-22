package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class LaserCutterItem extends Item {
    public LaserCutterItem(Properties properties) {
        super(properties.durability(512));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 14.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SlopeBlock) {
            if (!level.isClientSide) {
                BlockState next = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()
                        ? state.setValue(SlopeBlock.HALF, state.getValue(SlopeBlock.HALF) == Half.BOTTOM ? Half.TOP : Half.BOTTOM)
                        : state.setValue(SlopeBlock.FACING, state.getValue(SlopeBlock.FACING).getClockWise());
                level.setBlock(pos, next, 3);
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
