package io.github.python121212.astrodock.ship;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** コアのブロック原点を基準にした足元座標。歩行中は床と身体の近傍だけを引く。 */
final class CabinDeck {
    private final Long2ObjectOpenHashMap<BlockState> cells = new Long2ObjectOpenHashMap<>();
    final Vec3 entrance;

    CabinDeck(List<Hull.Part> parts) {
        BlockPos station=BlockPos.ZERO;
        int top=Integer.MIN_VALUE;
        for (var part:parts) {
            cells.put(part.pos().asLong(),part.state());
            if (part.state().is(ShipParts.HELM.get())) station=part.pos();
            top=Math.max(top,part.pos().getY());
        }
        Vec3 best=null; double score=Double.POSITIVE_INFINITY;
        for (var part:parts) {
            BlockPos foot=part.pos().above();
            Vec3 candidate=Vec3.atBottomCenterOf(foot);
            if (!canStand(candidate.x,candidate.y,candidate.z)) continue;
            // 甲板の上より、天井のある船室を優先する。
            boolean roof=false;
            for(int y=foot.getY()+2;y<=top;y++) if(solid(foot.getX(),y,foot.getZ())) {roof=true;break;}
            double cost=foot.distSqr(station)+(roof?0:1000);
            if(cost<score){best=candidate;score=cost;}
        }
        entrance=best;
    }
    private boolean solid(int x,int y,int z) {return cells.containsKey(BlockPos.asLong(x,y,z));}
    boolean canStand(double x,double y,double z) {
        int floor=Mth.floor(y);
        if(y!=floor || !solid(Mth.floor(x),floor-1,Mth.floor(z))) return false;
        // 幅0.6、高さ1.8。操縦桿は通り抜けられない設備として1セルを占有する。
        for(int i=Mth.floor(x-.2999);i<=Mth.floor(x+.2999);i++)
            for(int k=Mth.floor(z-.2999);k<=Mth.floor(z+.2999);k++)
                if(solid(i,floor,k)||solid(i,floor+1,k)) return false;
        return true;
    }
    Vector3f advance(Vector3f feet,Vec3 forward,float ahead,float left) {
        double norm=Math.hypot(forward.x,forward.z);
        if(norm<1e-5 || (ahead==0 && left==0)) return feet;
        double speed=.17/(norm*Math.max(1,Math.hypot(ahead,left)));
        float x=(float)(feet.x+(forward.x*ahead+forward.z*left)*speed);
        float z=(float)(feet.z+(forward.z*ahead-forward.x*left)*speed);
        if (!canStand(x,feet.y,feet.z)) x=feet.x;
        if (!canStand(x,feet.y,z)) z=feet.z;
        return x==feet.x && z==feet.z ? feet : new Vector3f(x,feet.y,z);
    }
    BlockState aimedBlock(Vec3 eye,Vec3 direction) {
        return BlockGetter.traverseBlocks(eye,eye.add(direction.scale(4.5)),cells,
                (index,pos)->index.get(pos.asLong()),index->null);
    }
}
