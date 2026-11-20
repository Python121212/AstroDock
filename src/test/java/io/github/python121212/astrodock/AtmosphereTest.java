package io.github.python121212.astrodock;

import io.github.python121212.astrodock.Atmosphere.Band;

/** ゲームを起動せずに境界の直前・ちょうど・降下方向を確認する。 */
public class AtmosphereTest {
    public static void main(String[] args) {
        Band prev = null;
        for (Band band : Band.values()) {
            check(Atmosphere.upcoming(band.prepareAt - 0.01) == null);
            check(Atmosphere.upcoming(band.prepareAt) == band);
            check(Atmosphere.active(band.prepareAt) == prev);
            check(Atmosphere.active(band.startsAt) == band);
            check(Atmosphere.upcoming(band.startsAt) == null);
            check(Atmosphere.active(band.startsAt - 0.01) == prev);
            check(Atmosphere.upcoming(band.startsAt - 0.01) == band);
            prev = band;
        }
        check(Atmosphere.active(5500) == Band.EXOSPHERE && Atmosphere.upcoming(5500) == null);
        check(Atmosphere.gravityLoss(2000) == 0 && Atmosphere.gravityLoss(5000) == 0.98);
        check(Atmosphere.muffling(3500) == 0 && Atmosphere.muffling(9000) == 1);
        check(Atmosphere.liftLoss(1200) == 0 && Atmosphere.liftLoss(1350) == 0.5 && Atmosphere.liftLoss(1500) == 1);
        check(Atmosphere.hypoxiaInterval(1499) == 0 && Atmosphere.hypoxiaInterval(1500) == 80 && Atmosphere.hypoxiaInterval(2000) == 20);
    }
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
