package io.github.python121212.astrodock.ship;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class CoreMenuTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top",timeoutTicks=160)
    public static void shellFirst(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);BlockPos lo=new BlockPos(1302,190,1302),hi=lo.offset(4,3,4),core=lo.offset(2,1,0);
        var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"ADShipwright"));p.setPos(Vec3.atCenterOf(core.north(2)));p.getAbilities().mayBuild=true;
        for(var n:BlockPos.betweenClosed(lo,hi))if(n.getX()==lo.getX()||n.getX()==hi.getX()||n.getY()==lo.getY()||n.getY()==hi.getY()||n.getZ()==lo.getZ()||n.getZ()==hi.getZ())l.setBlock(n,Blocks.IRON_BLOCK.defaultBlockState(),2);
        var stand=lo.below();l.setBlockAndUpdate(stand,Blocks.GOLD_BLOCK.defaultBlockState());l.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());
        var menu=new CoreMenu(12,p.getInventory(),core);t.assertTrue(!menu.clickMenuButton(p,1),"確認前に組立");menu.clickMenuButton(p,0);
        t.assertTrue(menu.value(0)==1 && menu.value(1)==82 && menu.value(2)==18 && !p.isPassenger(),"検出範囲・プレビューが不正");
        var gap=lo.offset(2,3,2);l.removeBlock(gap,false);menu.clickMenuButton(p,1);
        t.assertTrue(menu.value(0)==3 && l.getBlockState(core).is(ShipParts.CORE.get()) && ShellSurvey.scan(l,core)==null,"穴または確認中の変更を無視");
        l.setBlockAndUpdate(gap,Blocks.IRON_BLOCK.defaultBlockState());menu=new CoreMenu(13,p.getInventory(),core);menu.clickMenuButton(p,0);menu.clickMenuButton(p,1);
        t.assertTrue(p.getVehicle() instanceof Ship && l.getBlockState(stand).is(Blocks.GOLD_BLOCK),"船化失敗または地面を巻き込み");var s=(Ship)p.getVehicle();var pos=s.position();
        p.setPlayerInput(0,1,true,false);s.tick();t.assertTrue(pos.equals(s.position()) && !s.engineOn(),"未艤装の船が動いた");
        s.command(p,ShipCommand.Action.CABIN);var c=CabinRooms.level(l);var room=CabinRooms.data(l).get(s.roomId);t.assertTrue(p.level()==c,"作業船室に入れない");
        var work=new CoreMenu(14,p.getInventory(),room.core);p.setPos(Vec3.atCenterOf(room.core.south()));
        var outsider=new FakePlayer(c,new GameProfile(UUID.randomUUID(),"ADVisitor"));outsider.setPos(p.position());work.clickMenuButton(outsider,2);
        t.assertTrue(work.value(0)==5 && !s.isRemoved(),"非乗員が設備更新");
        var plant=room.core.south();c.setBlockAndUpdate(plant,ShipParts.PLANT.get().defaultBlockState());c.setBlockAndUpdate(plant.east(),ShipParts.DRIVE.get().defaultBlockState());
        work=new CoreMenu(15,p.getInventory(),room.core);work.clickMenuButton(p,2);t.assertTrue(work.value(0)==8 && !s.isRemoved(),"不完全な機関で旧船を破棄");
        c.setBlockAndUpdate(plant.east().south(),ShipParts.GENERATOR.get().defaultBlockState());c.setBlockAndUpdate(plant.west(),ShipParts.ENGINE.get().defaultBlockState());
        var cargo=plant.south();c.setBlockAndUpdate(cargo,Blocks.CHEST.defaultBlockState());((Container)c.getBlockEntity(cargo)).setItem(0,new ItemStack(Items.DIAMOND,3));
        work=new CoreMenu(16,p.getInventory(),room.core);work.clickMenuButton(p,2);
        t.assertTrue(work.value(0)==10 && s.isRemoved() && p.level()==c,"船内から設備更新できない: "+work.value(0));
        var live=(Ship)l.getEntity(room.shipId);t.assertTrue(live.roomId==room.id && live.hull().engines==1 && live.output()==3 && ((Container)c.getBlockEntity(cargo)).getItem(0).getCount()==3,"出力・積荷を保持していない");
        CabinRooms.leave(p);p.setPlayerInput(0,1,true,false);live.tick();t.assertTrue(p.getVehicle()==live && live.getDeltaMovement().y>0,"改装後に飛行できない");
        var h=live.hull();live.moveTo(core.getX()+.5+h.cx,core.getY()+h.floor,core.getZ()+.5+h.cz,0,0);live.setDeltaMovement(Vec3.ZERO);live.dock(p);
        t.assertTrue(live.isRemoved() && ((Container)l.getBlockEntity(core.south(2))).getItem(0).getCount()==3,"改装後のドック失敗");
        for(var n:BlockPos.betweenClosed(lo,hi))CabinRooms.erase(l,n);l.removeBlock(stand,false);
        com.mojang.logging.LogUtils.getLogger().info("AstroDock shell first verified");t.succeed();
    }
}
