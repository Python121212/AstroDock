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

public class PlanetScannerBlock extends Block {
    public PlanetScannerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            int ores = 0;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-16, -16, -16), pos.offset(16, 16, 16))) {
                BlockState bs = level.getBlockState(p);
                if (bs.is(ShipParts.STRATASTEEL_ORE.get()) || bs.is(ShipParts.BLACKSHELL_ORE.get())) {
                    ores++;
                }
            }
            player.displayClientMessage(Component.literal("スキャン完了: 半径16m以内に宇宙鉱石 " + ores + " ブロック検出"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
