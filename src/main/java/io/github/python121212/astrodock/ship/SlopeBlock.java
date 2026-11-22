package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SlopeBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    private static final VoxelShape BOTTOM_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 8, 16, 16, 16)
    );
    private static final VoxelShape BOTTOM_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 16, 16, 8)
    );
    private static final VoxelShape BOTTOM_WEST = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(8, 8, 0, 16, 16, 16)
    );
    private static final VoxelShape BOTTOM_EAST = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 8, 16, 16)
    );

    private static final VoxelShape TOP_NORTH = Shapes.or(
            Block.box(0, 8, 0, 16, 16, 16),
            Block.box(0, 0, 8, 16, 8, 16)
    );
    private static final VoxelShape TOP_SOUTH = Shapes.or(
            Block.box(0, 8, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 8, 8)
    );
    private static final VoxelShape TOP_WEST = Shapes.or(
            Block.box(0, 8, 0, 16, 16, 16),
            Block.box(8, 0, 0, 16, 8, 16)
    );
    private static final VoxelShape TOP_EAST = Shapes.or(
            Block.box(0, 8, 0, 16, 16, 16),
            Block.box(0, 0, 0, 8, 8, 16)
    );

    public SlopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        BlockPos pos = ctx.getClickedPos();
        Half half = (face == Direction.DOWN || (face != Direction.UP && ctx.getClickLocation().y - pos.getY() > 0.5))
                ? Half.TOP
                : Half.BOTTOM;
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection())
                .setValue(HALF, half);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean top = state.getValue(HALF) == Half.TOP;
        return switch (state.getValue(FACING)) {
            case SOUTH -> top ? TOP_SOUTH : BOTTOM_SOUTH;
            case WEST -> top ? TOP_WEST : BOTTOM_WEST;
            case EAST -> top ? TOP_EAST : BOTTOM_EAST;
            default -> top ? TOP_NORTH : BOTTOM_NORTH;
        };
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
