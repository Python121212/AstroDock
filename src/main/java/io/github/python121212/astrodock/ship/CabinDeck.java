package io.github.python121212.astrodock.ship;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** コアのブロック原点を基準にした足元座標。歩行中は床と身体の近傍だけを引く。 */
final class CabinDeck {
    private final Long2ObjectOpenHashMap<BlockState> cells = new Long2ObjectOpenHashMap<>();
    private final List<Vec3> entrances;

    CabinDeck(List<Hull.Part> parts) {
        BlockPos station=BlockPos.ZERO;
        int top=Integer.MIN_VALUE;
        for (var part:parts) {
            cells.put(part.pos().asLong(),part.state());
            if (part.state().is(ShipParts.HELM.get())) station=part.pos();
            top=Math.max(top,part.pos().getY());
        }
        record Landing(Vec3 feet,double cost) {}
        var candidates=new ArrayList<Landing>();
        for (var part:parts) {
            BlockPos foot=part.pos().above();
            Vec3 candidate=Vec3.atBottomCenterOf(foot);
            if (!canStand(candidate.x,candidate.y,candidate.z)) continue;
            // 甲板の上より、天井のある船室を優先する。
            boolean roof=false;
            for(int y=foot.getY()+2;y<=top;y++) if(solid(foot.getX(),y,foot.getZ())) {roof=true;break;}
            double cost=foot.distSqr(station)+(roof?0:1000);
            candidates.add(new Landing(candidate,cost));
        }
        candidates.sort(Comparator.comparingDouble(Landing::cost));
        entrances=candidates.stream().map(Landing::feet).toList();
    }
    private boolean solid(int x,int y,int z) {return cells.containsKey(BlockPos.asLong(x,y,z));}
    Vec3 entrance(Predicate<Vec3> free) {
        for(Vec3 candidate:entrances) if(free.test(candidate)) return candidate;
        return null;
    }
    private boolean clear(double x,int floor,double z) {
        for(int i=Mth.floor(x-.2999);i<=Mth.floor(x+.2999);i++)
            for(int k=Mth.floor(z-.2999);k<=Mth.floor(z+.2999);k++)
                if(solid(i,floor,k)||solid(i,floor+1,k)) return false;
        return true;
    }
    boolean canStand(double x,double y,double z) {
        int floor=Mth.floor(y);
        if(y!=floor || !clear(x,floor,z)) return false;
        // 足裏の一部が載れば段の縁でも体重を支えられる。
        for(int i=Mth.floor(x-.2999);i<=Mth.floor(x+.2999);i++)
            for(int k=Mth.floor(z-.2999);k<=Mth.floor(z+.2999);k++)
                if(solid(i,floor-1,k)) return true;
        return false;
    }
    private Vector3f slide(Vector3f from,float x,float z,int initialY) {
        if(x==from.x && z==from.z) return from;
        int y=(int)from.y;
        if(canStand(x,y,z)) return new Vector3f(x,y,z);
        // 上る時は先に身体を持ち上げ、下りる時は先に段の縁を越す。
        // 終点だけでなく途中の頭上も確認し、斜め移動でも1 tickに2段は越えない。
        if(y<=initialY && clear(from.x,y+1,from.z) && canStand(x,y+1,z)) return new Vector3f(x,y+1,z);
        if(y>=initialY && clear(x,y,z) && canStand(x,y-1,z)) return new Vector3f(x,y-1,z);
        return from;
    }
    Vector3f advance(Vector3f feet,Vec3 forward,float ahead,float left) {
        double norm=Math.hypot(forward.x,forward.z);
        if(norm<1e-5 || (ahead==0 && left==0)) return feet;
        double speed=.17/(norm*Math.max(1,Math.hypot(ahead,left)));
        float x=(float)(feet.x+(forward.x*ahead+forward.z*left)*speed);
        float z=(float)(feet.z+(forward.z*ahead-forward.x*left)*speed);
        Vector3f first=slide(feet,x,feet.z,(int)feet.y);
        return slide(first,first.x,z,(int)feet.y);
    }
    boolean clearMuzzle(BlockPos mount,Vec3 direction) {
        Vec3 center=Vec3.atCenterOf(mount);
        return BlockGetter.traverseBlocks(center,center.add(direction.scale(32)),cells,
                (index,pos)->!pos.equals(mount) && index.containsKey(pos.asLong())?Boolean.FALSE:null,index->Boolean.TRUE);
    }
    BlockState aimedBlock(Vec3 eye,Vec3 direction,double reach) {
        return BlockGetter.traverseBlocks(eye,eye.add(direction.scale(reach)),cells,
                (index,pos)->index.get(pos.asLong()),index->null);
    }
}
