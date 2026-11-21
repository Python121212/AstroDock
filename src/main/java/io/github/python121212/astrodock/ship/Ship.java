package io.github.python121212.astrodock.ship;

import java.util.*;
import io.github.python121212.astrodock.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.core.Direction;

public final class Ship extends Entity implements IEntityAdditionalSpawnData {
    private Hull hull;
    private UUID owner;
    private double spin;
    private int steps;
    private double tx,ty,tz; private float targetYaw;
    public Ship(EntityType<? extends Ship> type,Level level) { super(type,level); blocksBuilding=true; }
    public Hull hull() { return hull; }
    void configure(Hull h,UUID owner) { this.hull=h;this.owner=owner;setBoundingBox(makeBoundingBox()); }
    @Override protected AABB makeBoundingBox() {
        if (hull==null) return super.makeBoundingBox();
        double a=getYRot()*Mth.DEG_TO_RAD,c=Math.abs(Math.cos(a)),s=Math.abs(Math.sin(a));
        double x=c*hull.radiusX+s*hull.radiusZ,z=c*hull.radiusZ+s*hull.radiusX;
        return new AABB(getX()-x,getY(),getZ()-z,getX()+x,getY()+hull.height,getZ()+z);
    }
    @Override public double getPassengersRidingOffset() { return hull==null?1:hull.height; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public InteractionResult interact(Player p,InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (p.isSpectator() || !p.getUUID().equals(owner)) return InteractionResult.PASS;
        if (p.getItemInHand(hand).is(ShipParts.WRENCH.get())) dock((ServerPlayer)p);
        else if (!isVehicle()) p.startRiding(this,true);
        return InteractionResult.CONSUME;
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int count,boolean teleport) {
        tx=x;ty=y;tz=z;targetYaw=yaw;steps=Math.max(1,count);
    }
    @Override public void tick() {
        super.tick(); if (hull==null) { if (!level().isClientSide) discard(); return; }
        if(level().isClientSide) {
            if(steps>0) {setYRot(getYRot()+Mth.wrapDegrees(targetYaw-getYRot())/steps);setPos(getX()+(tx-getX())/steps,getY()+(ty-getY())/steps,getZ()+(tz-getZ())/steps);steps--;}
            return;
        }
        ServerPlayer pilot=getFirstPassenger() instanceof ServerPlayer rider?rider:null;
        double altitude=AstroDock.altitude(level(),getY());
        ShipMotion motion=new ShipMotion(getDeltaMovement(),spin).step(hull,altitude,getYRot(),pilot==null?0:pilot.zza,
                pilot==null?0:pilot.getXRot()*Mth.DEG_TO_RAD,pilot==null?0:-pilot.xxa);
        spin=motion.spin(); float old=getYRot(); setYRot((float)(old+spin));
        if (Math.abs(spin)>1e-5 && !level().noCollision(this,makeBoundingBox().deflate(0.001))) {setYRot(old);spin=0;}
        setBoundingBox(makeBoundingBox()); Vec3 before=position();move(MoverType.SELF,motion.velocity());
        setDeltaMovement(position().subtract(before));
        boolean orbit=level().dimension()==AstroDock.ORBIT;
        if (orbit!=(AstroDock.altitude(level(),getY())>=Atmosphere.SPACE)) transfer((ServerLevel)level(),orbit,pilot);
    }
    private void transfer(ServerLevel from,boolean leavingOrbit,ServerPlayer pilot) {
        ServerLevel to=from.getServer().getLevel(leavingOrbit?Level.OVERWORLD:AstroDock.ORBIT);
        if (to==null) return;
        Ship next=new Ship(ShipParts.SHIP.get(),to);next.configure(hull,owner);next.spin=spin;
        next.moveTo(getX(),getY()+(leavingOrbit?Atmosphere.SPACE:-Atmosphere.SPACE),getZ(),getYRot(),0);
        next.setDeltaMovement(getDeltaMovement());
        to.getChunkAt(next.blockPosition());
        if (!to.noCollision(next,next.getBoundingBox()) || !to.addFreshEntity(next)) {setDeltaMovement(Vec3.ZERO);return;}
        if (pilot!=null) {
            pilot.stopRiding();pilot.teleportTo(to,next.getX(),next.getY()+hull.height,next.getZ(),pilot.getYRot(),pilot.getXRot());
            pilot.startRiding(next,true);pilot.fallDistance=0;
        }
        discard();
    }
    void dock(ServerPlayer p) {
        if (!p.getUUID().equals(owner) || !p.getAbilities().mayBuild || hull==null || isRemoved()) return;
        if (getDeltaMovement().length()>0.45) {ShipWrench.message(p,"減速してから再設置してください");return;}
        ServerLevel l=(ServerLevel)level();
        Rotation rot=Rotation.values()[Math.floorMod(Math.round(getYRot()/90f),4)];
        Vec3 center=new Vec3(hull.cx,0,hull.cz).yRot(-Math.round(getYRot()/90f)*90*Mth.DEG_TO_RAD);
        BlockPos core=BlockPos.containing(getX()-center.x,getY()-hull.floor+0.5,getZ()-center.z);
        Map<BlockPos,BlockState> placement=new LinkedHashMap<>();
        for (Hull.Part part:hull.parts) {
            BlockPos pos=core.offset(part.pos().rotate(rot));
            if (l.isOutsideBuildHeight(pos) || !l.hasChunkAt(pos) || !l.getWorldBorder().isWithinBounds(pos) || !l.mayInteract(p,pos) || !l.isEmptyBlock(pos)) {
                ShipWrench.message(p,"再設置できません。建築可能な空間に移動してください");return;
            }
            placement.put(pos,part.state().rotate(rot));
        }
        List<BlockSnapshot> snapshots=new ArrayList<>();
        for(var e:placement.entrySet()) {
            snapshots.add(BlockSnapshot.create(l.dimension(),l,e.getKey())); l.setBlock(e.getKey(),e.getValue(),2|16|32);
        }
        if (ForgeEventFactory.onMultiBlockPlace(p,snapshots,Direction.UP)) {
            for(BlockSnapshot snapshot:snapshots) snapshot.restore(true,false);
            ShipWrench.message(p,"保護された場所には再設置できません");return;
        }
        boolean riding=p.getVehicle()==this;
        ejectPassengers(); if(riding) p.teleportTo(core.getX()+0.5,core.getY()+hull.floor+hull.height,core.getZ()+0.5);
        discard();ShipWrench.message(p,"船体を再設置しました");
    }
    @Override protected void defineSynchedData() {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {
        if(hull!=null) t.put("parts",hull.save()); if(owner!=null)t.putUUID("owner",owner); t.putDouble("spin",spin);
    }
    @Override protected void readAdditionalSaveData(CompoundTag t) {
        if(t.contains("parts") && t.hasUUID("owner")) configure(Hull.load(t.getList("parts",Tag.TAG_COMPOUND),level()),t.getUUID("owner"));spin=t.getDouble("spin");
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void writeSpawnData(FriendlyByteBuf b) {
        b.writeVarInt(hull.parts.size());for(Hull.Part p:hull.parts){b.writeBlockPos(p.pos());b.writeVarInt(Block.getId(p.state()));b.writeDouble(p.mass());}
    }
    @Override public void readSpawnData(FriendlyByteBuf b) {
        int n=b.readVarInt(); if(n<1 || n>ShipWrench.MAX_PARTS) throw new IllegalArgumentException("船体サイズが不正です");
        List<Hull.Part> parts=new ArrayList<>();for(int i=0;i<n;i++) parts.add(new Hull.Part(b.readBlockPos(),Block.stateById(b.readVarInt()),b.readDouble()));
        configure(new Hull(parts),null);
    }
}
