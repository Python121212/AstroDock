package io.github.python121212.astrodock;

import net.minecraft.world.phys.Vec3;

/** 宇宙空間における太陽の活動（太陽風・コロナ加熱・プロミネンス周期）を計算する。 */
public final class SolarActivity {
    public static boolean prominenceActive(long gameTime) {
        return (gameTime % 2400L) < 400L;
    }

    public static double coronaHeat(double altitude, long gameTime) {
        if (altitude < Atmosphere.SPACE) return 0;
        double base = 0.08 + 0.12 * Math.min(1.0, (altitude - Atmosphere.SPACE) / 3000.0);
        return prominenceActive(gameTime) ? base * 1.8 : base;
    }

    public static Vec3 solarWind(double altitude, long gameTime, double mass) {
        if (altitude < Atmosphere.SPACE) return Vec3.ZERO;
        double strength = (prominenceActive(gameTime) ? 0.0045 : 0.0018) / Math.max(1.0, Math.sqrt(mass) * 0.25);
        double angle = (gameTime % 24000L) * (Math.PI * 2.0 / 24000.0);
        return new Vec3(Math.cos(angle) * strength, 0, Math.sin(angle) * strength);
    }

    private SolarActivity() {}
}
