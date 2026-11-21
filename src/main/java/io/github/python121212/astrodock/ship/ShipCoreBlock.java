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
        if(!player.getItemInHand(hand).isEmpty() || hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(player.isSpectator() || !player.getAbilities().mayBuild || player.isPassenger() || !level.mayInteract(player,pos) ||
                !(io.github.python121212.astrodock.AstroDock.supported(level) || level.dimension()==CabinRooms.LEVEL))return InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraftforge.network.NetworkHooks.openScreen(sp,
                new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new CoreMenu(id,inv,pos),getName()),pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
