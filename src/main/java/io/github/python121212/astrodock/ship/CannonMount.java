package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CannonMount extends BlockEntity {
    private long shots;
    CannonMount(BlockPos p,BlockState s){super(ShipParts.CANNON_MOUNT.get(),p,s);}
    void fired(){shots++;setChanged();}
    long shots(){return shots;}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putLong("Pulses",shots);}
    @Override public void load(CompoundTag t){super.load(t);shots=t.getLong("Pulses");}
}
