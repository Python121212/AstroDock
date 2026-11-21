package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.Atmosphere;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 姿勢は機体→世界。角運動量は世界座標で保持するため、無推力・真空なら保存される。 */
final class RigidBody {
    final Quaternionf attitude = new Quaternionf();
    final Vector3f momentum = new Vector3f();
    private final Vector3f tmp = new Vector3f();
    private static final float MAX_RATE = 8 * Mth.DEG_TO_RAD;

    Vec3 step(Hull h, Vec3 v, double altitude, float throttle, float lookPitch, float yaw, float roll) {
        return step(h,v,altitude,throttle,lookPitch,yaw,roll,false);
    }

    Vec3 step(Hull h, Vec3 v, double altitude, float throttle, float lookPitch, float yaw, float roll, boolean rising) {
        float air = (float)(1 - Atmosphere.rarefaction(altitude));
        float thrust = 0.8f * throttle;
        // 地表でも離陸できる偏向推力。機首の仰角を引き、二重に傾けない。
        float noseY = 2 * (attitude.y * attitude.z - attitude.w * attitude.x);
        float pitchError = lookPitch + (float)Math.asin(Mth.clamp(noseY, -1, 1));
        float fy = -thrust * Mth.sin(pitchError), fz = thrust * Mth.cos(pitchError);
        attitude.transform(tmp.set(0, fy, fz));
        double drag = 1 - 0.055 * air, a = h.engines / h.mass;
        // 垂直推進はワールドの上方向。長押し中は重力に対抗し、機体質量に応じて加速する。
        double lift=rising && h.engines>0 ? 0.8*a : a*tmp.y-0.035*(1-Atmosphere.gravityLoss(altitude));
        double x=v.x*drag+a*tmp.x, y=v.y*drag+lift, z=v.z*drag+a*tmp.z;
        double speed2=x*x+y*y+z*z, limit=speed2>9 ? 3/Math.sqrt(speed2) : 1;

        // Σ(r × F) は同じ推力を受けるエンジンの腕の和から一度で求められる。
        float steering = h.engines * 0.65f;
        tmp.set(h.armY*fz-h.armZ*fy + steering*Math.abs(throttle)*Mth.clamp(pitchError,-1,1),
                -h.armX*fz + steering*yaw, h.armX*fy + steering*roll);
        attitude.transform(tmp);
        momentum.mul(1-0.1f*air).add(tmp);
        if (momentum.lengthSquared()>1e-14f) {
            attitude.transformInverse(momentum,tmp);
            h.inverseInertia.transform(tmp); // 機体座標の角速度（rad/tick）
            float w2=tmp.lengthSquared();
            if (w2>MAX_RATE*MAX_RATE) {
                float scale=MAX_RATE/(float)Math.sqrt(w2);
                tmp.mul(scale); momentum.mul(scale);
            }
            // q' = 0.5 q ω（正規化した一次積分、固定時間刻み）。
            float aX=tmp.x*0.5f,aY=tmp.y*0.5f,aZ=tmp.z*0.5f;
            attitude.mul(aX,aY,aZ,1).normalize();
        } else momentum.zero();
        return new Vec3(x*limit,y*limit,z*limit);
    }
    float helmYaw(Hull hull, float current, float desired) {
        // 機首と視線の短い側の角度差を使う。回転速度をフィードバックして振れを抑える。
        attitude.transformInverse(momentum,tmp);
        hull.inverseInertia.transform(tmp);
        return Mth.clamp(Mth.wrapDegrees(current-desired)/40f - 6*tmp.y,-1,1);
    }
    float heading(float fallback) {
        float x=2*(attitude.x*attitude.z+attitude.y*attitude.w), z=1-2*(attitude.x*attitude.x+attitude.y*attitude.y);
        return x*x+z*z<1e-6f ? fallback : (float)(-Math.atan2(x,z)*Mth.RAD_TO_DEG);
    }
    void copyFrom(RigidBody other) { attitude.set(other.attitude); momentum.set(other.momentum); }
}
