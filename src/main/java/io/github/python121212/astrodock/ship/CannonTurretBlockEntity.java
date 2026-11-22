package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CannonTurretBlockEntity extends BlockEntity {
    private float yaw;
    private float pitch;
    private int powderCharge;
    private boolean energyMode;

    public CannonTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ShipParts.CANNON_TURRET_BE.get(), pos, state);
    }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public int powderCharge() { return powderCharge; }
    public boolean isEnergyMode() { return energyMode; }
    public void toggleMode() {
        energyMode = !energyMode;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setAim(float yaw, float pitch) {
        this.yaw = ((yaw % 360.0f) + 360.0f) % 360.0f;
        this.pitch = Math.max(-15.0f, Math.min(60.0f, pitch));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int countBarrels(Level level, BlockPos origin) {
        int maxLen = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int len = 0;
            for (int i = 1; i <= 16; i++) {
                if (level.getBlockState(origin.relative(dir, i)).is(ShipParts.CANNON_BARREL.get())) {
                    len++;
                } else {
                    break;
                }
            }
            if (len > maxLen) maxLen = len;
        }
        return maxLen;
    }

    public int countBreeches(Level level, BlockPos origin) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(origin.relative(dir)).is(ShipParts.CANNON_BREECH.get())) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putInt("PowderCharge", powderCharge);
        tag.putBoolean("EnergyMode", energyMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        yaw = tag.getFloat("Yaw");
        pitch = tag.getFloat("Pitch");
        powderCharge = tag.getInt("PowderCharge");
        energyMode = tag.getBoolean("EnergyMode");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
