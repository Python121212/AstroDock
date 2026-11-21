package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.network.syncher.*;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
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
    private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.QUATERNION);
    final RigidBody body = new RigidBody();
    private final Quaternionf previous = new Quaternionf();
    private final Matrix3f basis = new Matrix3f();
    private AABB localBounds;
    private int steps;
    private double tx,ty,tz; private float targetYaw;
    public Ship(EntityType<? extends Ship> type,Level level) { super(type,level); blocksBuilding=true; }
    public Hull hull() { return hull; }
    void configure(Hull h,UUID owner) { this.hull=h;this.owner=owner;refreshBounds(); }
    public Quaternionf renderRotation(float partial) { return previous.nlerp(body.attitude,partial,new Quaternionf()); }
    // |R| × 半径から保守的な AABB を計算。並進だけなら再計算しない。
    private void refreshBounds() {
        basis.set(body.attitude);
        double x=hull.radiusX, y=hull.height*0.5, z=hull.radiusZ, offset=y-hull.pivotHeight;
        double dx=Math.abs(basis.m00)*x+Math.abs(basis.m10)*y+Math.abs(basis.m20)*z;
        double dy=Math.abs(basis.m01)*x+Math.abs(basis.m11)*y+Math.abs(basis.m21)*z;
        double dz=Math.abs(basis.m02)*x+Math.abs(basis.m12)*y+Math.abs(basis.m22)*z;
        double cx=basis.m10*offset, cy=hull.pivotHeight+basis.m11*offset, cz=basis.m12*offset;
        localBounds=new AABB(cx-dx,cy-dy,cz-dz,cx+dx,cy+dy,cz+dz);
        setBoundingBox(makeBoundingBox());
    }
    @Override protected AABB makeBoundingBox() {
        return localBounds==null ? super.makeBoundingBox() : localBounds.move(getX(),getY(),getZ());
    }
    @Override public double getPassengersRidingOffset() { return localBounds==null?1:localBounds.maxY; }
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
        previous.set(body.attitude);
        if(level().isClientSide) {
            body.attitude.set(entityData.get(ROTATION));
            if (!previous.equals(body.attitude)) refreshBounds();
            if(steps>0) {setYRot(getYRot()+Mth.wrapDegrees(targetYaw-getYRot())/steps);setPos(getX()+(tx-getX())/steps,getY()+(ty-getY())/steps,getZ()+(tz-getZ())/steps);steps--;}
            return;
        }
        ServerPlayer pilot=getFirstPassenger() instanceof ServerPlayer rider?rider:null;
        double altitude=AstroDock.altitude(level(),getY());
        // 接地・無人・静止中は10 tickごとの支持面確認だけにする。
        if (pilot==null && onGround() && getDeltaMovement().lengthSqr()==0 && body.momentum.lengthSquared()==0
                && tickCount%10!=0 && !PlanetTravel.approaching(level().dimension(),position())) return;
        boolean roll=pilot!=null && pilot.getOffhandItem().is(ShipParts.WRENCH.get());
        float lateral=pilot==null?0:pilot.xxa;
        Vec3 velocity=body.step(hull,getDeltaMovement(),altitude,pilot==null?0:pilot.zza,
                pilot==null?0:pilot.getXRot()*Mth.DEG_TO_RAD, roll?0:lateral, roll?lateral:0);
        boolean rotated=!previous.equals(body.attitude);
        AABB beforeBox=getBoundingBox();
        if (rotated) refreshBounds();
        AABB swept=getBoundingBox().expandTowards(velocity).minmax(beforeBox);
        boolean clear=ClearFlight.clear((ServerLevel)level(),this,swept);
        if (rotated) {
            if (!clear && !level().noCollision(this,getBoundingBox().deflate(0.001))) {
                body.attitude.set(previous);body.momentum.zero();refreshBounds();
            } else {
                setYRot(body.heading(getYRot()));
                entityData.set(ROTATION,new Quaternionf(body.attitude));
            }
        }
        if (clear) {
            setPos(getX()+velocity.x,getY()+velocity.y,getZ()+velocity.z);
            setDeltaMovement(velocity);setOnGround(false);
            horizontalCollision=false;verticalCollision=false;verticalCollisionBelow=false;
        } else {
            Vec3 before=position();move(MoverType.SELF,velocity);
            setDeltaMovement(position().subtract(before));
        }
        crossBoundary((ServerLevel)level(),pilot);
    }
    Ship crossBoundary(ServerLevel from, ServerPlayer pilot) {
        var passage=PlanetTravel.at(from.dimension(),position());
        if (passage==null) {
            if (pilot!=null && getDeltaMovement().y>0) OrbitArrival.prefetch(from,position(),tickCount);
            return null;
        }
        ServerLevel to=from.getServer().getLevel(passage.dimension());
        if (to==null || !to.getWorldBorder().isWithinBounds(BlockPos.containing(passage.position()))) return null;
        if (pilot!=null && to.dimension()==AstroDock.ORBIT && !OrbitArrival.ready(to,passage.position())) return null;
        Ship next=new Ship(ShipParts.SHIP.get(),to);next.body.copyFrom(body);
        if (passage.frame()!=null) {
            next.body.attitude.premul(passage.frame()).normalize();
            passage.frame().transform(next.body.momentum);
        }
        next.configure(hull,owner);next.syncAttitude();
        Vec3 at=passage.position();next.moveTo(at.x,at.y,at.z,next.body.heading(getYRot()),0);
        next.setDeltaMovement(passage.rotate(getDeltaMovement()));
        to.getChunkAt(next.blockPosition());
        if (!to.noCollision(next,next.getBoundingBox()) || !to.addFreshEntity(next)) {setDeltaMovement(Vec3.ZERO);return null;}
        if (pilot!=null) {
            Vec3 look=passage.rotate(pilot.getLookAngle());
            pilot.stopRiding();pilot.teleportTo(to,next.getX(),next.getY()+next.getPassengersRidingOffset(),next.getZ(),passage.yaw(look,pilot.getYRot()),passage.pitch(look,pilot.getXRot()));
            pilot.startRiding(next,true);pilot.fallDistance=0;
        }
        discard();return next;
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
    @Override protected void defineSynchedData() { entityData.define(ROTATION,new Quaternionf()); }
    private void syncAttitude() { previous.set(body.attitude);entityData.set(ROTATION,new Quaternionf(body.attitude)); }
    @Override protected void addAdditionalSaveData(CompoundTag t) {
        if(hull!=null) t.put("parts",hull.save()); if(owner!=null)t.putUUID("owner",owner);
        Quaternionf q=body.attitude;
        t.putFloat("qx",q.x); t.putFloat("qy",q.y); t.putFloat("qz",q.z); t.putFloat("qw",q.w);
        t.putFloat("lx",body.momentum.x); t.putFloat("ly",body.momentum.y); t.putFloat("lz",body.momentum.z);
    }
    @Override protected void readAdditionalSaveData(CompoundTag t) {
        if(!t.contains("parts") || !t.hasUUID("owner")) return;
        Hull loaded=Hull.load(t.getList("parts",Tag.TAG_COMPOUND),level());
        if (t.contains("qw")) {
            body.attitude.set(t.getFloat("qx"),t.getFloat("qy"),t.getFloat("qz"),t.getFloat("qw"));
            if (body.attitude.lengthSquared()>0 && Float.isFinite(body.attitude.lengthSquared())) body.attitude.normalize();
            else body.attitude.identity();
            body.momentum.set(t.getFloat("lx"),t.getFloat("ly"),t.getFloat("lz"));
            if(!body.momentum.isFinite()) body.momentum.zero();
        } else {
            // 0.3.0 のヨー角と deg/tick を新しい世界座標の角運動量に変換。
            body.attitude.rotationY(-getYRot()*Mth.DEG_TO_RAD);
            body.momentum.set(0,-t.getFloat("spin")*Mth.DEG_TO_RAD,0);
            loaded.inertiaTensor.transform(body.momentum);body.attitude.transform(body.momentum);
        }
        configure(loaded,t.getUUID("owner"));syncAttitude();
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
