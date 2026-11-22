package io.github.python121212.astrodock;

import io.github.python121212.astrodock.ship.ShipParts;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class DepartureGameTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top",timeoutTicks=400)
    public static void prepareOrbit(GameTestHelper test) {
        var earth=test.getLevel().getServer().overworld();
        var orbit=earth.getServer().getLevel(AstroDock.ORBIT);
        Vec3 launch=new Vec3(32000,Atmosphere.SPACE-50,-32000);
        Vec3 arrival=PlanetTravel.at(Level.OVERWORLD,launch.add(0,50,0)).position();
        test.assertTrue(!OrbitArrival.nearLaunch(Level.OVERWORLD,4903) && OrbitArrival.nearLaunch(Level.OVERWORLD,4904)
                && OrbitArrival.nearLaunch(AstroDock.MARS,416) && !OrbitArrival.nearLaunch(AstroDock.ORBIT,4999),"先読み範囲が不正");
        int chunks=orbit.getChunkSource().getLoadedChunksCount();
        OrbitArrival.prefetch(earth,launch.add(0,-200,0),0);
        test.assertTrue(chunks==orbit.getChunkSource().getLoadedChunksCount(),"範囲外で同期ロードされた");
        OrbitArrival.prefetch(earth,launch,0);
        test.assertTrue(!OrbitArrival.ready(orbit,arrival),"新規到着先を同期ロードしている");
        test.succeedWhen(()->{
            test.assertTrue(OrbitArrival.ready(orbit,arrival),"到着先を準備中");
            com.mojang.logging.LogUtils.getLogger().info("AstroDock orbit prefetch verified");
        });
    }

    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void shipyardContents(GameTestHelper test) {
        var tab=ShipParts.TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(test.getLevel().enabledFeatures(),false,test.getLevel().registryAccess()));
        test.assertTrue(tab.getIconItem().is(ShipParts.CORE.get().asItem()),"コアのアイコンがありません");
        test.assertTrue(tab.getDisplayItems().stream().map(ItemStack::getItem).toList().equals(List.of(
                ShipParts.CORE.get().asItem(),ShipParts.ENGINE.get().asItem(),ShipParts.HELM.get().asItem(),ShipParts.CONSOLE.get().asItem(),ShipParts.AIRLOCK.get().asItem(),ShipParts.HATCH.get().asItem(),ShipParts.CANNON.get().asItem(),ShipParts.MISSILES.get().asItem(),ShipParts.TORPEDOES.get().asItem(),ShipParts.WRENCH.get(),AstroDock.CAPSULE.get(),ShipParts.PLANT.get().asItem(),ShipParts.DRIVE.get().asItem(),ShipParts.GENERATOR.get().asItem(),ShipParts.COOLER.get().asItem(),ShipParts.GRAVITY_CONTROLLER.get().asItem(),
                ShipParts.STRATASTEEL_ORE.get().asItem(),ShipParts.BLACKSHELL_ORE.get().asItem(),ShipParts.LAMINATED_ARMOR.get().asItem(),ShipParts.LAMINATED_ARMOR_SLOPE.get().asItem(),ShipParts.BLACKSHELL_ARMOR.get().asItem(),ShipParts.BLACKSHELL_ARMOR_SLOPE.get().asItem(),ShipParts.STRATASTEEL_FLAKE.get(),ShipParts.RAW_BLACKSHELL.get(),ShipParts.BLACKSHELL_INGOT.get(),ShipParts.IRON_PLATE.get(),ShipParts.STRATASTEEL_PLATE.get(),ShipParts.BLACKSHELL_PLATE.get(),ShipParts.NOZZLE_CASING.get().asItem(),ShipParts.NOZZLE_CASING_SLOPE.get().asItem(),ShipParts.MINING_LASER.get().asItem(),ShipParts.SALVAGE_BEAM.get().asItem(),ShipParts.POINT_DEFENSE_LASER.get().asItem(),ShipParts.LIFE_SUPPORT.get().asItem(),ShipParts.WARP_DRIVE.get().asItem(),ShipParts.SHIELD_GENERATOR.get().asItem(),ShipParts.PLANET_SCANNER.get().asItem(),ShipParts.LASER_CUTTER.get(),ShipParts.WELDING_TORCH.get(),ShipParts.CANNON_BARREL.get().asItem(),ShipParts.CANNON_TURRET.get().asItem(),ShipParts.CANNON_BREECH.get().asItem())),"造船所の登録内容が不正");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock shipyard verified");test.succeed();
    }
}
