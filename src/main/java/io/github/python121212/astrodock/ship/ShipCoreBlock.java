package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 従来の艦船コアを同じ登録名のまま拡張する。ブロックエンティティは不要。 */
public final class ShipCoreBlock extends Block {
    ShipCoreBlock(Properties properties) { super(properties); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!player.getItemInHand(hand).isEmpty() || hand!=InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if(!level.isClientSide) player.displayClientMessage(Component.translatable("hint.astrodock.ship_core"),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
