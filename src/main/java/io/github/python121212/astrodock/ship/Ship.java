package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.network.syncher.*;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
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
    private final long[] nextShot=new long[ShipWeapon.values().length];
    int roomId; // 0 は 0.9 以前の船体
    private CabinRooms.Room room() {return roomId==0?null:CabinRooms.data((ServerLevel)level()).get(roomId);}
    private UUID owner;
    private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Boolean> ENGINE_ON=SynchedEntityData.defineId(Ship.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ON_DECK=SynchedEntityData.defineId(Ship.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> FEET=SynchedEntityData.defineId(Ship.class,EntityDataSerializers.VECTOR3);
    // UUID と足元を別フィールドにして歩いた乗員の位置だけを送る。所有者枠は常に予約。
    private record Guest(EntityDataAccessor<Optional<UUID>> id, EntityDataAccessor<Vector3f> feet) {}
    private static Guest guestSlot() {
        return new Guest(SynchedEntityData.defineId(Ship.class,EntityDataSerializers.OPTIONAL_UUID),
                SynchedEntityData.defineId(Ship.class,EntityDataSerializers.VECTOR3));
    }
    private static final List<Guest> GUESTS=List.of(guestSlot(),guestSlot(),guestSlot());
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
    public boolean engineOn() {return entityData.get(ENGINE_ON);}
    boolean ownerWalking() {return entityData.get(ON_DECK);}
    public boolean isOwner(Entity p) {return p.getUUID().equals(owner);}
    private Guest slot(UUID id) {
        for(Guest g:GUESTS) if(id.equals(entityData.get(g.id).orElse(null))) return g;
        return null;
    }
    private Guest vacant() {
        for(Guest g:GUESTS) if(entityData.get(g.id).isEmpty()) return g;
        return null;
    }
    private EntityDataAccessor<Vector3f> feetKey(Entity p) {
        if(isOwner(p)) return ownerWalking()?FEET:null;
        Guest g=slot(p.getUUID());return g==null?null:g.feet;
    }
    public boolean walking(Entity p) {return feetKey(p)!=null;}
    Vector3f deckFeet(Entity p) {return new Vector3f(entityData.get(feetKey(p)));}
    private Vec3 freeEntrance() {
        return hull.deck.entrance(candidate->{
            for(Entity rider:getPassengers()) if(walking(rider)) {
                Vector3f v=entityData.get(feetKey(rider));
                if(Math.abs(v.x-candidate.x)<.6 && Math.abs(v.y-candidate.y)<1.8 && Math.abs(v.z-candidate.z)<.6) return false;
            }
            return true;
        });
    }

    /** 船体ブロックのローカル座標と物理重心のオフセットは描画と同じものを使う。 */
    private Vec3 worldFeet(Entity p) {
        Vector3f v=deckFeet(p).sub((float)(hull.cx+.5),(float)(hull.cy+.5),(float)(hull.cz+.5));
        body.attitude.transform(v);
        return position().add(v.x,hull.pivotHeight+v.y,v.z);
    }
    private BlockState aimedPanel(Player p) {
        if(hull==null || !walking(p) || !hasPassenger(p)) return null;
        return aimedPart(p);
    }
    private BlockState aimedPart(Player p) {
        Vector3f eye=p.getEyePosition().subtract(getX(),getY()+hull.pivotHeight,getZ()).toVector3f();
        body.attitude.transformInverse(eye).add((float)(hull.cx+.5),(float)(hull.cy+.5),(float)(hull.cz+.5));
        Vector3f look=body.attitude.transformInverse(p.getLookAngle().toVector3f());
        return hull.deck.aimedBlock(new Vec3(eye.x,eye.y,eye.z),new Vec3(look.x,look.y,look.z));
    }
    public boolean hasPanelInSight(Player p) {
        BlockState b=aimedPanel(p);
        return isOwner(p) && b!=null && (b.is(ShipParts.HELM.get()) || b.is(ShipParts.CORE.get()) || b.is(ShipParts.CONSOLE.get()));
    }
    void command(ServerPlayer p,ShipCommand.Action action) {
        if (hull==null || isRemoved() || !p.isAlive() || p.isSpectator() || p.getVehicle()!=this) return;
        if(action==ShipCommand.Action.CABIN) {
            var r=room();if(r!=null) CabinRooms.enter(this,p,r);else ShipWrench.message(p,"作業船室を使うには艦を再組み立てしてください");return;
        }
        if(!isOwner(p))return;
        switch(action) {
            case CANNON, MISSILE, TORPEDO -> {
                if(!ownerWalking() && engineOn()) fire(p,ShipWeapon.valueOf(action.name()));
            }
            case ENGINE -> { if(!ownerWalking()) toggleEngine(p); }
            case SEAT -> {
                if(ownerWalking()) entityData.set(ON_DECK,false);
                else {
                    Vec3 entrance=freeEntrance();
                    if(entrance==null) {p.displayClientMessage(Component.translatable("hint.astrodock.no_deck"),true);return;}
                    entityData.set(FEET,entrance.toVector3f());entityData.set(ON_DECK,true);
                }
                positionRider(p);
            }
            case USE_PANEL -> {
                var target=aimedPanel(p);
                if(target!=null && target.is(ShipParts.CONSOLE.get())) toggleEngine(p);
                else if(target!=null && (target.is(ShipParts.HELM.get()) || target.is(ShipParts.CORE.get()))) {
                    entityData.set(ON_DECK,false);positionRider(p);
                }
            }
        }
    }
    private void fire(ServerPlayer p,ShipWeapon w) {
        var level=(ServerLevel)level();long now=level.getGameTime();
        if(now<nextShot[w.ordinal()])return;
        Vec3 aim=p.getLookAngle().normalize();var dir=body.attitude.transformInverse(aim.toVector3f());
        Vec3 local=new Vec3(dir.x,dir.y,dir.z);boolean installed=false;
        for(var part:hull.parts) if(part.state().is(w.mount.get())) {
            installed=true;if(!hull.deck.clearMuzzle(part.pos(),local))continue;
            var ammo=net.minecraft.world.item.ItemStack.EMPTY;
            if(w==ShipWeapon.CANNON) {
                if(hull.engines<MainCannonBlock.REQUIRED_OUTPUT) {
                    p.displayClientMessage(Component.translatable("hint.astrodock.cannon_power",hull.engines,MainCannonBlock.REQUIRED_OUTPUT),true);return;
                }
            } else if(!p.getAbilities().instabuild) {
                ammo=p.getInventory().items.stream().filter(stack->stack.is(w.ammo)).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY);
                if(ammo.isEmpty()){p.displayClientMessage(Component.translatable("hint.astrodock.no_ammo",w.ammo.getDescription()),true);return;}
            }
            var v=body.attitude.transform(new Vector3f((float)(part.pos().getX()-hull.cx),(float)(part.pos().getY()-hull.cy),(float)(part.pos().getZ()-hull.cz)));
            var origin=position().add(v.x,hull.pivotHeight+v.y,v.z);
            if(!level.hasChunkAt(BlockPos.containing(origin)))return;
            var shot=new ShipShot(w.type.get(),level);shot.launch(this,p,origin,aim);
            if(level.addFreshEntity(shot)) {
                nextShot[w.ordinal()]=now+w.delay;
                if(!ammo.isEmpty())ammo.shrink(1);
                if(w==ShipWeapon.CANNON && roomId!=0) {
                    var r=room();if(r!=null && CabinRooms.level(level).getBlockEntity(r.core.offset(part.pos())) instanceof CannonMount mount)mount.fired(nextShot[w.ordinal()]);
                }
                level.playSound(null,blockPosition(),w.sound,net.minecraft.sounds.SoundSource.PLAYERS,1,.8f);
            }return;
        }
        p.displayClientMessage(Component.translatable(installed?"hint.astrodock.muzzle_blocked":"hint.astrodock.no_mount",w.mount.get().getName()),true);
    }
    void restoreCannonCooldown(long readyAt){int k=ShipWeapon.CANNON.ordinal();nextShot[k]=Math.max(nextShot[k],readyAt);}
    private void toggleEngine(ServerPlayer p) {
        entityData.set(ENGINE_ON,!engineOn());
        p.displayClientMessage(Component.translatable(engineOn()?"hud.astrodock.engine_on":"hud.astrodock.engine_off"),true);
    }
    @Override protected void positionRider(Entity p,Entity.MoveFunction move) {
        if(hull!=null && hasPassenger(p) && walking(p)) {
            Vec3 at=worldFeet(p);move.accept(p,at.x,at.y,at.z);
        } else super.positionRider(p,move);
    }
    @Override public boolean shouldRiderSit() {return !ownerWalking() && getPassengers().size()==1 && isOwner(getFirstPassenger());}
    @Override protected boolean canAddPassenger(Entity p) {
        if(!(p instanceof Player) || hull==null)return false;
        if(isOwner(p))return true;
        CabinRooms.Room r=level().isClientSide?null:room();
        long guests=getPassengers().stream().filter(v->!isOwner(v)).count()+(r==null?0:r.visitors.stream().filter(id->!id.equals(owner)).count());
        return guests<3 && vacant()!=null && freeEntrance()!=null;
    }
    @Override protected void addPassenger(Entity p) {
        super.addPassenger(p);
        if(!level().isClientSide && !isOwner(p) && slot(p.getUUID())==null && hull!=null) {
            Guest g=vacant();Vec3 entrance=freeEntrance();
            if(g!=null && entrance!=null) {
                entityData.set(g.feet,entrance.toVector3f());entityData.set(g.id,Optional.of(p.getUUID()));
            }
        }
    }
    @Override protected void removePassenger(Entity p) {
        super.removePassenger(p);
        if(!level().isClientSide) {
            if(isOwner(p)) entityData.set(ON_DECK,false);
            else {Guest g=slot(p.getUUID());if(g!=null) entityData.set(g.id,Optional.empty());}
        }
    }
    private void walk(ServerPlayer rider) {
        if(rider.zza==0 && rider.xxa==0) return;
        Vector3f direction=body.attitude.transformInverse(rider.getLookAngle().toVector3f());
        if(direction.x*direction.x+direction.z*direction.z<1e-6) {
            // 真上・真下を向いた時は視線の横軸からデッキ上の前方を求める。
            double yaw=rider.getYRot()*Mth.DEG_TO_RAD;
            body.attitude.transformInverse(direction.set((float)Math.cos(yaw),0,(float)Math.sin(yaw)));
            direction.set(-direction.z,0,direction.x);
        }
        var key=feetKey(rider);
        var now=entityData.get(key);
        var next=hull.deck.advance(now,new Vec3(direction.x,0,direction.z),rider.zza,rider.xxa);
        if(next!=now) entityData.set(key,next);
    }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public InteractionResult interact(Player p,InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (p.isSpectator() || hull==null) return InteractionResult.PASS;
        if (p.getItemInHand(hand).is(ShipParts.WRENCH.get()) && isOwner(p)) dock((ServerPlayer)p);
        else if (!p.isPassenger()) {
            boolean hatch=p.getItemInHand(hand).isEmpty() && roomId!=0 && HullDoors.airlock(aimedPart(p));
            if(!p.startRiding(this)) p.displayClientMessage(Component.translatable("hint.astrodock.crew_full"),true);
            else if(hatch && room()!=null) CabinRooms.enter(this,(ServerPlayer)p,room());
        }
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
        if(roomId!=0) {
            var d=CabinRooms.data((ServerLevel)level());var r=d.get(roomId);
            if(r!=null) {
                var shell=(ServerLevel)level();var c=CabinRooms.level(shell);
                if(tickCount%10==0 || !chunkPosition().equals(r.chunk)) {
                    CabinRooms.ticket(c,new net.minecraft.world.level.ChunkPos(r.core));
                    if(r.visitors.stream().anyMatch(id->c.getPlayerByUUID(id)!=null))CabinRooms.follow(shell,chunkPosition());
                    d.locate(r,this);
                }
            }
        }
        ServerPlayer captain=null;
        for(Entity passenger:getPassengers()) if(passenger instanceof ServerPlayer rider) {
            if(isOwner(rider)) captain=rider;
            if(walking(rider)) walk(rider);
        }
        // 複数人搭乗中のサーバー終了などでバニラが乗員を復元しない場合も、空席を回収する。
        if(tickCount%20==0) for(Guest g:GUESTS) entityData.get(g.id).ifPresent(id->{
            if(getPassengers().stream().noneMatch(r->r.getUUID().equals(id))) entityData.set(g.id,Optional.empty());
        });
        if(captain==null && ownerWalking()) entityData.set(ON_DECK,false);
        ServerPlayer pilot=engineOn() && !ownerWalking()?captain:null;
        double altitude=AstroDock.altitude(level(),getY());
        // 接地・無人・静止中は10 tickごとの支持面確認だけにする。
        if (!isVehicle() && onGround() && getDeltaMovement().lengthSqr()==0 && body.momentum.lengthSquared()==0
                && tickCount%10!=0 && !PlanetTravel.approaching(level().dimension(),position())) return;
        boolean roll=pilot!=null && pilot.getOffhandItem().is(ShipParts.WRENCH.get());
        float lateral=pilot==null?0:pilot.xxa;
        float yaw=roll?0:lateral;
        if (hull.hasHelm && pilot!=null && pilot.zza!=0 && lateral==0 && !roll)
            yaw=body.helmYaw(hull,getYRot(),pilot.getYRot());
        Vec3 velocity=body.step(hull,getDeltaMovement(),altitude,pilot==null?0:pilot.zza,
                pilot==null?0:pilot.getXRot()*Mth.DEG_TO_RAD, yaw, roll?lateral:0, pilot!=null && pilot.jumping);
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
        crossBoundary((ServerLevel)level());
    }
    Ship crossBoundary(ServerLevel from) {
        var passage=PlanetTravel.at(from.dimension(),position());
        if (passage==null) {
            if (isVehicle() && getDeltaMovement().y>0) OrbitArrival.prefetch(from,position(),tickCount);
            return null;
        }
        ServerLevel to=from.getServer().getLevel(passage.dimension());
        if (to==null || !to.getWorldBorder().isWithinBounds(BlockPos.containing(passage.position()))) return null;
        if (isVehicle() && to.dimension()==AstroDock.ORBIT && !OrbitArrival.ready(to,passage.position())) return null;
        Ship next=new Ship(ShipParts.SHIP.get(),to);next.body.copyFrom(body);
        if (passage.frame()!=null) {
            next.body.attitude.premul(passage.frame()).normalize();
            passage.frame().transform(next.body.momentum);
        }
        System.arraycopy(nextShot,0,next.nextShot,0,nextShot.length);next.configure(hull,owner);next.syncAttitude();
        Vec3 at=passage.position();next.moveTo(at.x,at.y,at.z,next.body.heading(getYRot()),0);
        next.setDeltaMovement(passage.rotate(getDeltaMovement()));
        to.getChunkAt(next.blockPosition());
        next.entityData.set(ENGINE_ON,engineOn());
        next.entityData.set(FEET,new Vector3f(entityData.get(FEET)));next.entityData.set(ON_DECK,ownerWalking());
        for(Guest g:GUESTS) {
            next.entityData.set(g.id,entityData.get(g.id));next.entityData.set(g.feet,new Vector3f(entityData.get(g.feet)));
        }
        if (!to.noCollision(next,next.getBoundingBox()) || !to.addFreshEntity(next)) {setDeltaMovement(Vec3.ZERO);return null;}
        next.roomId=roomId;
        if(roomId!=0){var d=CabinRooms.data(from);var r=d.get(roomId);if(r!=null){d.locate(r,next);if(!r.visitors.isEmpty())CabinRooms.follow(to,next.chunkPosition());}}
        // 降船時に元の席情報が消えるので、先に全員分を転送先に複製する。
        for(Entity rider:List.copyOf(getPassengers())) if(rider instanceof ServerPlayer player) {
            Vec3 look=passage.rotate(player.getLookAngle());
            Vec3 arrival=next.walking(player)?next.worldFeet(player):next.position().add(0,next.getPassengersRidingOffset(),0);
            player.stopRiding();player.teleportTo(to,arrival.x,arrival.y,arrival.z,passage.yaw(look,player.getYRot()),passage.pitch(look,player.getXRot()));
            player.startRiding(next,true);next.positionRider(player);player.fallDistance=0;
        }
        discard();return next;
    }
    void dock(ServerPlayer p) {
        if (ownerWalking() || !p.getUUID().equals(owner) || !p.getAbilities().mayBuild || hull==null || isRemoved()) return;
        if (getPassengers().stream().anyMatch(r->r!=p)) {p.displayClientMessage(Component.translatable("hint.astrodock.crew_aboard"),true);return;}
        if (getDeltaMovement().length()>0.45) {ShipWrench.message(p,"減速してから再設置してください");return;}
        ServerLevel l=(ServerLevel)level();
        var cabin=CabinRooms.level(l);var room=room();
        if(roomId!=0 && room==null){ShipWrench.message(p,"船室データを読み込めません");return;}
        Map<BlockPos,BlockState> local=new LinkedHashMap<>();
        if(room!=null) {
            if(!room.visitors.isEmpty()){ShipWrench.message(p,"作業船室の乗員が戻ってから再設置してください");return;}
            var cells=room.cells(cabin);
            if(cells.size()>ShipWrench.MAX_PARTS || !cabin.getBlockState(room.core).is(ShipParts.CORE.get()) || HullDoors.exposed(cells,room.lo,room.hi)!=null){
                ShipWrench.message(p,"船室を確認してください（256ブロックまで、コア必須、外壁は艦外用ドアのみ）");return;
            }
            for(var pos:BlockPos.betweenClosed(room.lo.atY(room.hi.getY()+1),room.hi.above(2))) if(!cabin.isEmptyBlock(pos)) {
                ShipWrench.message(p,"選択範囲より上のブロックを片付けてください");return;
            }
            if(!cabin.getEntities(this,new AABB(room.lo,room.hi.offset(1,3,1))).isEmpty()) {ShipWrench.message(p,"船室内のアイテムや生物を先に回収してください");return;}
            cells.forEach((pos,state)->local.put(pos.subtract(room.core),state));
        } else for(var part:hull.parts)local.put(part.pos(),part.state());
        Rotation rot=Rotation.values()[Math.floorMod(Math.round(getYRot()/90f),4)];
        Vec3 center=new Vec3(hull.cx,0,hull.cz).yRot(-Math.round(getYRot()/90f)*90*Mth.DEG_TO_RAD);
        BlockPos core=BlockPos.containing(getX()-center.x,getY()-hull.floor+0.5,getZ()-center.z);
        Map<BlockPos,BlockState> placement=new LinkedHashMap<>();
        for (var part:local.entrySet()) {
            BlockPos pos=core.offset(part.getKey().rotate(rot));
            if (l.isOutsideBuildHeight(pos) || !l.hasChunkAt(pos) || !l.getWorldBorder().isWithinBounds(pos) || !l.mayInteract(p,pos) || !l.isEmptyBlock(pos)) {
                ShipWrench.message(p,"再設置できません。建築可能な空間に移動してください");return;
            }
            placement.put(pos,part.getValue().rotate(rot));
        }
        List<BlockSnapshot> snapshots=new ArrayList<>();
        for(var e:placement.entrySet()) {
            snapshots.add(BlockSnapshot.create(l.dimension(),l,e.getKey())); l.setBlock(e.getKey(),e.getValue(),2|16|32);
        }
        if (ForgeEventFactory.onMultiBlockPlace(p,snapshots,Direction.UP)) {
            for(BlockSnapshot snapshot:snapshots) snapshot.restore(true,false);
            ShipWrench.message(p,"保護された場所には再設置できません");return;
        }
        if(room!=null) {
            for(var pos:local.keySet()) {
                var dest=core.offset(pos.rotate(rot));
                if(!l.getBlockState(dest).equals(placement.get(dest)) || !CabinRooms.tryCopy(cabin,room.core.offset(pos),l,dest)) {
                    for(var placed:placement.keySet()) CabinRooms.erase(l,placed);
                    ShipWrench.message(p,"機械データを復元できません。船室の内容は変更していません");return;
                }
            }
            for(var pos:local.keySet()) CabinRooms.erase(cabin,room.core.offset(pos));
            room.walls(cabin,false);CabinRooms.data(l).forget(room);
        }
        placement.forEach((pos,state)->l.updateNeighborsAt(pos,state.getBlock()));
        boolean riding=p.getVehicle()==this;
        ejectPassengers(); if(riding) p.teleportTo(core.getX()+0.5,core.getY()+hull.floor+hull.height,core.getZ()+0.5);
        discard();ShipWrench.message(p,"船体を再設置しました");
    }
    @Override protected void defineSynchedData() { entityData.define(ROTATION,new Quaternionf());
        entityData.define(ENGINE_ON,true);entityData.define(ON_DECK,false);entityData.define(FEET,new Vector3f());
        for(Guest g:GUESTS) {entityData.define(g.id,Optional.empty());entityData.define(g.feet,new Vector3f());}
    }
    private void syncAttitude() { previous.set(body.attitude);entityData.set(ROTATION,new Quaternionf(body.attitude)); }
    @Override protected void addAdditionalSaveData(CompoundTag t) {
        t.putLongArray("nextShot",nextShot);t.putInt("room",roomId);
        if(hull!=null) t.put("parts",hull.save()); if(owner!=null)t.putUUID("owner",owner);
        t.putBoolean("engineOn",engineOn());
        if(ownerWalking()) {var feet=entityData.get(FEET);t.putFloat("deckX",feet.x);t.putFloat("deckY",feet.y);t.putFloat("deckZ",feet.z);}
        ListTag guests=new ListTag();
        for(Guest g:GUESTS) entityData.get(g.id).ifPresent(id->{
            CompoundTag row=new CompoundTag();Vector3f f=entityData.get(g.feet);
            row.putUUID("id",id);row.putFloat("x",f.x);row.putFloat("y",f.y);row.putFloat("z",f.z);guests.add(row);
        });
        t.put("guests",guests);
        Quaternionf q=body.attitude;
        t.putFloat("qx",q.x); t.putFloat("qy",q.y); t.putFloat("qz",q.z); t.putFloat("qw",q.w);
        t.putFloat("lx",body.momentum.x); t.putFloat("ly",body.momentum.y); t.putFloat("lz",body.momentum.z);
    }
    @Override protected void readAdditionalSaveData(CompoundTag t) {
        if(!t.contains("parts") || !t.hasUUID("owner")) return;
        var times=t.getLongArray("nextShot");System.arraycopy(times,0,nextShot,0,Math.min(times.length,nextShot.length));
        roomId=t.getInt("room");
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
        entityData.set(ENGINE_ON,!t.contains("engineOn") || t.getBoolean("engineOn"));
        var feet=new Vector3f(t.getFloat("deckX"),t.getFloat("deckY"),t.getFloat("deckZ"));
        boolean restore=t.contains("deckX") && feet.isFinite() && loaded.deck.canStand(feet.x,feet.y,feet.z);
        if(restore) entityData.set(FEET,feet);
        entityData.set(ON_DECK,restore);
        for(Guest g:GUESTS) entityData.set(g.id,Optional.empty());
        for(Tag entry:t.getList("guests",Tag.TAG_COMPOUND)) {
            CompoundTag row=(CompoundTag)entry;Guest g=vacant();if(g==null) break;
            if(!row.hasUUID("id")) continue;
            UUID id=row.getUUID("id");Vector3f f=new Vector3f(row.getFloat("x"),row.getFloat("y"),row.getFloat("z"));
            if(!id.equals(owner) && slot(id)==null && f.isFinite() && loaded.deck.canStand(f.x,f.y,f.z)) {
                entityData.set(g.feet,f);entityData.set(g.id,Optional.of(id));
            }
        }
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void writeSpawnData(FriendlyByteBuf b) {
        b.writeUUID(owner);b.writeVarInt(hull.parts.size());for(Hull.Part p:hull.parts){b.writeBlockPos(p.pos());b.writeVarInt(Block.getId(p.state()));b.writeDouble(p.mass());}
    }
    @Override public void readSpawnData(FriendlyByteBuf b) {
        UUID captain=b.readUUID();int n=b.readVarInt(); if(n<1 || n>ShipWrench.MAX_PARTS) throw new IllegalArgumentException("船体サイズが不正です");
        List<Hull.Part> parts=new ArrayList<>();for(int i=0;i<n;i++) parts.add(new Hull.Part(b.readBlockPos(),Block.stateById(b.readVarInt()),b.readDouble()));
        configure(new Hull(parts),captain);
    }
}
