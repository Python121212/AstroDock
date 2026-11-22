package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CannonTurretBlock extends Block implements EntityBlock {
    public CannonTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CannonTurretBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CannonTurretBlockEntity turret) {
            float targetYaw = ((player.getYRot() + 180.0f) % 360.0f + 360.0f) % 360.0f;
            float targetPitch = Math.max(-15.0f, Math.min(60.0f, -player.getXRot()));
            turret.setAim(targetYaw, targetPitch);
            int barrels = turret.countBarrels(level, pos);
            int breeches = turret.countBreeches(level, pos);
            player.displayClientMessage(Component.literal(
                    String.format("砲台制御: 方位 %.0f° / 仰角 %.0f° | 砲身長: %d | 薬室: %d", targetYaw, targetPitch, barrels, breeches)
            ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
