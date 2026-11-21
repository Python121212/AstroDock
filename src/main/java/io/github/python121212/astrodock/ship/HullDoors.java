package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

final class HullDoors {
    static boolean ordinary(BlockState s) {
        return !s.is(ShipParts.AIRLOCK.get()) && !s.is(ShipParts.HATCH.get()) &&
                (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof TrapDoorBlock || s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS));
    }
    // 部品を閉じたセルとして扱い、外から空セルを塗る。凹んだ外壁も検出する。
    static BlockPos exposed(Map<BlockPos,BlockState> cells, BlockPos min, BlockPos max) {
        if(cells.values().stream().noneMatch(HullDoors::ordinary)) return null;
        BlockPos lo=min.offset(-1,-1,-1), hi=max.offset(1,1,1);
        var queue=new ArrayDeque<BlockPos>(); var seen=new HashSet<BlockPos>(); queue.add(lo); seen.add(lo);
        while(!queue.isEmpty()) {
            var p=queue.removeFirst();
            for(Direction d:Direction.values()) {
                var n=p.relative(d);
                if(n.getX()<lo.getX() || n.getY()<lo.getY() || n.getZ()<lo.getZ() || n.getX()>hi.getX() || n.getY()>hi.getY() || n.getZ()>hi.getZ() || !seen.add(n)) continue;
                var s=cells.get(n); if(s==null) queue.add(n); else if(ordinary(s)) return n;
            }
        }
        return null;
    }
}
