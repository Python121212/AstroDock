package io.github.python121212.astrodock.ship;

import com.mojang.authlib.GameProfile;
import java.util.*;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class WorkingCabinTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void doors(GameTestHelper t) {
        BlockPos lo=BlockPos.ZERO,hi=new BlockPos(4,4,4),inside=new BlockPos(2,1,2),wall=new BlockPos(2,1,0);
        var cells=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var p:BlockPos.betweenClosed(lo,hi)) if(p.getX()==0||p.getY()==0||p.getZ()==0||p.getX()==4||p.getY()==4||p.getZ()==4)cells.put(p.immutable(),Blocks.IRON_BLOCK.defaultBlockState());
        for(Block b:List.of(Blocks.OAK_DOOR,Blocks.IRON_TRAPDOOR)) {
            cells.put(inside,b.defaultBlockState());cells.put(wall,ShipParts.AIRLOCK.get().defaultBlockState());
            t.assertTrue(HullDoors.exposed(cells,lo,hi)==null,"内装ドアを拒否");cells.put(wall,b.defaultBlockState());
            t.assertTrue(wall.equals(HullDoors.exposed(cells,lo,hi)),"外壁の通常ドアを許可");cells.remove(wall);
            t.assertTrue(inside.equals(HullDoors.exposed(cells,lo,hi)),"凹んだ外壁を検出できない");
        }t.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top",timeoutTicks=550)
    public static void workingCabin(GameTestHelper t) {
        var orbit=t.getLevel().getServer().getLevel(AstroDock.ORBIT);var cabin=CabinRooms.level(orbit);
        var p=new FakePlayer(orbit,new GameProfile(UUID.randomUUID(),"ADTechnician"));
        BlockPos core=new BlockPos(448,110,448),chest=core.east(2),furnace=core.east(3),hatch=core.east(4);
        orbit.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());orbit.setBlockAndUpdate(core.east(),ShipParts.ENGINE.get().defaultBlockState());
        orbit.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());orbit.setBlockAndUpdate(furnace,Blocks.FURNACE.defaultBlockState());
        ((Container)orbit.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,5));
        ((Container)orbit.getBlockEntity(furnace)).setItem(0,new ItemStack(Items.RAW_IRON));((Container)orbit.getBlockEntity(furnace)).setItem(1,new ItemStack(Items.COAL));
        orbit.setBlockAndUpdate(hatch,Blocks.IRON_TRAPDOOR.defaultBlockState());
        var tag=new CompoundTag();tag.putLong("core",core.asLong());tag.putLong("a",core.asLong());tag.putLong("b",hatch.above().asLong());tag.putBoolean("all",true);
        t.assertTrue(!ShipWrench.assemble(orbit,p,tag) && ((Container)orbit.getBlockEntity(chest)).getItem(0).getCount()==5,"外壁検証で積荷が消失");
        orbit.setBlockAndUpdate(hatch,ShipParts.HATCH.get().defaultBlockState());t.assertTrue(ShipWrench.assemble(orbit,p,tag),"機械搭載船を組めない");
        Ship ship=(Ship)p.getVehicle();var d=CabinRooms.data(orbit);var r=d.get(ship.roomId);
        ship.command(p,ShipCommand.Action.CABIN);t.assertTrue(p.level()==cabin && !p.isPassenger() && r.visitors.contains(p.getUUID()),"作業船室への入室失敗");
        BlockPos port=r.core.east(4);var state=cabin.getBlockState(port);
        state.use(cabin,p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(port),net.minecraft.core.Direction.UP,port,false));
        t.assertTrue(cabin.getBlockState(port).getValue(TrapDoorBlock.OPEN),"艦外ハッチが手で開かない");
        var cargo=(Container)cabin.getBlockEntity(r.core.east(2));t.assertTrue(cargo.getItem(0).getCount()==5,"積荷の数が不正");cargo.removeItem(0,2);
        cabin.setBlockAndUpdate(r.core.east().above(),Blocks.CRAFTING_TABLE.defaultBlockState());
        ship.setPos(ship.getX(),-.1,ship.getZ());var next=ship.crossBoundary(orbit);
        t.assertTrue(next!=null && next.roomId==r.id && next.getUUID().equals(r.shipId),"境界移動で船室が分離");
        var db=CabinRooms.load(d.save(new CompoundTag()));t.assertTrue(db.get(r.id).visitors.contains(p.getUUID()) && db.get(r.id).shipId.equals(next.getUUID()),"船室の保存に失敗");
        // 精錬はテストから ticker を呼ばず、実際のワールド更新に任せる。
        t.startSequence().thenWaitUntil(()->t.assertTrue(((Container)cabin.getBlockEntity(r.core.east(3))).getItem(2).is(Items.IRON_INGOT),"船室のかまどが稼働していない")).thenExecute(()->{
            CabinRooms.leave(p);t.assertTrue(p.getVehicle()==next && p.level()==next.level(),"転送先の船に帰れない: dim="+p.level().dimension()+", vehicle="+p.getVehicle()+", shell="+next.getRemovalReason()+", lookup="+((net.minecraft.server.level.ServerLevel)next.level()).getEntity(r.shipId));
            next.moveTo(core.getX()+.5+next.hull().cx,core.getY(),core.getZ()+.5+next.hull().cz,0,0);next.setDeltaMovement(Vec3.ZERO);
            var dest=p.serverLevel();dest.getChunkAt(core);for(var pos:BlockPos.betweenClosed(core,hatch.above()))dest.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            next.dock(p);t.assertTrue(next.isRemoved() && d.get(r.id)==null,"再設置できない");
            t.assertTrue(((Container)dest.getBlockEntity(chest)).getItem(0).getCount()==3 && ((Container)dest.getBlockEntity(furnace)).getItem(2).is(Items.IRON_INGOT) && dest.getBlockState(core.east().above()).is(Blocks.CRAFTING_TABLE),"編集・精錬結果の復元失敗");
            t.assertTrue(cabin.isEmptyBlock(r.core.east(2)),"船室に積荷の複製が残った");
            com.mojang.logging.LogUtils.getLogger().info("AstroDock working cabin verified");
        }).thenSucceed();
    }
}
