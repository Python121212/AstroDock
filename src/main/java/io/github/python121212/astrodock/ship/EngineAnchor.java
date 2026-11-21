package io.github.python121212.astrodock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 相対座標の範囲だけを保存。設定しなければ船体選択範囲全体が対象。 */
final class EngineAnchor extends BlockEntity {
    EnginePlant.Range range;
    EngineAnchor(BlockPos p,BlockState s){super(ShipParts.ANCHOR.get(),p,s);}
    void setRange(EnginePlant.Range r){range=r;setChanged();}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);if(range!=null){t.putLong("lo",range.a().asLong());t.putLong("hi",range.b().asLong());}}
    @Override public void load(CompoundTag t){super.load(t);range=t.contains("lo") && t.contains("hi")?new EnginePlant.Range(BlockPos.of(t.getLong("lo")),BlockPos.of(t.getLong("hi"))):null;}
}
