package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.Atmosphere;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 1 tick の並進・ヨー剛体積分。機体のブロック数に依存するループはない。 */
public record ShipMotion(Vec3 velocity, double spin) {
    public ShipMotion step(Hull h, double y, float yaw, double throttle, double pitch, double helm) {
        double air=1-Atmosphere.rarefaction(y), thrust=0.8*throttle;
        double horizontal=thrust*Math.cos(pitch);
        Vec3 dv=new Vec3(0, -thrust*Math.sin(pitch),horizontal).yRot(-yaw*Mth.DEG_TO_RAD).scale(h.engines/h.mass);
        Vec3 v=velocity.scale(1-0.055*air).add(dv).add(0,-0.035*(1-Atmosphere.gravityLoss(y)),0);
        if (v.lengthSqr()>9) v=v.normalize().scale(3);
        double torque=h.engineArm*horizontal + helm*h.engines*0.65;
        return new ShipMotion(v,Mth.clamp(spin*(1-0.10*air)+torque/h.inertia*5,-8,8));
    }
}
