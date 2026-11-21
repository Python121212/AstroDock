package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.AstroDock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class ShipGameTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void shipRoundTrip(GameTestHelper test) {
        var orbit=test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        var pilot=FakePlayerFactory.getMinecraft(orbit);
        BlockPos core=new BlockPos(180,64,180),engine=core.east(),excluded=core.east(2);
        orbit.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());
        orbit.setBlockAndUpdate(engine,ShipParts.ENGINE.get().defaultBlockState());
        orbit.setBlockAndUpdate(excluded,Blocks.CHEST.defaultBlockState());
        CompoundTag t=new CompoundTag(); t.putLong("a",excluded.asLong());t.putLong("b",core.asLong());t.putLong("core",core.asLong());t.putBoolean("all",true);
        test.assertTrue(!ShipWrench.assemble(orbit,pilot,t) && orbit.getBlockState(core).is(ShipParts.CORE.get()),"チェストを含む船は無変更で拒否する");
        t.putLongArray("toggle",new long[]{excluded.asLong()});
        test.assertTrue(ShipWrench.assemble(orbit,pilot,t),"組み立て失敗");
        Ship ship=(Ship)pilot.getVehicle(); Hull hull=ship.hull();
        test.assertTrue(hull.parts.size()==2 && orbit.isEmptyBlock(core) && orbit.getBlockState(excluded).is(Blocks.CHEST),"除外または搭乗に失敗");
        RigidBody drift=new RigidBody();
        var accelerate=drift.step(hull,new Vec3(1,0,0),5000,1,0,0,0);
        test.assertTrue(drift.momentum.y<0 && accelerate.z>0 && accelerate.x==1,"偏心推力と真空慣性");
        Hull balanced=new Hull(List.of(hull.parts.get(0),new Hull.Part(new BlockPos(1,0,0),ShipParts.ENGINE.get().defaultBlockState(),2),new Hull.Part(new BlockPos(-1,0,0),ShipParts.ENGINE.get().defaultBlockState(),2)));
        RigidBody straight=new RigidBody();straight.step(balanced,Vec3.ZERO,320,1,0,0,0);
        test.assertTrue(straight.momentum.lengthSquared()==0,"対称配置で回転が発生");
        orbit.setBlockAndUpdate(engine,Blocks.OBSIDIAN.defaultBlockState());ship.dock(pilot);
        test.assertTrue(!ship.isRemoved() && orbit.isEmptyBlock(core),"着艦失敗時に部分設置された");
        orbit.removeBlock(engine,false);ship.dock(pilot);
        test.assertTrue(ship.isRemoved() && orbit.getBlockState(core).is(ShipParts.CORE.get()) && orbit.getBlockState(engine).is(ShipParts.ENGINE.get()),"着艦で復元できません");
        orbit.removeBlock(core,false);orbit.removeBlock(engine,false);orbit.removeBlock(excluded,false);
        com.mojang.logging.LogUtils.getLogger().info("AstroDock ship round trip verified");test.succeed();
    }
}
