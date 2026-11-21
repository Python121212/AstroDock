package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** バニラ投射物の衝突判定・Forge の命中イベントを使う。ブロックの破壊や着火はしない。 */
public final class ShipShot extends ThrowableItemProjectile {
    private final Set<UUID> friendly=new HashSet<>();
    private Entity locked;
    private long expires;
    public ShipShot(EntityType<? extends ShipShot> type,Level level) {super(type,level);}
    private ShipWeapon weapon() {
        return getType()==ShipParts.TORPEDO.get()?ShipWeapon.TORPEDO:getType()==ShipParts.MISSILE.get()?ShipWeapon.MISSILE:ShipWeapon.CANNON;
    }
    @Override protected Item getDefaultItem(){return weapon().ammo;}
    @Override protected float getGravity(){return 0;}
    @Override public boolean canChangeDimensions(){return false;}
    void launch(Ship ship,ServerPlayer p,Vec3 origin,Vec3 aim) {
        setOwner(p);setPos(origin);setDeltaMovement(aim.scale(weapon().speed));expires=level().getGameTime()+weapon().lifetime;
        friendly.add(ship.getUUID());ship.getPassengers().forEach(e->friendly.add(e.getUUID()));
        if(weapon()==ShipWeapon.MISSILE) {
            Vec3 eye=p.getEyePosition(),end=eye.add(aim.scale(48));
            if(loaded(eye,end)) {
                var wall=level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                var hit=ProjectileUtil.getEntityHitResult(level(),this,eye,wall.getLocation(),new AABB(eye,end).inflate(1),this::eligible);
                if(hit!=null)locked=hit.getEntity();
            }
        }
    }
    private boolean loaded(Vec3 a,Vec3 b) {
        return level().hasChunksAt(BlockPos.containing(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z)).atY(level().getMinBuildHeight()),
                BlockPos.containing(Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z)).atY(level().getMinBuildHeight()));
    }
    private boolean eligible(Entity e) {
        if(!(getOwner() instanceof ServerPlayer p) || e==p || e.level()!=level() || !e.isAlive() || e.isSpectator() || friendly.contains(e.getUUID()) || p.isAlliedTo(e))return false;
        if(e instanceof Ship s)return !s.isOwner(p) && p.serverLevel().getServer().isPvpAllowed();
        if(e instanceof Player other && (other.isCreative() || !p.canHarmPlayer(other) || !p.serverLevel().getServer().isPvpAllowed()))return false;
        return e instanceof LivingEntity && !friendly.contains(e.getRootVehicle().getUUID());
    }
    @Override protected boolean canHitEntity(Entity e){return !level().isClientSide && eligible(e) && super.canHitEntity(e);}
    @Override public void tick() {
        var w=weapon();var dir=getDeltaMovement().normalize();
        if(!level().isClientSide) {
            if(level().getGameTime()>=expires || tickCount>=w.lifetime || getOwner()==null){discard();return;}
            if(locked!=null) {
                if(!eligible(locked))locked=null;
                else dir=dir.lerp(locked.getBoundingBox().getCenter().subtract(position()).normalize(),.2).normalize();
            }
        } else level().addParticle(w.trail,getX(),getY(),getZ(),0,0,0);
        // 水中でも軌道の真空でも同じゲーム速度。チャンクの強制ロードは行わない。
        setDeltaMovement(dir.scale(w.speed));
        if(!level().isClientSide && !loaded(position(),position().add(getDeltaMovement()))){discard();return;}
        super.tick();
    }
    @Override protected void onHit(HitResult hit) {
        if(!(level() instanceof ServerLevel l))return;
        if(hit instanceof EntityHitResult h && eligible(h.getEntity())) {
            Entity e=h.getEntity();
            if(e instanceof Ship s)s.setDeltaMovement(s.getDeltaMovement().scale(.5));
            else e.hurt(damageSources().thrown(this,getOwner()),weapon().damage);
        }
        var at=hit.getLocation();l.sendParticles(weapon().trail,at.x,at.y,at.z,14,.22,.22,.22,.04);discard();
    }
}
