package io.github.python121212.astrodock.ship;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class CabinGameTest {
    private static Hull.Part part(int y,int z,Block block) {
        return new Hull.Part(new BlockPos(0,y,z),block.defaultBlockState(),2);
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void engineAndDeck(GameTestHelper test) {
        var orbit=test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        var p=new FakePlayer(orbit,new GameProfile(UUID.randomUUID(),"ADDeckTest"));
        var other=new FakePlayer(orbit,new GameProfile(UUID.randomUUID(),"ADNotPilot"));
        List<Hull.Part> parts=new ArrayList<>();
        for(int z=0;z<4;z++) {
            parts.add(part(0,z,z==0?ShipParts.CORE.get():z==1?ShipParts.ENGINE.get():Blocks.IRON_BLOCK));
            parts.add(part(3,z,Blocks.IRON_BLOCK));
        }
        parts.add(part(2,-1,ShipParts.HELM.get()));parts.add(part(2,3,ShipParts.CONSOLE.get()));
        Hull h=new Hull(parts);Ship ship=new Ship(ShipParts.SHIP.get(),orbit);
        ship.configure(h,p.getUUID());ship.setPos(210,168,210);orbit.getChunkAt(ship.blockPosition());
        orbit.addFreshEntity(ship);p.startRiding(ship,true);
        ship.command(other,ShipCommand.Action.ENGINE);test.assertTrue(ship.engineOn(),"外部から主機を操作できた");
        ship.command(p,ShipCommand.Action.ENGINE);test.assertTrue(!ship.engineOn(),"主機が止まらない");
        ship.setDeltaMovement(.25,0,0);p.setPlayerInput(1,1,true,false);ship.tick();
        test.assertTrue(Math.abs(ship.getDeltaMovement().x-.25)<1e-6 && ship.getDeltaMovement().z==0 && ship.getDeltaMovement().y<=0 && ship.body.momentum.lengthSquared()==0,"停止時の推力または慣性が不正");
        ship.command(p,ShipCommand.Action.SEAT);test.assertTrue(ship.walking(p) && p.getVehicle()==ship,"席を離れると降船した");
        float start=ship.deckFeet(p).z;p.setYRot(0);p.setXRot(0);p.setPlayerInput(0,1,false,false);
        for(int i=0;i<32;i++) ship.tick();
        var feet=ship.deckFeet(p);test.assertTrue(feet.z>start+1 && feet.z<2.71 && feet.y==1,"通路内を歩けない、または頭上の壁を通過した");
        test.assertTrue(!h.deck.canStand(1.5,1,1.5),"床のない場所にも立てる");
        ship.positionRider(p);test.assertTrue(ship.hasPanelInSight(p),"制御盤を照準できない");
        ship.command(p,ShipCommand.Action.USE_PANEL);test.assertTrue(ship.engineOn() && ship.walking(p),"艦内から再起動できない");
        ship.tick();test.assertTrue(ship.getDeltaMovement().z==0,"歩行入力が推進に流れた");
        p.setYRot(180);ship.positionRider(p);ship.command(p,ShipCommand.Action.USE_PANEL);
        test.assertTrue(!ship.walking(p),"操縦桿で復帰できない");
        ship.command(p,ShipCommand.Action.ENGINE);ship.command(p,ShipCommand.Action.SEAT);
        // 回転・並進後の足位置を、描画時と同じ重心で検証。
        ship.body.attitude.rotationY((float)Math.PI/2);ship.positionRider(p);
        var local=ship.deckFeet(p);double expectedX=ship.getX()+local.z-h.cz-.5;
        test.assertTrue(Math.abs(p.getX()-expectedX)<1e-4,"乗員が回転した床に追従しない");
        CompoundTag tag=new CompoundTag();ship.addAdditionalSaveData(tag);Ship loaded=new Ship(ShipParts.SHIP.get(),orbit);loaded.readAdditionalSaveData(tag);
        test.assertTrue(!loaded.engineOn() && loaded.walking(p) && loaded.deckFeet(p).equals(local),"セーブ復元失敗");
        ship.setPos(ship.getX(),-.1,ship.getZ());Ship next=ship.crossBoundary(orbit);
        test.assertTrue(next!=null && p.getVehicle()==next && next.walking(p) && !next.engineOn() && next.deckFeet(p).equals(local),"境界移動で乗員状態を失った");
        p.stopRiding();test.assertTrue(!next.walking(p),"降船後に歩行状態が残った");next.discard();
        com.mojang.logging.LogUtils.getLogger().info("AstroDock engine and deck verified");test.succeed();
    }
}
