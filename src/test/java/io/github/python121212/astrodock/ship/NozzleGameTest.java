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
                new Hull.Part(BlockPos.ZERO.south(), iron, 4),
                new Hull.Part(BlockPos.ZERO.south().east(), iron, 4)
        ));
        var equipped = new Hull(List.of(
                new Hull.Part(BlockPos.ZERO, thruster, 4),
                new Hull.Part(BlockPos.ZERO.south(), nozzle, 4),
                new Hull.Part(BlockPos.ZERO.south().east(), nozzle, 4)
        ));
        test.assertTrue(equipped.effectiveEngines > bare.effectiveEngines, "ノズル外殻で実効推力が上がっていない");

        var b1 = new RigidBody(); var v1 = Vec3.ZERO;
        var b2 = new RigidBody(); var v2 = Vec3.ZERO;
        for (int i = 0; i < 10; i++) {
            v1 = b1.step(bare, v1, 5000, 1.0f, 0, 0, 0);
            v2 = b2.step(equipped, v2, 5000, 1.0f, 0, 0, 0);
        }
        test.assertTrue(v2.length() > v1.length(), "自作ノズル付きの加速が通常より大きくない");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock custom nozzle verified");
        test.succeed();
    }
}
