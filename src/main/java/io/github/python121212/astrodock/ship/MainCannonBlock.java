package io.github.python121212.astrodock.ship;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** アイテムと艦上の描画にも同じ焼き込みモデルを使う。設置中の毎 tick 処理は不要。 */
final class MainCannonBlock extends HorizontalDirectionalBlock implements EntityBlock {
    static final int REQUIRED_OUTPUT=2;
    MainCannonBlock(Properties props) {
        super(props.noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos p,CollisionContext ctx){return box(0,0,0,16,10,16);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CannonMount(p,s);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!p.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(!l.isClientSide && l.getBlockEntity(pos) instanceof CannonMount mount)
            p.displayClientMessage(Component.translatable("hint.astrodock.cannon_log",mount.shots(),REQUIRED_OUTPUT),true);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
