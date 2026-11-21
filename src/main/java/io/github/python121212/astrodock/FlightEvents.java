package io.github.python121212.astrodock;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "astrodock")
public final class FlightEvents {
    private static final UUID GRAVITY = UUID.fromString("0645b716-b2ac-438b-ba2d-b7eb28ca4661");

    private record Sample(Level level, Vec3 position, int tick) {}
    private static final Map<ServerPlayer, Sample> approach = new WeakHashMap<>();

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        Level level = p.level();
        boolean supported = AstroDock.supported(level);
        double y = AstroDock.altitude(level, p.getY());
        if (p instanceof ServerPlayer sp) {
            gravity(sp, supported ? Atmosphere.gravityLoss(y) : 0);
            if (!supported || !sp.isAlive()) return;
            if (!sp.isPassenger() && crossBoundary(sp, y)) return;
            int interval = Atmosphere.hypoxiaInterval(y);
            if (sp.getVehicle() instanceof io.github.python121212.astrodock.ship.Ship) return;
            if (interval > 0 && !sp.isCreative() && !sp.isSpectator()
                    && !sp.isHolding(AstroDock.CAPSULE.get()) && sp.tickCount % interval == 0) {
                sp.hurt(sp.damageSources().drown(), 2);
            }
        }
        if (!supported || p.isPassenger() || p.isSpectator()) return;
        if (p.isFallFlying() && y >= 1500) p.stopFallFlying();
        // プレイヤー移動はローカル側で積分。サーバーで二重加速させない。
        if (!level.isClientSide || !p.isLocalPlayer()) return;
        Vec3 v = p.getDeltaMovement();
        if (p.isFallFlying() && y > 1200) {
            double loss = Atmosphere.liftLoss(y);
            v = v.multiply(1 - 0.08 * loss, 1, 1 - 0.08 * loss).add(0, -0.08 * loss, 0);
        } else if (y > 2000 && !p.onGround() && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying) {
            // バニラの空気抵抗 (水平 0.91、垂直 0.98) の一部を相殺する。
            double thin = Atmosphere.rarefaction(y);
            v = v.multiply(1 / (1 - 0.089 * thin), 1 / (1 - 0.019 * thin), 1 / (1 - 0.089 * thin));
        }
        if (p.getUseItem().is(AstroDock.CAPSULE.get())) {
            v = v.add(p.getLookAngle().scale(0.22));
            if (v.lengthSqr() > 3.8 * 3.8) v = v.normalize().scale(3.8);
        }
        p.setDeltaMovement(v);
    }

    private static void gravity(ServerPlayer p, double loss) {
        AttributeInstance a = p.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (a == null) return;
        // 高度が少し変わるたびに属性パケットを送らない。
        double amount = -Math.round(loss * 100) / 100.0;
        AttributeModifier old = a.getModifier(GRAVITY);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) a.removeModifier(GRAVITY);
        if (amount != 0) a.addTransientModifier(new AttributeModifier(GRAVITY, "AstroDock gravity", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static boolean crossBoundary(ServerPlayer p, double altitude) {
        boolean orbit = p.level().dimension() == AstroDock.ORBIT;
        if (orbit == (altitude >= Atmosphere.SPACE)) {
            if (Math.abs(altitude - Atmosphere.SPACE) < 32) {
                approach.put(p, new Sample(p.level(), p.position(), p.tickCount));
            } else if (!approach.isEmpty()) approach.remove(p);
            return false;
        }
        ServerLevel dest = p.serverLevel().getServer().getLevel(orbit ? Level.OVERWORLD : AstroDock.ORBIT);
        if (dest == null) return false;
        Vec3 velocity = p.getDeltaMovement();
        // サーバー上のプレイヤー速度はクライアントの飛行入力を含まないため、
        // 境界直前に受信した移動量を優先する。コマンドでの長距離転移は除外。
        Sample last = approach.remove(p);
        if (last != null && last.level == p.level() && p.tickCount == last.tick + 1) {
            Vec3 delta = p.position().subtract(last.position);
            if (delta.lengthSqr() <= 3.9 * 3.9) velocity = delta;
        }
        p.teleportTo(dest, p.getX(), orbit ? altitude : altitude - Atmosphere.SPACE, p.getZ(), p.getYRot(), p.getXRot());
        p.setDeltaMovement(velocity);
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
        p.fallDistance = 0;
        return true;
    }
}
