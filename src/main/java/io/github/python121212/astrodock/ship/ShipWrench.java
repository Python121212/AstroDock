package io.github.python121212.astrodock.ship;

import java.util.*;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public final class ShipWrench extends Item {
    public static final int MAX_PARTS=256, MAX_VOLUME=512;
    public static final String EDIT="AstroDockSelection";
    public ShipWrench() { super(new Item.Properties().stacksTo(1)); }
    public static boolean selected(CompoundTag t, BlockPos p) {
        if (p.asLong()==t.getLong("core")) return true;
        boolean initial=t.getBoolean("all");
        for (long alt:t.getLongArray("toggle")) if (alt==p.asLong()) return !initial;
        return initial;
    }
    static void message(Player p,String s) { p.displayClientMessage(Component.literal(s),true); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext c) {
        Player p=c.getPlayer(); Level l=c.getLevel(); BlockPos pos=c.getClickedPos();
        if (p==null || p.isSpectator() || !p.getAbilities().mayBuild || !AstroDock.supported(l)) return InteractionResult.PASS;
        if (p.getVehicle() instanceof Ship ship) { if (!l.isClientSide) ship.dock((ServerPlayer)p); return InteractionResult.SUCCESS; }
        CompoundTag t=stack.getTagElement(EDIT);
        if (t==null && !l.getBlockState(pos).is(ShipParts.CORE.get())) return InteractionResult.PASS;
        if (l.isClientSide) return InteractionResult.SUCCESS;
        if (t==null) {
            t=stack.getOrCreateTagElement(EDIT); t.putLong("core",pos.asLong()); t.putString("dim",l.dimension().location().toString());
            t.putBoolean("all",!p.isShiftKeyDown()); message(p,"範囲の1点目を右クリック（空中をしゃがみ右クリックで中止）"); return InteractionResult.SUCCESS;
        }
        BlockPos core=BlockPos.of(t.getLong("core"));
        if (!t.getString("dim").equals(l.dimension().location().toString()) || !l.hasChunkAt(core) || !l.getBlockState(core).is(ShipParts.CORE.get())) {
            stack.removeTagKey(EDIT); message(p,"元のコアが見つかりません。設定を解除しました"); return InteractionResult.SUCCESS;
        }
        if (p.isShiftKeyDown() && pos.equals(core)) {
            stack.removeTagKey(EDIT);
            p.displayClientMessage(Component.translatable("hint.astrodock.selection_cleared"),true);
            return InteractionResult.SUCCESS;
        }
        if (!t.contains("a")) { t.putLong("a",pos.asLong()); message(p,"対角の2点目を右クリック"); }
        else if (!t.contains("b")) {
            BlockPos a=BlockPos.of(t.getLong("a"));
            if (!validRange(a,pos,core)) message(p,"コアを含む512マス以内の範囲を指定してください（各辺16以下）");
            else { t.putLong("b",pos.asLong()); message(p,"選択中: "+count(t,l)+"。右クリックで反転、コアを右クリックで組み立て"); }
        } else if (pos.equals(core)) {
            if (assemble((ServerLevel)l,(ServerPlayer)p,t)) stack.removeTagKey(EDIT);
        } else if (inside(t,pos)) {
            long key=pos.asLong(); Set<Long> toggles=new HashSet<>(); for(long v:t.getLongArray("toggle")) toggles.add(v);
            if (!toggles.remove(key)) toggles.add(key); t.putLongArray("toggle",new ArrayList<>(toggles));
            message(p,(selected(t,pos)?"船体に追加":"船体から除外")+"（選択中: "+count(t,l)+"）");
        } else message(p,"設定範囲の外です");
        return InteractionResult.SUCCESS;
    }
    public static boolean validRange(BlockPos a,BlockPos b,BlockPos core) {
        long x=Math.abs((long)a.getX()-b.getX())+1,y=Math.abs((long)a.getY()-b.getY())+1,z=Math.abs((long)a.getZ()-b.getZ())+1;
        return x<=16 && y<=16 && z<=16 && x*y*z<=MAX_VOLUME && new AABB(a,b).inflate(0.01).contains(core.getX(),core.getY(),core.getZ());
    }
    private static int count(CompoundTag t, Level l) {
        int n=0;
        for (BlockPos p:BlockPos.betweenClosed(BlockPos.of(t.getLong("a")),BlockPos.of(t.getLong("b"))))
            if (selected(t,p) && l.hasChunkAt(p) && !l.isEmptyBlock(p)) n++;
        return n;
    }
    private static boolean inside(CompoundTag t, BlockPos p) { return validRange(BlockPos.of(t.getLong("a")),BlockPos.of(t.getLong("b")),p); }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand) {
        ItemStack s=p.getItemInHand(hand);
        if (p.getVehicle() instanceof Ship ship) { if (!l.isClientSide) ship.dock((ServerPlayer)p); }
        else if (p.isShiftKeyDown() && s.getTagElement(EDIT)!=null) { if (!l.isClientSide) {s.removeTagKey(EDIT);message(p,"設定を中止しました");} }
        else return InteractionResultHolder.pass(s);
        return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
    }
    static boolean assemble(ServerLevel l, ServerPlayer p, CompoundTag t) {
        BlockPos core=BlockPos.of(t.getLong("core")),a=BlockPos.of(t.getLong("a")),b=BlockPos.of(t.getLong("b"));
        if (!validRange(a,b,core) || !l.getBlockState(core).is(ShipParts.CORE.get()) || p.isPassenger()) return false;
        List<Hull.Part> parts=new ArrayList<>();
        for (BlockPos pos:BlockPos.betweenClosed(a,b)) {
            if (!selected(t,pos)) continue;
            if (!l.hasChunkAt(pos) || !l.getWorldBorder().isWithinBounds(pos)) {message(p,"読み込み済みの範囲内で設定してください");return false;}
            var state=l.getBlockState(pos); if (state.isAir()) continue;
            float hardness=state.getDestroySpeed(l,pos);
            if (state.hasBlockEntity() || !state.getFluidState().isEmpty() || hardness<0 || (!state.is(ShipParts.HELM.get()) && !state.isCollisionShapeFullBlock(l,pos))
                    || !l.mayInteract(p,pos) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l,pos,state,p))) {
                message(p,"移動できないブロックです。除外してください: "+pos.toShortString()); return false;
            }
            parts.add(new Hull.Part(pos.subtract(core),state,1+Math.min(8,hardness)));
            if (parts.size()>MAX_PARTS) {message(p,"船体は256ブロックまでです");return false;}
        }
        Hull hull=new Hull(parts); if (hull.engines==0) {message(p,"スラスターを1個以上含めてください");return false;}
        Ship ship=new Ship(ShipParts.SHIP.get(),l); ship.configure(hull,p.getUUID()); ship.setPos(core.getX()+0.5+hull.cx,core.getY()+hull.floor,core.getZ()+0.5+hull.cz);
        if (!l.addFreshEntity(ship)) return false;
        for (Hull.Part part:parts) l.setBlock(core.offset(part.pos()),Blocks.AIR.defaultBlockState(),2|16|32);
        p.startRiding(ship,true); message(p,"組み立て完了。Wで推進、ジャンプ長押しで上昇、A/Dで旋回"); return true;
    }
}
