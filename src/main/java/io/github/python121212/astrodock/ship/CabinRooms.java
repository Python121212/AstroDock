package io.github.python121212.astrodock.ship;

import java.util.*;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 船室の正本は実チャンク。エンティティやスポーンパケットにはコンテナの内容を複製しない。 */
@Mod.EventBusSubscriber(modid="astrodock")
public final class CabinRooms extends SavedData {
    public static final ResourceKey<Level> LEVEL=ResourceKey.create(Registries.DIMENSION,new ResourceLocation("astrodock","cabins"));
    private static final String VISIT="AstroDockRoom";
    private static final TicketType<ChunkPos> ACTIVE=TicketType.create("astrodock_cabin", Comparator.comparingLong(ChunkPos::toLong),80);
    private final Map<Integer,Room> rooms=new HashMap<>();
    private int next=1;
    static ServerLevel level(ServerLevel l) {return l.getServer().getLevel(LEVEL);}
    static CabinRooms data(ServerLevel l) {return level(l).getDataStorage().computeIfAbsent(CabinRooms::load,CabinRooms::new,"astrodock_rooms");}
    static BlockPos min(BlockPos a,BlockPos b) {return new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));}
    static BlockPos max(BlockPos a,BlockPos b) {return new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));}
    static class Room {
        final int id; final BlockPos lo,hi,core;
        final Set<UUID> visitors=new HashSet<>();
        ResourceKey<Level> dimension; UUID shipId; ChunkPos chunk;
        Room(int id,BlockPos lo,BlockPos hi,BlockPos core){this.id=id;this.lo=lo;this.hi=hi;this.core=core;}
        Map<BlockPos,BlockState> cells(ServerLevel cabin) {
            Map<BlockPos,BlockState> result=new LinkedHashMap<>();
            for(var pos:BlockPos.betweenClosed(lo,hi)) {var state=cabin.getBlockState(pos);if(!state.isAir()) result.put(pos.immutable(),state);}
            return result;
        }
        boolean contains(BlockPos p) {return min(p,lo).equals(lo) && max(p,hi).equals(hi);}
        void walls(ServerLevel l,boolean place) {
            var a=lo.offset(-1,-1,-1);var b=hi.offset(1,3,1);
            for(var p:BlockPos.betweenClosed(a,b)) if(p.getX()==a.getX() || p.getY()==a.getY() || p.getZ()==a.getZ() || p.getX()==b.getX() || p.getY()==b.getY() || p.getZ()==b.getZ())
                l.setBlock(p,(place?Blocks.BARRIER:Blocks.AIR).defaultBlockState(),2|16|32);
        }
        Vec3 entry(ServerLevel cabin,Hull hull) {
            return hull.deck.entrance(v->{Vec3 at=v.add(core.getX(),core.getY(),core.getZ());return cabin.noCollision(new AABB(at.x-.3,at.y,at.z-.3,at.x+.3,at.y+1.8,at.z+.3)) && !cabin.getBlockState(BlockPos.containing(at).below()).getCollisionShape(cabin,BlockPos.containing(at).below()).isEmpty();});
        }
    }
    Room allocate(BlockPos a,BlockPos b,BlockPos core) {
        int id=next++; BlockPos src=min(a,b), lo=new BlockPos((id%8192)*128,96,(id/8192)*128);
        Room room=new Room(id,lo,lo.offset(max(a,b).subtract(src)),lo.offset(core.subtract(src)));rooms.put(id,room);setDirty();return room;
    }
    Room get(int id) {return rooms.get(id);}
    void forget(Room r) {rooms.remove(r.id);setDirty();}
    void locate(Room r,Ship ship) {
        var c=ship.chunkPosition();var dim=ship.level().dimension();
        if(!c.equals(r.chunk) || r.dimension!=dim || !ship.getUUID().equals(r.shipId)) {
            r.chunk=c;r.dimension=dim;r.shipId=ship.getUUID();setDirty();
        }
    }
    static void ticket(ServerLevel l,ChunkPos c) {l.getChunkSource().addRegionTicket(ACTIVE,c,2,c);}
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("next",next);var list=new ListTag();for(Room r:rooms.values()) {
            var t=new CompoundTag();t.putInt("id",r.id);t.putLong("lo",r.lo.asLong());t.putLong("hi",r.hi.asLong());t.putLong("core",r.core.asLong());
            t.putString("dim",r.dimension.location().toString());t.putUUID("ship",r.shipId);t.putLong("chunk",r.chunk.toLong());
            var crew=new ListTag();for(UUID v:r.visitors) crew.add(NbtUtils.createUUID(v));t.put("crew",crew);list.add(t);
        }tag.put("rooms",list);return tag;
    }
    static CabinRooms load(CompoundTag tag) {
        var d=new CabinRooms();d.next=tag.getInt("next");for(Tag row:tag.getList("rooms",Tag.TAG_COMPOUND)) {
            var t=(CompoundTag)row;var r=new Room(t.getInt("id"),BlockPos.of(t.getLong("lo")),BlockPos.of(t.getLong("hi")),BlockPos.of(t.getLong("core")));
            r.dimension=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(t.getString("dim")));r.shipId=t.getUUID("ship");r.chunk=new ChunkPos(t.getLong("chunk"));
            for(Tag id:t.getList("crew",Tag.TAG_INT_ARRAY))r.visitors.add(NbtUtils.loadUUID(id));d.rooms.put(r.id,r);
        }return d;
    }
    // CloneCommands と同様に全状態の設置後に BE を読む。ForgeCaps を含む永続 NBT のみを転送。
    static void copyData(ServerLevel src,BlockPos from,ServerLevel dest,BlockPos to) {
        var old=src.getBlockEntity(from);var be=dest.getBlockEntity(to);
        if(old!=null && be!=null) {be.load(old.saveWithoutMetadata());be.setChanged();dest.sendBlockUpdated(to,dest.getBlockState(to),dest.getBlockState(to),2);}
        var box=new BoundingBox(from);var delta=to.subtract(from);
        dest.getBlockTicks().copyAreaFrom(src.getBlockTicks(),box,delta);dest.getFluidTicks().copyAreaFrom(src.getFluidTicks(),box,delta);
    }
    static void erase(ServerLevel l,BlockPos pos) {
        Clearable.tryClear(l.getBlockEntity(pos));l.removeBlockEntity(pos);
        l.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
        var box=new BoundingBox(pos);l.getBlockTicks().clearArea(box);l.getFluidTicks().clearArea(box);
    }
    static void enter(Ship s,ServerPlayer p,Room r) {
        ServerLevel c=level(p.serverLevel());Vec3 v=r.entry(c,s.hull());
        if(v==null) {ShipWrench.message(p,"船室に入れる足場がありません");return;}
        r.visitors.add(p.getUUID());data(c).setDirty();p.getPersistentData().putInt(VISIT,r.id);
        ticket(p.serverLevel(),s.chunkPosition());p.stopRiding();v=v.add(r.core.getX(),r.core.getY(),r.core.getZ());
        p.teleportTo(c,v.x,v.y,v.z,p.getYRot(),0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
    }
    public static void leave(ServerPlayer p) {
        if(p.level().dimension()!=LEVEL || !p.isAlive())return;
        CabinRooms d=data(p.serverLevel());Room r=d.get(p.getPersistentData().getInt(VISIT));
        if(r==null || !r.visitors.contains(p.getUUID()))return;
        var dest=p.serverLevel().getServer().getLevel(r.dimension);if(dest==null)return;
        ticket(dest,r.chunk);if(!(dest.getEntity(r.shipId) instanceof Ship s)) {ShipWrench.message(p,"艦を読み込んでいます。少し待ってもう一度操作してください");return;}
        r.visitors.remove(p.getUUID());d.setDirty();p.getPersistentData().remove(VISIT);
        p.teleportTo(dest,s.getX(),s.getY()+s.getPassengersRidingOffset(),s.getZ(),p.getYRot(),0);p.startRiding(s,true);p.fallDistance=0;
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount%20!=0 || !p.getPersistentData().contains(VISIT))return;
        CabinRooms d=data(p.serverLevel());Room r=d.get(p.getPersistentData().getInt(VISIT));if(r==null)return;
        if(p.level().dimension()!=LEVEL || !p.isAlive()) {r.visitors.remove(p.getUUID());d.setDirty();p.getPersistentData().remove(VISIT);return;}
        var dest=p.serverLevel().getServer().getLevel(r.dimension);if(dest!=null) ticket(dest,r.chunk);
    }
    @SubscribeEvent public static void died(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if(e.getEntity() instanceof ServerPlayer p && p.level().dimension()==LEVEL) {
            var d=data(p.serverLevel());var r=d.get(p.getPersistentData().getInt(VISIT));if(r!=null && r.visitors.remove(p.getUUID()))d.setDirty();
        }
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e) {
        if(!(e.getLevel() instanceof ServerLevel l) || l.dimension()!=LEVEL)return;
        Room r=data(l).get(e.getPlayer().getPersistentData().getInt(VISIT));
        if(r==null || !r.contains(e.getPos()) || e.getPos().equals(r.core)) e.setCanceled(true);
    }
    @SubscribeEvent public static void placing(BlockEvent.EntityPlaceEvent e) {
        if(!(e.getLevel() instanceof ServerLevel l) || l.dimension()!=LEVEL || !(e.getEntity() instanceof Player p))return;
        Room r=data(l).get(p.getPersistentData().getInt(VISIT));
        if(r==null || !r.contains(e.getPos()) || e instanceof BlockEvent.EntityMultiPlaceEvent m && m.getReplacedBlockSnapshots().stream().anyMatch(b->!r.contains(b.getPos()))) {e.setCanceled(true);return;}
        if(HullDoors.ordinary(e.getPlacedBlock()) && HullDoors.exposed(r.cells(l),r.lo,r.hi)!=null) {
            e.setCanceled(true);ShipWrench.message(p,"外壁には艦外ドアまたは艦外トラップドアを使ってください");
        }
    }
}
