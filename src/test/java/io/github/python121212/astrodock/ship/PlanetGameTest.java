package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class PlanetGameTest {
    @GameTest(templateNamespace="minecraft", template="igloo/top")
    public static void descendAndLaunch(GameTestHelper test) {
        var server=test.getLevel().getServer();var orbit=server.getLevel(AstroDock.ORBIT);var rubra=server.getLevel(AstroDock.MARS);
        test.assertTrue(rubra!=null,"惑星ディメンション未登録");
        Ship ship=new Ship(ShipParts.SHIP.get(),orbit);
        Hull h=new Hull(List.of(new Hull.Part(BlockPos.ZERO,ShipParts.CORE.get().defaultBlockState(),4),
                new Hull.Part(BlockPos.ZERO.above(),ShipParts.ENGINE.get().defaultBlockState(),3)));
        ship.body.attitude.rotationX((float)Math.PI/2);ship.body.momentum.set(0.01f,0.02f,0.03f);
        ship.configure(h,UUID.randomUUID());ship.setPos(0,100,0);
        int before=rubra.getChunkSource().getLoadedChunksCount();
        for(int i=0;i<2048;i++) test.assertTrue(ship.crossBoundary(orbit)==null,"遠方で転送が発生");
        test.assertTrue(rubra.getChunkSource().getLoadedChunksCount()==before,"遠方で惑星チャンクが読まれた");
        Vec3 normal=new Vec3(0,0,1), start=PlanetTravel.CENTER.add(normal.scale(PlanetTravel.ENTRY));
        ship.setPos(start.x,start.y,start.z);ship.setDeltaMovement(normal.scale(-1.25));
        Ship land=ship.crossBoundary(orbit);
        test.assertTrue(land!=null && land.level()==rubra && ship.isRemoved(),"大気圏に突入できない");
        test.assertTrue(land.getY()==PlanetTravel.ARRIVAL_Y && land.getDeltaMovement().distanceTo(new Vec3(0,-1.25,0))<1e-5,"進入速度が降下方向に変換されない");
        test.assertTrue(Math.abs(land.body.attitude.w)>0.99999 && land.crossBoundary(rubra)==null,"姿勢または再転送防止に失敗");
        int top=rubra.getHeight(Heightmap.Types.MOTION_BLOCKING,0,0);
        test.assertTrue(top>0 && top<PlanetTravel.ARRIVAL_Y && !rubra.isEmptyBlock(new BlockPos(0,top-1,0)),"着陸可能な地形がない");
        for(int i=0;i<1200 && !land.onGround();i++) land.tick();
        test.assertTrue(land.onGround(),"船が地表で停止しない");
        BlockPos marker=new BlockPos(4,rubra.getHeight(Heightmap.Types.MOTION_BLOCKING,4,4),4);
        rubra.setBlockAndUpdate(marker,Blocks.IRON_BLOCK.defaultBlockState());
        // 別の緯度・経度へ飛んで離脱し、同じ位置へ再突入。姿勢・慣性と建築を確認。
        land.setPos(1300,PlanetTravel.DEPARTURE_Y,-900);land.setDeltaMovement(new Vec3(0.3,1,0.2));
        land.body.attitude.rotationXYZ(0.1f,0.2f,-0.3f);land.body.momentum.set(0.1f,-0.01f,0.07f);
        Quaternionf attitude=new Quaternionf(land.body.attitude);Vector3f momentum=new Vector3f(land.body.momentum);
        Ship launched=land.crossBoundary(rubra);
        test.assertTrue(launched!=null && launched.level()==orbit && launched.crossBoundary(orbit)==null,"離脱地点が不正");
        Vec3 direction=launched.position().subtract(PlanetTravel.CENTER).normalize();Vec3 approach=PlanetTravel.CENTER.add(direction.scale(PlanetTravel.ENTRY-1));
        launched.setPos(approach.x,approach.y,approach.z);Ship again=launched.crossBoundary(orbit);
        test.assertTrue(again!=null && Math.abs(again.getX()-1300)<0.001 && Math.abs(again.getZ()+900)<0.001,"地表座標が往復でずれる");
        test.assertTrue(Math.abs(again.body.attitude.dot(attitude))>0.99999 && again.body.momentum.distance(momentum)<1e-5 && again.getDeltaMovement().distanceTo(new Vec3(0.3,1,0.2))<1e-5,"慣性が保存されない");
        test.assertTrue(rubra.getBlockState(marker).is(Blocks.IRON_BLOCK),"建築物が残らない");
        rubra.removeBlock(marker,false);again.discard();
        test.assertTrue(PlanetTravel.at(Level.OVERWORLD,start)==null,"地球で惑星転送が発生");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock planet landing verified");test.succeed();
    }
}
