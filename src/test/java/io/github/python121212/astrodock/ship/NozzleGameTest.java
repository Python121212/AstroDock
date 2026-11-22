package io.github.python121212.astrodock.ship;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class NozzleGameTest {
    @GameTest(templateNamespace="minecraft", template="igloo/top")
    public static void customNozzleThrust(GameTestHelper test) {
        var level = test.getLevel();
        test.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("astrodock", "nozzle_casing")).isPresent(), "ノズル外殻のレシピがない");

        var thruster = ShipParts.ENGINE.get().defaultBlockState();
        var nozzle = ShipParts.NOZZLE_CASING.get().defaultBlockState();
        var iron = Blocks.IRON_BLOCK.defaultBlockState();
        var bare = new Hull(List.of(
                new Hull.Part(BlockPos.ZERO, thruster, 4),
                new Hull.Part(BlockPos.ZERO.east(), iron, 4),
                new Hull.Part(BlockPos.ZERO.west(), iron, 4)
        ));
        var blocked = new Hull(List.of(
                new Hull.Part(BlockPos.ZERO, thruster, 4),
                new Hull.Part(BlockPos.ZERO.south(), nozzle, 4),
                new Hull.Part(BlockPos.ZERO.north(), nozzle, 4)
        ));
        var flared = new Hull(List.of(
                new Hull.Part(BlockPos.ZERO, thruster, 4),
                new Hull.Part(BlockPos.ZERO.east(), nozzle, 4),
                new Hull.Part(BlockPos.ZERO.east().south(), nozzle, 4)
        ));
        test.assertTrue(blocked.effectiveEngines < bare.effectiveEngines, "排気口を塞いだノズルで推力が落ちていない");
        test.assertTrue(flared.effectiveEngines > bare.effectiveEngines, "後方に広がるノズルで推力が上がっていない");

        var b1 = new RigidBody(); var v1 = Vec3.ZERO;
        var b2 = new RigidBody(); var v2 = Vec3.ZERO;
        var b3 = new RigidBody(); var v3 = Vec3.ZERO;
        for (int i = 0; i < 10; i++) {
            v1 = b1.step(bare, v1, 5000, 1.0f, 0, 0, 0);
            v2 = b2.step(flared, v2, 5000, 1.0f, 0, 0, 0);
            v3 = b3.step(blocked, v3, 5000, 1.0f, 0, 0, 0);
        }
        test.assertTrue(v2.length() > v1.length() && v1.length() > v3.length(), "ノズル形状による速度差が不正");

        var sleek = new Hull(List.of(
                new Hull.Part(new BlockPos(0, 0, 0), thruster, 4),
                new Hull.Part(new BlockPos(0, 0, 1), iron, 4),
                new Hull.Part(new BlockPos(0, 0, 2), iron, 4),
                new Hull.Part(new BlockPos(0, 0, 3), iron, 4),
                new Hull.Part(new BlockPos(1, 0, 2), iron, 4), new Hull.Part(new BlockPos(1, 0, 3), iron, 4)
        ));
        test.assertTrue(sleek.streamline < bare.streamline && sleek.ricochetChance > 0, "船体形状の流線型・跳弾判定が不正");

        List<Hull.Part> sealedParts = new java.util.ArrayList<>();
        List<Hull.Part> holedParts = new java.util.ArrayList<>();
        for (int x = 0; x <= 2; x++) for (int y = 0; y <= 2; y++) for (int z = 0; z <= 2; z++) {
            if (x == 1 && y == 1 && z == 1) continue;
            var pos = new BlockPos(x, y, z);
            sealedParts.add(new Hull.Part(pos, iron, 4));
            if (!(x == 1 && y == 2 && z == 1)) holedParts.add(new Hull.Part(pos, iron, 4));
        }
        var sealedHull = new Hull(sealedParts);
        var holedHull = new Hull(holedParts);
        test.assertTrue(sealedHull.watertight && !holedHull.watertight, "船体の水密・浸水判定が不正");
        test.assertTrue(sealedHull.displacementCapacity > sealedHull.mass && holedHull.displacementCapacity == 0, "排水量計算が不正");

        var windCalm = io.github.python121212.astrodock.SolarActivity.solarWind(5000, 600, 100);
        var windFlare = io.github.python121212.astrodock.SolarActivity.solarWind(5000, 100, 100);
        test.assertTrue(windFlare.length() > windCalm.length(), "プロミネンス時の太陽風強化が不正");
        test.assertTrue(io.github.python121212.astrodock.SolarActivity.coronaHeat(6000, 100) > io.github.python121212.astrodock.SolarActivity.coronaHeat(6000, 600), "コロナ加熱が不正");

        test.assertTrue(level.getServer().getLevel(io.github.python121212.astrodock.AstroDock.MARS) != null, "火星ディメンションが登録されていない");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock custom nozzle verified");
        test.succeed();
    }
}
