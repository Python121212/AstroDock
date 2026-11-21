package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

/** 相対比較用。結果はサーバー全体の MSPT ではなく、指定隻数1ステップの中央値。 */
@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class PhysicsBench {
    private static volatile double sink;
    private static long median(int ticks, Runnable step) {
        for(int i=0;i<ticks;i++)step.run();
        long[] times=new long[5];
        for(int s=0;s<times.length;s++) {
            long start=System.nanoTime();for(int i=0;i<ticks;i++)step.run();
            times[s]=(System.nanoTime()-start)/ticks;
        }
        Arrays.sort(times);return times[2];
    }
    private static void sample(Hull h,int count,ServerLevel level,BlockPos at) {
        RigidBody[] bodies=new RigidBody[count];Vec3[] v=new Vec3[count];
        LegacyMotion[] old=new LegacyMotion[count];float[] yaw=new float[count];
        AABB[] boxes=new AABB[count];
        for(int i=0;i<count;i++) {
            bodies[i]=new RigidBody();v[i]=Vec3.ZERO;old[i]=new LegacyMotion(Vec3.ZERO,0);
            // 同じ読み込み済みの構造物付近。半数は上空。衝突問い合わせの条件を固定。
            boxes[i]=new AABB(at.offset(i%4,(i%2)*24,i%5)).inflate(h.radiusX,0.5*h.height,h.radiusZ);
        }
        int ticks=2048;
        long legacy=median(ticks,()->{
            for(int i=0;i<count;i++){old[i]=old[i].step(h,4000,yaw[i],0.7,0.2,0.3);yaw[i]+=old[i].spin();}
            sink=old[0].velocity().z;
        });
        long physics=median(ticks,()->{
            for(int i=0;i<count;i++)v[i]=bodies[i].step(h,v[i],4000,0.7f,0.2f,0.3f,0);
            sink=v[0].z;
        });
        long query=median(32,()->{int hits=0;for(AABB b:boxes)if(!level.noCollision(null,b))hits++;sink=hits;});
        Ship probe=new Ship(ShipParts.SHIP.get(),level);probe.configure(h,java.util.UUID.randomUUID());
        for(int i=0;i<count;i++)boxes[i]=boxes[i].move(0,64,0);
        long emptyVanilla=median(64,()->{int hits=0;for(AABB b:boxes)if(level.noCollision(probe,b))hits++;sink=hits;});
        long emptyFast=median(64,()->{int hits=0;for(AABB b:boxes)if(ClearFlight.clear(level,probe,b))hits++;sink=hits;});
        com.mojang.logging.LogUtils.getLogger().info("ASTRO_BENCH blocks={} ships={} old_ns={} six_dof_ns={} collision_query_ns={} empty_vanilla_ns={} empty_fast_ns={}",h.parts.size(),count,legacy,physics,query,emptyVanilla,emptyFast);
    }
    @GameTest(templateNamespace="minecraft", template="igloo/top")
    public static void profileFleet(GameTestHelper test) {
        for(int n:new int[]{8,256}) {
            List<Hull.Part> parts=new ArrayList<>();for(int i=0;i<n;i++)parts.add(new Hull.Part(new BlockPos(i%8,(i/8)%4,i/32),
                    (i%8==0?ShipParts.ENGINE:ShipParts.CORE).get().defaultBlockState(),2));
            Hull h=new Hull(parts);for(int count:new int[]{1,32})sample(h,count,test.getLevel(),test.absolutePos(BlockPos.ZERO));
        }
        test.succeed();
    }
}
