package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** 内側の空間から厚さ1マスだけを拾う。外装につながった地面を連結探索しない。 */
record ShellSurvey(BlockPos lo,BlockPos hi,Map<BlockPos,BlockState> blocks,int interior) {
    private static boolean open(BlockState s){return s.isAir() || s.hasProperty(BlockStateProperties.OPEN) && s.getValue(BlockStateProperties.OPEN);}
    private static Set<BlockPos> enclosed(ServerLevel l,BlockPos seed,BlockPos core){
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> q=new ArrayDeque<>();seen.add(seed);q.add(seed);
        BlockPos lo=core,hi=core;
        while(!q.isEmpty()){
            var p=q.remove();for(Direction d:Direction.values()){
                var n=p.relative(d);if(seen.contains(n))continue;
                if(l.isOutsideBuildHeight(n)||!l.hasChunkAt(n)||!l.getWorldBorder().isWithinBounds(n))return Set.of();
                if(!open(l.getBlockState(n)))continue;
                lo=CabinRooms.min(lo,n);hi=CabinRooms.max(hi,n);
                if(!ShipWrench.validRange(lo,hi,core) || seen.size()>=ShipWrench.MAX_VOLUME)return Set.of();
                seen.add(n);q.add(n);
            }
        }return seen;
    }
    static ShellSurvey scan(ServerLevel l,BlockPos core){
        if(!l.getBlockState(core).is(ShipParts.CORE.get()))return null;
        Set<BlockPos> space=Set.of();
        for(Direction d:Direction.values()){
            var seed=core.relative(d);if(!l.hasChunkAt(seed)||space.contains(seed)||!open(l.getBlockState(seed)))continue;
            var found=enclosed(l,seed,core);if(found.isEmpty())continue;
            if(!space.isEmpty())return null; // コアの両側が別の密閉区画。範囲選択に任せる。
            space=found;
        }
        if(space.stream().noneMatch(p->l.isEmptyBlock(p) && l.isEmptyBlock(p.above()) && !l.isEmptyBlock(p.below())))return null;
        Map<BlockPos,BlockState> cells=new HashMap<>();BlockPos lo=core,hi=core;
        for(var p:space)for(var cursor:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1))){
            if(!l.hasChunkAt(cursor)||l.isOutsideBuildHeight(cursor)||!l.getWorldBorder().isWithinBounds(cursor))return null;
            var state=l.getBlockState(cursor);if(state.isAir()||cells.containsKey(cursor))continue;
            if(!cursor.equals(core) && state.is(ShipParts.CORE.get()))return null;
            var n=cursor.immutable();cells.put(n,state);lo=CabinRooms.min(lo,n);hi=CabinRooms.max(hi,n);
            if(cells.size()>ShipWrench.MAX_PARTS || !ShipWrench.validRange(lo,hi,core))return null;
        }return cells.containsKey(core)?new ShellSurvey(lo,hi,Map.copyOf(cells),space.size()):null;
    }
    CompoundTag selection(BlockPos core){
        var t=new CompoundTag();t.putLong("core",core.asLong());t.putLong("a",lo.asLong());t.putLong("b",hi.asLong());
        t.putLongArray("toggle",blocks.keySet().stream().mapToLong(BlockPos::asLong).toArray());return t;
    }
}
