package io.github.python121212.astrodock.ship;

import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class EnergyTurretTest {
    @GameTest(templateNamespace="minecraft",template="igloo/top",timeoutTicks=120)
    public static void craftedTurret(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"ADCannonTest"));
        BlockPos core=new BlockPos(816,180,816),gun=core.east();CabinRooms.follow(l,new ChunkPos(core));
        l.setBlockAndUpdate(core,ShipParts.CORE.get().defaultBlockState());l.setBlockAndUpdate(core.north(),ShipParts.ENGINE.get().defaultBlockState());
        p.setPos(Vec3.atBottomCenterOf(core.south(2)));p.setYRot(0);p.setXRot(0);p.getAbilities().instabuild=false;
        var grid=new TransientCraftingContainer(p.inventoryMenu,3,3);
        grid.setItem(1,new ItemStack(Items.IRON_INGOT));grid.setItem(3,new ItemStack(Items.DISPENSER));
        grid.setItem(4,new ItemStack(Items.IRON_BLOCK));grid.setItem(5,new ItemStack(Items.DISPENSER));grid.setItem(7,new ItemStack(Items.REDSTONE));
        var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow();
        var hand=recipe.assemble(grid,l.registryAccess());t.assertTrue(hand.is(ShipParts.CANNON.get().asItem()) && hand.getCount()==1,"主砲をクラフトできない");
        p.setItemInHand(InteractionHand.MAIN_HAND,hand);
        hand.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(core),Direction.EAST,core,false)));
        t.assertTrue(p.getInventory().isEmpty() && l.getBlockEntity(gun) instanceof CannonMount,"ブロックアイテムを消費して砲塔を設置できない");
        var sel=new CompoundTag();sel.putLong("core",core.asLong());sel.putLong("a",core.north().asLong());sel.putLong("b",gun.asLong());sel.putBoolean("all",true);
        t.assertTrue(ShipWrench.assemble(l,p,sel),"組立失敗");Ship weak=(Ship)p.getVehicle();weak.command(p,ShipCommand.Action.CANNON);
        t.assertTrue(l.getEntitiesOfClass(ShipShot.class,weak.getBoundingBox().inflate(4)).isEmpty(),"出力不足でも発射できた");weak.dock(p);
        l.setBlockAndUpdate(gun.north(),ShipParts.ENGINE.get().defaultBlockState());t.assertTrue(ShipWrench.assemble(l,p,sel),"2基搭載での組立失敗");
        Ship ship=(Ship)p.getVehicle();var c=CabinRooms.level(l);var room=CabinRooms.data(l).get(ship.roomId);var mount=(CannonMount)c.getBlockEntity(room.core.east());
        ship.command(p,ShipCommand.Action.ENGINE);ship.command(p,ShipCommand.Action.CANNON);t.assertTrue(mount.shots()==0,"主機停止中に発射できた");
        ship.command(p,ShipCommand.Action.ENGINE);ship.command(p,ShipCommand.Action.CANNON);long first=l.getGameTime();
        t.assertTrue(mount.shots()==1 && mount.readyAt()==first+60,"弾薬なしでの発射・次弾時刻が不正");ship.dock(p);
        t.assertTrue(l.getBlockEntity(gun) instanceof CannonMount m && m.shots()==1,"再設置で記録が失われた");
        t.assertTrue(ShipWrench.assemble(l,p,sel),"再組み立て失敗");Ship rebuilt=(Ship)p.getVehicle();var live=(CannonMount)c.getBlockEntity(CabinRooms.data(l).get(rebuilt.roomId).core.east());
        t.startSequence().thenWaitUntil(()->t.assertTrue(l.getGameTime()>=first+59,"待機中")).thenExecute(()->{
            rebuilt.command(p,ShipCommand.Action.CANNON);t.assertTrue(l.getGameTime()==first+59 && live.shots()==1,"再組立または59 tickで連射できた");
        }).thenWaitUntil(()->t.assertTrue(l.getGameTime()>=first+60,"次弾待機中")).thenExecute(()->{
            rebuilt.command(p,ShipCommand.Action.CANNON);t.assertTrue(l.getGameTime()==first+60 && live.shots()==2,"60 tickで次弾が出ない");
            // 漂流した船を試験位置へ戻し、船室の実ブロックも通常のドック処理で片付ける。
            var h=rebuilt.hull();rebuilt.moveTo(core.getX()+.5+h.cx,core.getY()+h.floor,core.getZ()+.5+h.cz,0,0);rebuilt.setDeltaMovement(Vec3.ZERO);rebuilt.dock(p);
            for(var pos:BlockPos.betweenClosed(core.north(),gun))l.removeBlock(pos,false);
            com.mojang.logging.LogUtils.getLogger().info("AstroDock energy turret verified");
        }).thenSucceed();
    }
}
