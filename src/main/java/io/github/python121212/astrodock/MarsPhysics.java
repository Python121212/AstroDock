package io.github.python121212.astrodock;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 火星（MARS）の低重力（約0.38G）とジャンプ力向上を適用する。 */
@Mod.EventBusSubscriber(modid = "astrodock")
public final class MarsPhysics {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().dimension() != AstroDock.MARS) return;

        // 落下中の重力を約38%に軽減し、落下ダメージも抑える
        var v = entity.getDeltaMovement();
        if (!entity.onGround() && !entity.isInWater() && v.y < 0) {
            entity.setDeltaMovement(v.x, v.y * 0.86, v.z);
            entity.fallDistance *= 0.45f;
        }
        // 火星の低重力によるジャンプ力向上
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 35, 1, true, false, false));
        }
    }

    private MarsPhysics() {}
}
