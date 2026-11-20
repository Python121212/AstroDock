package io.github.python121212.astrodock;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class OrbitGameTest {
    @GameTest(templateNamespace = "minecraft", template = "igloo/top")
    public static void buildInOrbit(GameTestHelper test) {
        var orbit = test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        test.assertTrue(orbit != null, "宇宙ディメンションが読み込まれていません");
        BlockPos pos = new BlockPos(100, 64, 100);
        test.assertTrue(AstroDock.altitude(orbit, pos.getY()) == 5064, "宇宙の高度換算が一致しません");
        orbit.setBlockAndUpdate(pos, Blocks.IRON_BLOCK.defaultBlockState());
        test.assertTrue(orbit.getBlockState(pos).is(Blocks.IRON_BLOCK), "宇宙で建築できません");
        orbit.removeBlock(pos, false);
        test.succeed();
    }
}
