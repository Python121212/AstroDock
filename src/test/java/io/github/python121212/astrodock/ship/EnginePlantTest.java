package io.github.python121212.astrodock.ship;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("astrodock")
@PrefixGameTestTemplate(false)
public class EnginePlantTest {
    private static Hull.Part part(Block b,int x,int y,int z){return new Hull.Part(new BlockPos(x,y,z),b.defaultBlockState(),1);}
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void topology(GameTestHelper t){
        // L字・縦積み・分岐・斜めの孤立部品。固定形状でないことも検証。
        var parts=new ArrayList<>(List.of(part(ShipParts.PLANT.get(),0,0,0),part(ShipParts.DRIVE.get(),0,1,0),
                part(ShipParts.GENERATOR.get(),1,1,0),part(ShipParts.GENERATOR.get(),1,1,1),part(ShipParts.COOLER.get(),1,2,1),part(ShipParts.DRIVE.get(),2,2,2)));
        var plant=EnginePlant.build(parts);t.assertTrue(plant.error()==null && plant.output()==4 && plant.cooling()==3,"接続・動力上限・冷却の集計が不正");
        parts.add(part(ShipParts.PLANT.get(),1,2,0));t.assertTrue(EnginePlant.build(parts).error()!=null,"部品の二重使用");
        parts.remove(parts.size()-1);parts.remove(1);t.assertTrue(EnginePlant.build(parts).error()!=null,"切断された機関が運転可能");t.succeed();
    }
    private static void click(FakePlayer p,ItemStack wrench,BlockPos pos){
        wrench.getItem().onItemUseFirst(wrench,new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
    }
    @GameTest(templateNamespace="minecraft",template="igloo/top")
    public static void modularShip(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(AstroDock.ORBIT);var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"ADEngineer"));
        BlockPos core=new BlockPos(944,200,944),anchor=core.east(),drive=core.east(2),gen=drive.south();
        var parts=Map.of(core,ShipParts.CORE,anchor,ShipParts.PLANT,drive,ShipParts.DRIVE,gen,ShipParts.GENERATOR,gen.east(),ShipParts.COOLER,
                core.north(),ShipParts.ENGINE,anchor.north(),ShipParts.ENGINE,core.south(),ShipParts.CANNON);
        parts.forEach((k,v)->l.setBlockAndUpdate(k,v.get().defaultBlockState()));
        var wrench=new ItemStack(ShipParts.WRENCH.get());p.setItemInHand(InteractionHand.MAIN_HAND,wrench);click(p,wrench,anchor);click(p,wrench,anchor);click(p,wrench,gen);
        var bounds=((EngineAnchor)l.getBlockEntity(anchor)).range;t.assertTrue(bounds!=null && !bounds.contains(gen.east().subtract(anchor)),"機関の範囲指定に失敗");
        var tag=new CompoundTag();tag.putLong("core",core.asLong());tag.putLong("a",core.north().asLong());tag.putLong("b",gen.east().asLong());tag.putBoolean("all",true);
        // 発電部を除外すると組立失敗、元のコア・ブロックは変更しない。
        tag.putLongArray("toggle",new long[]{gen.asLong()});t.assertTrue(!ShipWrench.assemble(l,p,tag) && l.getBlockState(core).is(ShipParts.CORE.get()),"不完全な機関で組立");
        tag.remove("toggle");t.assertTrue(ShipWrench.assemble(l,p,tag),"機関付きの船を組み立てられない");
        var s=(Ship)p.getVehicle();t.assertTrue(s.customEngine() && s.output()==3 && s.hull().plant.cooling()==1,"範囲外の冷却部を使用");
        p.setYRot(0);p.setXRot(0);p.setPlayerInput(0,1,false,false);
        var shots=s.getBoundingBox().inflate(4);s.command(p,ShipCommand.Action.CANNON);t.assertTrue(l.getEntitiesOfClass(ShipShot.class,shots).isEmpty(),"推進中に不足分の電力で発射");
        p.setPlayerInput(0,0,false,false);s.command(p,ShipCommand.Action.CANNON);t.assertTrue(!l.getEntitiesOfClass(ShipShot.class,shots).isEmpty(),"停船時に発射不能");
        p.setPlayerInput(0,1,true,false);s.tick();t.assertTrue(s.propulsionLoad()==1 && s.spareOutput()==0,"発射と推進の電力を二重使用");
        long later=l.getGameTime()+10;
        for(int i=0;i<55;i++)s.allocatePower(p,later+i);
        t.assertTrue(s.derated() && s.output()==1,"過熱時に出力が制限されない");
        var tag2=new CompoundTag();s.addAdditionalSaveData(tag2);var copy=new Ship(ShipParts.SHIP.get(),l);copy.readAdditionalSaveData(tag2);
        t.assertTrue(copy.derated() && copy.heat()==s.heat() && copy.hull().plant.equals(s.hull().plant),"セーブで機関情報消失");
        p.setPlayerInput(0,0,false,false);for(int i=0;i<65;i++)s.allocatePower(p,later+55+i);
        t.assertTrue(!s.derated() && s.output()==3,"負荷を下げても冷却・復帰しない");
        // 90度回してドック。保存した範囲も物理ブロックに合わせて回る。
        var h=s.hull();s.moveTo(core.getX()+.5-h.cz,core.getY()+h.floor,core.getZ()+.5+h.cx,90,0);s.setDeltaMovement(Vec3.ZERO);s.dock(p);
        var landed=core.offset(anchor.subtract(core).rotate(Rotation.CLOCKWISE_90));t.assertTrue(s.isRemoved() && l.getBlockEntity(landed) instanceof EngineAnchor a && a.range.equals(bounds.rotate(Rotation.CLOCKWISE_90)),"再設置で機関範囲がずれた");
        for(var pos:parts.keySet())l.removeBlock(core.offset(pos.subtract(core).rotate(Rotation.CLOCKWISE_90)),false);
        com.mojang.logging.LogUtils.getLogger().info("AstroDock modular engine verified");t.succeed();
    }
}
