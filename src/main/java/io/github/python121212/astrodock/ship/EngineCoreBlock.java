package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class EngineCoreBlock extends Block implements EntityBlock {
    EngineCoreBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EngineAnchor(p,s);}
}
