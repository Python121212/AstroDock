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
        if (p==null || p.isSpectator() || !p.getAbilities().mayBuild || !(AstroDock.supported(l) || l.dimension()==CabinRooms.LEVEL)) return InteractionResult.PASS;
        if (p.getVehicle() instanceof Ship ship) { if (!l.isClientSide) ship.dock((ServerPlayer)p); return InteractionResult.SUCCESS; }
        CompoundTag t=stack.getTagElement(EDIT);
        boolean plant=l.getBlockState(pos).is(ShipParts.PLANT.get());
        // 船室内では機関範囲だけを設定可能。艦を入れ子にしない。
        if(l.dimension()==CabinRooms.LEVEL && !(t==null?plant:t.getBoolean("plant")))return InteractionResult.PASS;
        if (t==null && !plant && !l.getBlockState(pos).is(ShipParts.CORE.get())) return InteractionResult.PASS;
        if (l.isClientSide) return InteractionResult.SUCCESS;
        if (t==null) {
            if(plant && !l.mayInteract(p,pos))return InteractionResult.FAIL;
            if(plant && p.isShiftKeyDown() && l.getBlockEntity(pos) instanceof EngineAnchor anchor){anchor.setRange(null);message(p,"機関部の範囲を自動に戻しました");return InteractionResult.SUCCESS;}
            t=stack.getOrCreateTagElement(EDIT);t.putBoolean("plant",plant); t.putLong("core",pos.asLong()); t.putString("dim",l.dimension().location().toString());
            t.putBoolean("all",!p.isShiftKeyDown()); message(p,"範囲の1点目を右クリック（空中をしゃがみ右クリックで中止）"); return InteractionResult.SUCCESS;
        }
        BlockPos core=BlockPos.of(t.getLong("core"));
        if (!t.getString("dim").equals(l.dimension().location().toString()) || !l.hasChunkAt(core) || !l.getBlockState(core).is(t.getBoolean("plant")?ShipParts.PLANT.get():ShipParts.CORE.get())) {
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
            else if(t.getBoolean("plant")) {
                if(l.mayInteract(p,core) && l.getBlockEntity(core) instanceof EngineAnchor anchor){
                    anchor.setRange(new EnginePlant.Range(a.subtract(core),pos.subtract(core)));message(p,"機関部の範囲を登録しました。次に艦船コアで船体を選択してください");
                }stack.removeTagKey(EDIT);
            } else { t.putLong("b",pos.asLong()); message(p,"選択中: "+count(t,l)+"。右クリックで反転、コアを右クリックで組み立て"); }
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
            if (hardness<0
                    || !l.mayInteract(p,pos) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l,pos,state,p))) {
                message(p,"移動できないブロックです。除外してください: "+pos.toShortString()); return false;
            }
            var range=state.is(ShipParts.PLANT.get()) && l.getBlockEntity(pos) instanceof EngineAnchor anchor?anchor.range:null;
            parts.add(new Hull.Part(pos.subtract(core),state,Hull.partMass(state,hardness),range));
            if (parts.size()>MAX_PARTS) {message(p,"船体は256ブロックまでです");return false;}
        }
        Map<BlockPos,net.minecraft.world.level.block.state.BlockState> cells=new HashMap<>();
        for(var part:parts) cells.put(core.offset(part.pos()),part.state());
        var exposed=HullDoors.exposed(cells,CabinRooms.min(a,b),CabinRooms.max(a,b));
        if(exposed!=null) {message(p,"外壁には艦外ドアまたは艦外トラップドアを使用してください: "+exposed.toShortString());return false;}
        if(!HullDoors.completePairs(cells)){message(p,"ドアは上下両方を選択してください");return false;}
        var cabin=CabinRooms.level(l);if(cabin==null){message(p,"船室ディメンションがありません");return false;}
        Hull hull=new Hull(parts); if(hull.plant.error()!=null){message(p,hull.plant.error());return false;}

        Ship ship=new Ship(ShipParts.SHIP.get(),l); ship.configure(hull,p.getUUID()); ship.setPos(core.getX()+0.5+hull.cx,core.getY()+hull.floor,core.getZ()+0.5+hull.cz);
        if (!l.addFreshEntity(ship)) return false;
        var db=CabinRooms.data(l);var room=db.allocate(a,b,core);ship.roomId=room.id;db.locate(room,ship);
        room.walls(cabin,true);
        for(var part:parts) cabin.setBlock(room.core.offset(part.pos()),part.state(),2|16|32);
        for(var part:parts) if(!cabin.getBlockState(room.core.offset(part.pos())).equals(part.state()) ||
                !CabinRooms.tryCopy(l,core.offset(part.pos()),cabin,room.core.offset(part.pos()))) {
            for(var placed:parts)CabinRooms.erase(cabin,room.core.offset(placed.pos()));
            room.walls(cabin,false);db.forget(room);ship.discard();message(p,"この部品は船室へ移せません。元の船体は変更していません");return false;
        }
        for(var part:parts) {
            var pos=core.offset(part.pos());
            if(part.state().is(ShipParts.CANNON.get()) && l.getBlockEntity(pos) instanceof CannonMount mount)
                ship.restoreCannonCooldown(mount.readyAt());
            CabinRooms.erase(l,pos);
        }
        for(var part:parts) {
            l.updateNeighborsAt(core.offset(part.pos()),Blocks.AIR);
            cabin.updateNeighborsAt(room.core.offset(part.pos()),part.state().getBlock());
        }
        CabinRooms.ticket(cabin,new net.minecraft.world.level.ChunkPos(room.core));
        p.startRiding(ship,true); message(p,hull.engines==0?"外装の船化完了。Hで船室に入り、設備を置いてコアで反映してください":"組み立て完了。Wで推進、ジャンプ長押しで上昇、A/Dで旋回、Hで作業船室へ"); return true;
    }
}
