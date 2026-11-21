package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.AstroDock;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import org.joml.Vector3f;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class PhysicsGameTest {
    @GameTest(templateNamespace="minecraft", template="igloo/top")
    public static void rigidBodyContinuity(GameTestHelper test) {
        Hull h=new Hull(List.of(new Hull.Part(BlockPos.ZERO,ShipParts.CORE.get().defaultBlockState(),4),
                new Hull.Part(new BlockPos(0,-1,0),ShipParts.ENGINE.get().defaultBlockState(),2)));
        RigidBody body=new RigidBody();
        body.step(h,Vec3.ZERO,5000,1,0,0,0);
        test.assertTrue(body.attitude.x<0 && body.momentum.y==0,"低い位置の主機で機首が上がるはず");
        body.step(h,Vec3.ZERO,5000,0,0,0,1);
        test.assertTrue(body.momentum.z>0,"ロール用トルクがない");
        body.momentum.set(0.01f,-0.02f,0.03f);
        Vector3f original=new Vector3f(body.momentum);
        for(int i=0;i<8192;i++) body.step(h,Vec3.ZERO,5000,0,0,0,0);
        test.assertTrue(original.equals(body.momentum) && Math.abs(body.attitude.lengthSquared()-1)<1e-5,"真空での角運動量または姿勢の正規化");
        var orbit=test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        Ship ship=new Ship(ShipParts.SHIP.get(),orbit);ship.body.copyFrom(body);
        UUID owner=UUID.randomUUID();ship.configure(h,owner);ship.moveTo(256,-0.25,272,body.heading(0),0);
        Vec3 velocity=new Vec3(0.12,-0.2,-0.14);ship.setDeltaMovement(velocity);
        var out=ship.crossBoundary(orbit,null);
        test.assertTrue(out!=null && out.getY()==4999.75 && ship.isRemoved(),"降下時の境界移動に失敗");
        out.setPos(out.getX(),5000.25,out.getZ());
        var back=out.crossBoundary(test.getLevel().getServer().overworld(),null);
        test.assertTrue(back!=null && back.level()==orbit && back.getY()==0.25 && out.isRemoved(),"宇宙への再突入に失敗");
        test.assertTrue(back.body.attitude.equals(body.attitude) && back.body.momentum.equals(original) && back.getDeltaMovement().equals(velocity),"往復で運動状態が失われた");
        for(var part:h.parts) for(int i=0;i<8;i++) {
            var corner=new Vector3f((float)(part.pos().getX()-h.cx + ((i&1)==0?-0.5:0.5)),
                    (float)(part.pos().getY()-h.cy + ((i&2)==0?-0.5:0.5)), (float)(part.pos().getZ()-h.cz + ((i&4)==0?-0.5:0.5)));
            body.attitude.transform(corner);
            test.assertTrue(back.getBoundingBox().inflate(1e-5).contains(back.getX()+corner.x,back.getY()+h.pivotHeight+corner.y,back.getZ()+corner.z),"傾いた船体が衝突枠から外れている");
        }
        CompoundTag tag=new CompoundTag();back.addAdditionalSaveData(tag);
        Ship loaded=new Ship(ShipParts.SHIP.get(),orbit);loaded.readAdditionalSaveData(tag);
        test.assertTrue(Math.abs(loaded.body.attitude.dot(body.attitude))>0.99999 && loaded.body.momentum.equals(original),"保存復元の不一致");
        tag.remove("qw");tag.putDouble("spin",2);loaded.setYRot(35);loaded.readAdditionalSaveData(tag);
        test.assertTrue(Math.abs(loaded.body.heading(0)-35)<1e-3 && loaded.body.momentum.y<0,"0.3.0 データの移行失敗");
        // テスト構造物の真上の読み込み済み空間と、同じ場所に壁がある場合を比較。
        var level=test.getLevel();var block=test.absolutePos(new BlockPos(2,40,2));
        var volume=new net.minecraft.world.phys.AABB(block).inflate(0.1);
        Ship probe=new Ship(ShipParts.SHIP.get(),level);probe.configure(h,owner);
        test.assertTrue(ClearFlight.clear(level,probe,volume),"空の飛行経路を検出できない");
        level.setBlockAndUpdate(block,ShipParts.CORE.get().defaultBlockState());
        test.assertTrue(!ClearFlight.clear(level,probe,volume) && !level.noCollision(probe,volume),"障害物を素通りする");
        level.removeBlock(block,false);
        back.discard();test.succeed();
    }
}
