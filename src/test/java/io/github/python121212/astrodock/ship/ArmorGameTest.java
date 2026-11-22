package io.github.python121212.astrodock.ship;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class ArmorGameTest {
    private static Hull hull(Block armor) {
        var plate=armor.defaultBlockState();
        return new Hull(List.of(new Hull.Part(BlockPos.ZERO,ShipParts.ENGINE.get().defaultBlockState(),4),
            new Hull.Part(BlockPos.ZERO.east(),plate,Hull.partMass(plate,4)),
            new Hull.Part(BlockPos.ZERO.west(),plate,Hull.partMass(plate,4))));
    }
    private static Vec3 fly(Hull hull,float throttle,double altitude) {
        var body=new RigidBody();var v=Vec3.ZERO;
        for(int i=0;i<150;i++) v=body.step(hull,v,altitude,throttle,0,0,0);
        return v;
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void armorFlight(GameTestHelper test) {
        var level=test.getLevel();var access=level.registryAccess();
        var plains=access.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        for(String id:List.of("stratasteel_ore","blackshell_ore")) {
            var feature=access.registryOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,new ResourceLocation("astrodock",id)));
            test.assertTrue(plains.getGenerationSettings().hasFeature(feature),"平原に鉱脈が登録されていない: "+id);
        }
        for(String recipe:List.of("laminated_armor","blackshell_armor","blackshell_ingot_from_smelting","blackshell_ingot_from_blasting","iron_plate","stratasteel_plate","blackshell_plate"))
            test.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("astrodock",recipe)).isPresent(),"レシピがない: "+recipe);
        var light=hull(ShipParts.LAMINATED_ARMOR.get());var heavy=hull(ShipParts.BLACKSHELL_ARMOR.get());
        test.assertTrue(heavy.mass>light.mass && fly(heavy,1,64).z<fly(light,1,64).z,"重装甲で水平加速が落ちない");
        test.assertTrue(fly(heavy,0,64).y<fly(light,0,64).y && fly(heavy,0,5000).equals(fly(light,0,5000)),"大気と真空の落下が不正");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock armor flight verified");test.succeed();
    }
}
