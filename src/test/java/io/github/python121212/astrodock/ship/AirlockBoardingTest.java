package io.github.python121212.astrodock.ship;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class AirlockBoardingTest {
    private static FakePlayer player(ServerLevel l,String name){return new FakePlayer(l,new GameProfile(UUID.randomUUID(),name));}
    private static void aim(FakePlayer p,BlockPos target,int distance){
        p.setPos(target.getX()+.5,target.getY()+.5-p.getEyeHeight(),target.getZ()-distance);p.setYRot(0);p.setXRot(0);
    }
    private static void outside(GameTestHelper t,Ship ship,FakePlayer p){
        ship.interact(p,InteractionHand.MAIN_HAND);t.assertTrue(p.level()==ship.level() && !p.isPassenger(),"入口以外から搭乗した");
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void entrances(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);var owner=player(l,"ADLockOwner");
        BlockPos core=new BlockPos(1610,165,1610),door=core.east().above(),hatch=core.east(2).above();
        for(int x=0;x<5;x++)for(int z=0;z<3;z++)l.setBlockAndUpdate(core.offset(x,0,z),Blocks.IRON_BLOCK.defaultBlockState());
        l.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());var lower=ShipParts.AIRLOCK.get().defaultBlockState();
        l.setBlock(door,lower,2|16|32);l.setBlock(door.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2|16|32);
        l.setBlockAndUpdate(hatch,ShipParts.HATCH.get().defaultBlockState());var tag=new CompoundTag();tag.putLong("a",core.asLong());tag.putLong("b",core.offset(4,2,2).asLong());tag.putLong("core",core.asLong());tag.putBoolean("all",true);
        t.assertTrue(ShipWrench.assemble(l,owner,tag),"試験船を組み立てられない");var ship=(Ship)owner.getVehicle();owner.stopRiding();
        var guest=player(l,"ADLockGuest");aim(owner,core,2);outside(t,ship,owner);aim(guest,core,2);outside(t,ship,guest);
        aim(guest,door,7);outside(t,ship,guest);aim(guest,door,2);guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));outside(t,ship,guest);
        ship.interact(guest,InteractionHand.OFF_HAND);t.assertTrue(guest.level()==l && !guest.isPassenger(),"空のオフハンドで入室した");guest.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        l.setBlockAndUpdate(door.north(),Blocks.STONE.defaultBlockState());outside(t,ship,guest);l.removeBlock(door.north(),false);
        ship.interact(guest,InteractionHand.MAIN_HAND);var cabin=CabinRooms.level(l);t.assertTrue(guest.level()==cabin && !guest.isPassenger(),"ドアから直接船室に入れない");
        for(int i=0;i<2;i++){var mate=player(l,"ADHatch"+i);aim(mate,hatch,2);ship.interact(mate,InteractionHand.MAIN_HAND);t.assertTrue(mate.level()==cabin && !mate.isPassenger(),"ハッチから直接船室に入れない");}
        var extra=player(l,"ADLockExtra");aim(extra,door,2);outside(t,ship,extra);
        aim(owner,hatch,2);ship.interact(owner,InteractionHand.MAIN_HAND);t.assertTrue(owner.level()==cabin && !owner.isPassenger(),"所有者枠で入れない");
        com.mojang.logging.LogUtils.getLogger().info("AstroDock airlock boarding verified");t.succeed();
    }
}
