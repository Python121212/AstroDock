package io.github.python121212.astrodock.ship;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import org.joml.Vector3f;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class CrewGameTest {
    private static Hull.Part tile(int x,int y,int z) {
        return new Hull.Part(new BlockPos(x,y,z),Blocks.IRON_BLOCK.defaultBlockState(),1);
    }
    private static Vector3f traverse(CabinDeck deck,Vector3f start,int direction,int ticks) {
        Vec3 south=new Vec3(0,0,direction);
        for(int i=0;i<ticks;i++) start=deck.advance(start,south,1,0);
        return start;
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void deckSteps(GameTestHelper test) {
        var start=new Vector3f(.5f,1,.5f);var parts=new ArrayList<>(List.of(tile(0,0,0),tile(0,1,1),tile(0,1,2)));
        CabinDeck stairs=new CabinDeck(parts);
        Vector3f up=traverse(stairs,start,1,10),down=traverse(stairs,up,-1,10);
        test.assertTrue(up.y==2 && up.z>1.5 && down.y==1 && down.distance(start)<.01,"1段の往復に失敗");
        parts.add(tile(0,3,0));
        test.assertTrue(traverse(new CabinDeck(parts),start,1,10).z<1,"上昇途中で頭が梁を通過した");
        parts.remove(parts.size()-1);parts.add(tile(0,2,1));
        test.assertTrue(traverse(new CabinDeck(parts),start,1,10).z<1,"2段壁を越えた");
        CabinDeck cliff=new CabinDeck(List.of(tile(0,2,0),tile(0,0,1)));
        test.assertTrue(traverse(cliff,new Vector3f(.5f,3,.5f),1,20).y==3,"深い落差へ降りた");
        test.succeed();
    }
    private static FakePlayer player(ServerLevel level,String name) {
        return new FakePlayer(level,new GameProfile(UUID.randomUUID(),name));
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top",timeoutTicks=400)
    public static void groupFlight(GameTestHelper test) {
        var orbit=test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        var captain=player(orbit,"ADCaptain");var a=player(orbit,"ADCrewA");var b=player(orbit,"ADCrewB");var c=player(orbit,"ADCrewC");var extra=player(orbit,"ADCrewD");
        var parts=new ArrayList<Hull.Part>();
        for(int z=0;z<6;z++) for(int x=0;x<3;x++) parts.add(tile(x,z>1?1:0,z));
        parts.set(0,new Hull.Part(BlockPos.ZERO,ShipParts.ENGINE.get().defaultBlockState(),1));
        var ship=new Ship(ShipParts.SHIP.get(),orbit);ship.configure(new Hull(parts),captain.getUUID());
        ship.setPos(290,128,290);orbit.getChunkAt(ship.blockPosition());orbit.addFreshEntity(ship);
        for(var p:List.of(a,b,c,extra,captain)) ship.interact(p,InteractionHand.MAIN_HAND);
        test.assertTrue(ship.getFirstPassenger()==a && captain.getVehicle()==ship && ship.getPassengers().size()==4 && !extra.isPassenger(),"操縦席の予約または定員が不正");
        Vector3f feetB=ship.deckFeet(b),feetA=ship.deckFeet(a);
        test.assertTrue(!feetA.equals(feetB),"乗り口が重なった");
        a.setPlayerInput(0,1,true,false);a.setYRot(0);a.setXRot(0);
        for(int i=0;i<14;i++) ship.tick();
        test.assertTrue(ship.deckFeet(a).y==2 && ship.deckFeet(b).equals(feetB) && ship.getDeltaMovement().z==0 && ship.getDeltaMovement().y<0,"乗員の入力が分離されていない");
        ship.command(a,ShipCommand.Action.SEAT);ship.command(a,ShipCommand.Action.ENGINE);
        test.assertTrue(ship.walking(a) && ship.engineOn(),"乗員が操縦権を取得した");
        ship.dock(captain);test.assertTrue(!ship.isRemoved(),"乗員を残して再設置した");
        captain.setPlayerInput(0,0,true,false);ship.tick();test.assertTrue(ship.getDeltaMovement().y>0,"先頭でない所有者が操縦できない");
        b.stopRiding();test.assertTrue(ship.walking(a) && !ship.walking(b),"他の乗員の席も消えた");
        ship.interact(extra,InteractionHand.MAIN_HAND);test.assertTrue(extra.getVehicle()==ship,"空席が再利用されない");
        ship.command(captain,ShipCommand.Action.SEAT);
        var riders=List.of(captain,a,c,extra);var positions=riders.stream().map(ship::deckFeet).toList();
        var tag=new CompoundTag();ship.addAdditionalSaveData(tag);var restored=new Ship(ShipParts.SHIP.get(),orbit);restored.readAdditionalSaveData(tag);
        for(int i=0;i<4;i++) test.assertTrue(restored.deckFeet(riders.get(i)).equals(positions.get(i)),"乗員別の保存位置が不一致");
        Vec3 entry=PlanetTravel.CENTER.add(0,0,PlanetTravel.ENTRY-1);ship.setPos(entry.x,entry.y,entry.z);
        Ship surface=ship.crossBoundary(orbit);test.assertTrue(surface!=null,"惑星へ転送されない");
        for(int i=0;i<4;i++) test.assertTrue(riders.get(i).getVehicle()==surface && surface.deckFeet(riders.get(i)).equals(positions.get(i)),"ルブラで乗員が分離");
        for(var p:riders) p.setPlayerInput(0,0,false,false);
        test.succeedWhen(()->{
            Vec3 launch=new Vec3(surface.getX(),PlanetTravel.DEPARTURE_Y,surface.getZ());
            test.assertTrue(OrbitArrival.ready(orbit,PlanetTravel.at(AstroDock.RUBRA,launch).position()),"宇宙側の準備中");
            surface.setPos(launch.x,launch.y,launch.z);Ship back=surface.crossBoundary((ServerLevel)surface.level());
            test.assertTrue(back!=null && back.level()==orbit,"宇宙へ戻れない");
            for(int i=0;i<4;i++) test.assertTrue(riders.get(i).getVehicle()==back && back.deckFeet(riders.get(i)).equals(positions.get(i)),"帰還後の乗員位置が不一致");
            for(var p:riders) p.stopRiding();back.discard();com.mojang.logging.LogUtils.getLogger().info("AstroDock four crew verified");
        });
    }
}
