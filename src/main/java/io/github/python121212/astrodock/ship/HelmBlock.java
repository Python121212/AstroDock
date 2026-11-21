package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** 操縦桿は受動的な船体部品。設置中に tick するブロックエンティティは作らない。 */
final class HelmBlock extends Block {
    private static final VoxelShape SHAPE=Shapes.or(box(1,0,1,15,5,15),box(6,5,6,10,14,10),box(5,12,5,11,16,11));
    HelmBlock(Properties properties) { super(properties.noOcclusion()); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
