package io.github.python121212.astrodock.ship;

import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class ArmamentGameTest {
    private static Cow target(ServerLevel l,Vec3 at) {
        Cow cow=EntityType.COW.create(l);cow.setNoAi(true);cow.setPos(at);l.addFreshEntity(cow);return cow;
    }
    private static void flight(ShipShot shot,int ticks) {for(int i=0;i<ticks && !shot.isRemoved();i++)shot.tick();}
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void orbitalArmament(GameTestHelper t) {
        ServerLevel l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);
        BlockPos base=new BlockPos(644,185,644);l.getChunkAt(base);l.getChunkAt(base.south(16));l.getChunkAt(base.south(32));l.getChunkAt(base.south(48));
        FakePlayer p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"ADGunner")),guest=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"ADNotGunner"));
        Ship s=new Ship(ShipParts.SHIP.get(),l);s.configure(new Hull(List.of(
                new Hull.Part(BlockPos.ZERO,ShipParts.CANNON.get().defaultBlockState(),1),
                new Hull.Part(new BlockPos(2,0,0),ShipParts.MISSILES.get().defaultBlockState(),1),
                new Hull.Part(new BlockPos(4,0,0),ShipParts.TORPEDOES.get().defaultBlockState(),1))),p.getUUID());
        s.setPos(Vec3.atBottomCenterOf(base.east(2)));l.addFreshEntity(s);p.startRiding(s,true);guest.startRiding(s,true);s.positionRider(p);p.setYRot(0);p.setXRot(0);
        var area=s.getBoundingBox().inflate(15);p.getAbilities().instabuild=false;
        var iron=new ItemStack(Items.IRON_NUGGET,2);var rocket=new ItemStack(Items.FIREWORK_ROCKET);var crystal=new ItemStack(Items.PRISMARINE_CRYSTALS,2);
        p.getInventory().items.set(0,iron);p.getInventory().items.set(1,rocket);guest.getInventory().items.set(0,crystal.copy());
        s.command(guest,ShipCommand.Action.TORPEDO);s.command(p,ShipCommand.Action.TORPEDO);
        s.command(p,ShipCommand.Action.SEAT);s.command(p,ShipCommand.Action.CANNON);s.command(p,ShipCommand.Action.SEAT);
        t.assertTrue(l.getEntitiesOfClass(ShipShot.class,area).isEmpty() && iron.getCount()==2,"権限・残弾チェックに失敗");
        p.getInventory().items.set(2,crystal);p.setYRot(90);s.command(p,ShipCommand.Action.TORPEDO);p.setYRot(0);
        t.assertTrue(crystal.getCount()==2,"射線を塞いだ自艦を貫通");
        for(ShipWeapon w:ShipWeapon.values()) {
            Cow cow=target(l,w==ShipWeapon.MISSILE?p.getEyePosition().add(0,-.5,12):Vec3.atBottomCenterOf(base.east(w==ShipWeapon.CANNON?0:4).south(9)));
            float hp=cow.getHealth();var action=ShipCommand.Action.valueOf(w.name());s.command(p,action);s.command(p,action);
            var shots=l.getEntitiesOfClass(ShipShot.class,area,e->e.getType()==w.type.get());t.assertTrue(shots.size()==1,"重複または未発射: "+w);
            if(w==ShipWeapon.MISSILE)cow.setPos(cow.position().add(2,0,0)); // 発射時に照準した相手だけを追尾
            flight(shots.get(0),26);t.assertTrue(shots.get(0).isRemoved() && cow.getHealth()<hp,"宇宙で命中しない: "+w);cow.discard();
        }
        t.assertTrue(iron.getCount()==1 && rocket.isEmpty() && crystal.getCount()==1,"1発ごとの消費量が不正");
        // 水中から発射し、壁で停止。地形や遮蔽裏の Mob に影響しない。
        var start=base.east(4);var wall=start.south(4);l.setBlock(wall,Blocks.STONE.defaultBlockState(),2);l.setBlock(start.south(),Blocks.WATER.defaultBlockState(),2);
        Cow cover=target(l,Vec3.atBottomCenterOf(wall.south(2)));float hp=cover.getHealth();
        var waterShot=new ShipShot(ShipParts.TORPEDO.get(),l);waterShot.launch(s,p,Vec3.atCenterOf(start.south()),new Vec3(0,0,1));l.addFreshEntity(waterShot);flight(waterShot,10);
        t.assertTrue(waterShot.isRemoved() && cover.getHealth()==hp && l.getBlockState(wall).is(Blocks.STONE),"魚雷が遮蔽を破壊・貫通した");
        l.removeBlock(wall,false);l.removeBlock(start.south(),false);cover.discard();guest.stopRiding();p.stopRiding();s.discard();
        com.mojang.logging.LogUtils.getLogger().info("AstroDock orbital armament verified");t.succeed();
    }
}
