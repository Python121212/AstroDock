package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** 船体ローカル座標の通過面。クリックではなく前後2地点が開口を横切った時だけ通る。 */
record Walkway(BlockPos pos,Direction inward,Block block) {
    private static final Map<ServerPlayer,Sample> last=new WeakHashMap<>();
    private record Sample(ServerLevel world,Vec3 feet,long time){}
    static List<Walkway> find(List<Hull.Part> parts){
        var occupied=new HashSet<BlockPos>();BlockPos lo=parts.get(0).pos(),hi=lo;
        for(var part:parts){occupied.add(part.pos());lo=CabinRooms.min(lo,part.pos());hi=CabinRooms.max(hi,part.pos());}
        var result=new ArrayList<Walkway>();
        for(var part:parts){var state=part.state();if(!HullDoors.airlock(state))continue;
            boolean hatch=state.is(ShipParts.HATCH.get());
            if(!hatch && state.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER)continue;
            var normal=hatch?Direction.UP:state.getValue(DoorBlock.FACING);var p=part.pos();
            int a=outsideDistance(p,normal,lo,hi,occupied),b=outsideDistance(p,normal.getOpposite(),lo,hi,occupied);
            if(a==Integer.MAX_VALUE && b==Integer.MAX_VALUE)continue;
            Direction in=a<=b?normal.getOpposite():normal;
            result.add(new Walkway(p,in,state.getBlock()));
        }return List.copyOf(result);
    }
    private static int outsideDistance(BlockPos p,Direction d,BlockPos lo,BlockPos hi,Set<BlockPos> cells){
        for(int i=1;i<=17;i++){var n=p.relative(d,i);if(cells.contains(n))break;
            if(n.getX()<lo.getX()||n.getX()>hi.getX()||n.getY()<lo.getY()||n.getY()>hi.getY()||n.getZ()<lo.getZ()||n.getZ()>hi.getZ())return i;
        }return Integer.MAX_VALUE;
    }
    private boolean vertical(){return inward.getAxis()==Direction.Axis.Y;}
    boolean crossed(Vec3 from,Vec3 to,boolean enter,double height){
        Vec3 normal=Vec3.atLowerCornerOf((enter?inward:inward.getOpposite()).getNormal());
        Vec3 center=Vec3.atCenterOf(pos);
        // ハッチは上昇なら頭、下降なら足で面を横切る。外周バリアに頭が当たる前に判定。
        double y=vertical()?(normal.y>0?height-.1:.1):height*.5;
        Vec3 a=from.add(0,y,0).subtract(center),b=to.add(0,y,0).subtract(center);double before=a.dot(normal),after=b.dot(normal);
        if(before>=0 || after<0)return false;
        Vec3 hit=a.lerp(b,-before/(after-before));
        if(vertical())return Math.abs(hit.x)<=.33 && Math.abs(hit.z)<=.33;
        return hit.y>=-.45 && hit.y<=1.45 && Math.abs(inward.getAxis()==Direction.Axis.X?hit.z:hit.x)<=.33;
    }
    Vec3 landing(boolean enter,double height){
        Direction dir=enter?inward:inward.getOpposite();Vec3 at=Vec3.atBottomCenterOf(pos);
        if(vertical())return at.add(0,dir==Direction.UP?1.01:-height-.01,0);
        return at.add(Vec3.atLowerCornerOf(dir.getNormal()).scale(.93));
    }
    boolean isOpen(ServerLevel c,CabinRooms.Room r){var state=c.getBlockState(r.core.offset(pos));return state.is(block) && state.getValue(BlockStateProperties.OPEN);}
    static void step(ServerPlayer p){
        var level=p.serverLevel();var now=p.position();long tick=level.getGameTime();var prev=last.put(p,new Sample(level,now,tick));
        if(prev==null || prev.world!=level || tick-prev.time>1 || p.isSpectator() || !p.isAlive() || p.isPassenger())return;
        double dist=now.distanceToSqr(prev.feet);if(dist<1e-8 || dist>16)return;
        if(level.dimension()==CabinRooms.LEVEL){
            var room=CabinRooms.visiting(p);if(room==null)return;
            Vec3 origin=Vec3.atLowerCornerOf(room.core),a=prev.feet.subtract(origin),b=now.subtract(origin);
            // 身近に実際の艦外ドアがなければ船外エンティティを検索しない。
            if(!nearOpening(level,prev.feet,now,p.getBbHeight()))return;
            var dest=level.getServer().getLevel(room.dimension);
            if(dest!=null && dest.getEntity(room.shipId) instanceof Ship s)s.passDoor(p,room,a,b,false);
        }else {
            for(Ship s:level.getEntitiesOfClass(Ship.class,new AABB(prev.feet,now).inflate(1,2,1))){
                if(s.hull()==null || s.hull().walkways.isEmpty())continue;
                if(s.walkIn(p,prev.feet,now))break;
            }
        }
    }
    private static boolean nearOpening(ServerLevel level,Vec3 a,Vec3 b,float height){
        var box=new AABB(a,b).expandTowards(0,height,0).inflate(.05);
        for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(HullDoors.airlock(level.getBlockState(p)))return true;
        return false;
    }
    static boolean fits(ServerLevel l,ServerPlayer p,Vec3 at){
        var box=p.getDimensions(p.getPose()).makeBoundingBox(at);return l.hasChunkAt(BlockPos.containing(at)) && l.getWorldBorder().isWithinBounds(box) && l.noCollision(p,box)
                && l.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,box,other->other!=p && !other.isSpectator()).isEmpty();
    }
}
