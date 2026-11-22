package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CannonMount extends BlockEntity {
    private long shots, readyAt;
    private float yaw;

    CannonMount(BlockPos p, BlockState s) {
        super(ShipParts.CANNON_MOUNT.get(), p, s);
    }

    void fired(long next) { shots++; readyAt = next; setChanged(); }
    long readyAt() { return readyAt; }
    long shots() { return shots; }

    public float yaw() { return yaw; }
    public void setYaw(float yaw) {
        this.yaw = ((yaw % 360.0f) + 360.0f) % 360.0f;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putLong("Pulses", shots);
        t.putLong("ReadyAt", readyAt);
        t.putFloat("Yaw", yaw);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        shots = t.getLong("Pulses");
        readyAt = t.getLong("ReadyAt");
        yaw = t.getFloat("Yaw");
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
