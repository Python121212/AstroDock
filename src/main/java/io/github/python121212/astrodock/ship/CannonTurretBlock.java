package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
            var held = player.getItemInHand(hand);
            if (held.is(ShipParts.LASER_CUTTER.get()) || held.is(ShipParts.WRENCH.get())) {
                turret.toggleMode();
                String mode = turret.isEnergyMode() ? "エネルギー兵器" : "実体弾";
                player.displayClientMessage(Component.literal("射撃モード切替: " + mode), true);
                return InteractionResult.sidedSuccess(false);
            }

            int barrels = turret.countBarrels(level, pos);
            int breeches = turret.countBreeches(level, pos);

            if (player.isShiftKeyDown()) {
                float yawRad = turret.yaw() * Mth.DEG_TO_RAD;
                float pitchRad = turret.pitch() * Mth.DEG_TO_RAD;
                double dx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
                double dy = Mth.sin(pitchRad);
                double dz = Mth.cos(yawRad) * Mth.cos(pitchRad);
                Vec3 dir = new Vec3(dx, dy, dz).normalize();

                double speed = (turret.isEnergyMode() ? 3.5 : 2.0) + barrels * 0.25 + breeches * 0.15;
                ShipShot shot = new ShipShot(ShipParts.SHELL.get(), level);
                shot.setOwner(player);
                shot.setPos(pos.getX() + 0.5 + dir.x * (1.5 + barrels), pos.getY() + 0.5 + dir.y * 1.5, pos.getZ() + 0.5 + dir.z * (1.5 + barrels));
                shot.setDeltaMovement(dir.scale(speed));
                if (turret.isEnergyMode()) {
                    shot.setNoGravity(true);
                }
                level.addFreshEntity(shot);
                level.playSound(null, pos, turret.isEnergyMode() ? SoundEvents.BEACON_ACTIVATE : SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, turret.isEnergyMode() ? 1.8f : 0.9f);
                String mode = turret.isEnergyMode() ? "エネルギー弾" : "実体弾";
                player.displayClientMessage(Component.literal(mode + " 発射！ (砲身長: " + barrels + " / 薬室: " + breeches + ")"), true);
            } else {
                float targetYaw = ((player.getYRot() + 180.0f) % 360.0f + 360.0f) % 360.0f;
                float targetPitch = Math.max(-15.0f, Math.min(60.0f, -player.getXRot()));
                turret.setAim(targetYaw, targetPitch);
                String mode = turret.isEnergyMode() ? "エネルギー兵器" : "実体弾";
                player.displayClientMessage(Component.literal(
                        String.format("砲台制御 [%s]: 方位 %.0f° / 仰角 %.0f° | 砲身長: %d | 薬室: %d", mode, targetYaw, targetPitch, barrels, breeches)
                ), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
