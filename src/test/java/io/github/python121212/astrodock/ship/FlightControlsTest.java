package io.github.python121212.astrodock.ship;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class FlightControlsTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void controlShip(GameTestHelper test) {
        var orbit=test.getLevel().getServer().getLevel(AstroDock.ORBIT);
        var pilot=new FakePlayer(orbit,new GameProfile(UUID.randomUUID(),"ADFlightTest"));
        var wrench=new ItemStack(ShipParts.WRENCH.get());pilot.setItemInHand(InteractionHand.MAIN_HAND,wrench);
        BlockPos core=new BlockPos(234,100,251), engine=core.south(), helm=core.south(2);
        orbit.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());
        orbit.setBlockAndUpdate(engine,ShipParts.ENGINE.get().defaultBlockState());
        orbit.setBlockAndUpdate(helm,ShipParts.HELM.get().defaultBlockState());
        for(int stage=0;stage<3;stage++) {
            click(pilot,wrench,core);
            if(stage>0) click(pilot,wrench,core);
            if(stage>1) click(pilot,wrench,helm);
            pilot.setShiftKeyDown(true);click(pilot,wrench,core);pilot.setShiftKeyDown(false);
            test.assertTrue(wrench.getTagElement(ShipWrench.EDIT)==null && !pilot.isPassenger() && orbit.getBlockState(engine).is(ShipParts.ENGINE.get()),"確定せずに選択解除できない");
        }
        click(pilot,wrench,core);click(pilot,wrench,core);click(pilot,wrench,helm);click(pilot,wrench,core);
        test.assertTrue(pilot.getVehicle() instanceof Ship,"操縦桿を含む艦を組み立てられない");
        Ship ship=(Ship)pilot.getVehicle();test.assertTrue(ship.hull().hasHelm,"操縦桿が検出されない");
        pilot.setPlayerInput(0,0,true,false);
        for(int i=0;i<5;i++) ship.tick();
        double held=ship.getDeltaMovement().y;
        test.assertTrue(held>0 && ship.getY()>core.getY(),"長押しで上昇しない");
        pilot.setPlayerInput(0,0,false,false);ship.tick();
        test.assertTrue(ship.getDeltaMovement().y<held && ship.getDeltaMovement().y>0,"キーを離した後の推力・慣性が不正");
        pilot.setYRot(45);pilot.setXRot(0);pilot.setPlayerInput(0,1,false,false);ship.tick();
        test.assertTrue(ship.getYRot()>0,"操縦桿が視線方向に旋回しない");
        pilot.stopRiding();ship.discard();
        com.mojang.logging.LogUtils.getLogger().info("AstroDock pilot controls verified");test.succeed();
    }
    private static void click(FakePlayer pilot,ItemStack wrench,BlockPos pos) {
        wrench.getItem().onItemUseFirst(wrench,new UseOnContext(pilot,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
    }
}
