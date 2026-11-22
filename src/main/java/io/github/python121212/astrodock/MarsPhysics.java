package io.github.python121212.astrodock;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 各惑星固有の重力（水星0.38G〜木星2.53G〜冥王星0.06G）とジャンプ力を適用する。 */
@Mod.EventBusSubscriber(modid = "astrodock")
public final class MarsPhysics {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        double g = PlanetTravel.gravityFor(entity.level().dimension());
        if (g == 1.0) return;

        var v = entity.getDeltaMovement();
        if (!entity.onGround() && !entity.isInWater()) {
            if (g < 1.0 && v.y < 0) {
                entity.setDeltaMovement(v.x, v.y * (0.78 + 0.22 * g), v.z);
                entity.fallDistance *= (float) g;
            } else if (g > 1.0 && v.y < 0) {
                entity.setDeltaMovement(v.x, v.y * Math.min(1.12, 1.0 + (g - 1.0) * 0.06), v.z);
            }
        }
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0 && g < 0.7) {
            int amp = g < 0.2 ? 3 : 1;
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 35, amp, true, false, false));
        }
    }

    private MarsPhysics() {}
}
