package io.github.python121212.astrodock.ship;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class AirlockBoardingTest {
    private static FakePlayer player(ServerLevel l,String name){return new FakePlayer(l,new GameProfile(UUID.randomUUID(),name));}
    private static void aim(FakePlayer p,BlockPos target,int distance){
        p.setPos(target.getX()+.5,target.getY()+.5-p.getEyeHeight(),target.getZ()-distance);p.setYRot(0);p.setXRot(0);
    }
    private static void outside(GameTestHelper t,Ship ship,FakePlayer p){
        ship.interact(p,InteractionHand.MAIN_HAND);t.assertTrue(p.level()==ship.level() && !p.isPassenger(),"入口以外から搭乗した");
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void entrances(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);var owner=player(l,"ADLockOwner");
        BlockPos core=new BlockPos(1610,165,1610),door=core.east().above(),hatch=core.east(2).above();
        hatch=core.offset(3,3,2);var console=core.offset(4,1,3);var helm=core.offset(1,1,4);
        for(int x=0;x<6;x++)for(int z=0;z<6;z++)l.setBlockAndUpdate(core.offset(x,0,z),Blocks.IRON_BLOCK.defaultBlockState());
        l.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());var lower=ShipParts.AIRLOCK.get().defaultBlockState();
        l.setBlock(door,lower,2|16|32);l.setBlock(door.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2|16|32);
        l.setBlockAndUpdate(hatch,ShipParts.HATCH.get().defaultBlockState());l.setBlockAndUpdate(console,ShipParts.CONSOLE.get().defaultBlockState());
        l.setBlockAndUpdate(helm,ShipParts.HELM.get().defaultBlockState());l.setBlockAndUpdate(core.east(5),ShipParts.ENGINE.get().defaultBlockState());
        var tag=new CompoundTag();tag.putLong("a",core.asLong());tag.putLong("b",core.offset(5,3,5).asLong());tag.putLong("core",core.asLong());tag.putBoolean("all",true);
        t.assertTrue(ShipWrench.assemble(l,owner,tag),"試験船を組み立てられない");var ship=(Ship)owner.getVehicle();owner.stopRiding();
        var origin=ship.position();var r=CabinRooms.data(l).get(ship.roomId);var c=CabinRooms.level(l);var cd=r.core.offset(door.subtract(core));var roof=hatch;
        CabinRooms.follow(l,ship.chunkPosition());
        t.startSequence().thenWaitUntil(()->t.assertTrue(l.getEntity(ship.getUUID())==ship,"外装読み込み中")).thenExecute(()->{
            ship.setPos(origin);ship.setDeltaMovement(Vec3.ZERO);var guest=player(l,"ADLockGuest");
            aim(owner,core,2);outside(t,ship,owner);aim(guest,core,2);outside(t,ship,guest);
            aim(guest,door,7);outside(t,ship,guest);aim(guest,door,2);guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));outside(t,ship,guest);
            ship.interact(guest,InteractionHand.OFF_HAND);guest.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            l.setBlockAndUpdate(door.north(),Blocks.STONE.defaultBlockState());outside(t,ship,guest);l.removeBlock(door.north(),false);
            t.assertTrue(!c.getBlockState(cd).getValue(DoorBlock.OPEN),"遠距離・壁越し・アイテム所持で扉が開いた");
            var threshold=Vec3.atBottomCenterOf(door);walk(guest,threshold.add(0,0,-.35),threshold.add(0,0,.1));
            t.assertTrue(guest.level()==l,"閉じた扉を通過");
            aim(guest,door,2);outside(t,ship,guest);
            t.assertTrue(c.getBlockState(cd).getValue(DoorBlock.OPEN) && ship.visibleState(ship.hull().doors.stream().filter(v->v.pos().equals(door.subtract(core))).findFirst().orElseThrow()).getValue(DoorBlock.OPEN),"実ドアと外装の開状態が不一致");
            c.setBlockAndUpdate(cd.south(),Blocks.IRON_BLOCK.defaultBlockState());walk(guest,threshold.add(0,0,-.35),threshold.add(0,0,.1));
            t.assertTrue(guest.level()==l,"到着位置が塞がれても入室");c.removeBlock(cd.south(),false);
            walk(guest,threshold.add(0,0,-.35),threshold.add(0,0,.1));
            t.assertTrue(guest.level()==c && !guest.isPassenger() && r.visitors.contains(guest.getUUID()),"開いた扉を歩いて入れない");
            var center=Vec3.atBottomCenterOf(cd);guest.setPos(center.add(0,0,3));CabinRooms.operate(guest,r.core.offset(helm.subtract(core)));t.assertTrue(!guest.isPassenger(),"乗員が操縦席を取得");
            walk(guest,center.add(0,0,.35),center.add(0,0,-.1));t.assertTrue(guest.level()==l && !guest.isPassenger() && !r.visitors.contains(guest.getUUID()),"扉から徒歩で降船できない");
            guest.setPos(threshold.add(0,0,-3));
            // ハッチは水平に走り抜けても入れない。開けて上から降りる。
            var top=Vec3.atCenterOf(roof);guest.setPos(top.add(0,2,0));guest.setXRot(90);ship.interact(guest,InteractionHand.MAIN_HAND);
            for(int i=0;i<3;i++){
                var mate=i==0?guest:player(l,"ADLock"+i);walk(mate,top.add(0,.4,0),top.add(0,-.2,0));
                t.assertTrue(mate.level()==c && !mate.isPassenger(),"ハッチから降りられない: "+i);mate.setPos(center.add(i,0,3));
            }
            var extra=player(l,"ADLockExtra");walk(extra,threshold.add(0,0,-.35),threshold.add(0,0,.1));t.assertTrue(extra.level()==l,"定員を超えて入室");
            walk(owner,threshold.add(0,0,-.35),threshold.add(0,0,.1));t.assertTrue(owner.level()==c && !owner.isPassenger(),"所有者枠で入れない");
            var panel=r.core.offset(console.subtract(core));owner.setPos(Vec3.atCenterOf(panel.north()));CabinRooms.operate(owner,panel);t.assertTrue(!ship.engineOn(),"機関室で主機を停止できない");
            var seat=r.core.offset(helm.subtract(core));owner.setPos(Vec3.atCenterOf(seat.north()));CabinRooms.operate(owner,seat);t.assertTrue(owner.getVehicle()==ship && owner.position().distanceTo(Vec3.atCenterOf(helm))<.02,"船室の操縦桿で着席できない");
            com.mojang.logging.LogUtils.getLogger().info("AstroDock airlock boarding verified");
        }).thenSucceed();
    }
    private static void walk(FakePlayer p,Vec3 from,Vec3 to){p.setPos(from);Walkway.step(p);p.setPos(to);Walkway.step(p);}
}
