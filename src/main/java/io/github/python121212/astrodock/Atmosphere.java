package io.github.python121212.astrodock;

/** 描画と物理が共有する、地上を基準にした高度モデル。 */
public final class Atmosphere {
    public static final int SPACE = 5000;

    public enum Band {
        LOWER(200, 320), STRATOSPHERE(700, 800), THERMOSPHERE(1800, 2000),
        EXOSPHERE(3200, 3500);

        public final int prepareAt, startsAt;
        Band(int prepareAt, int startsAt) {
            this.prepareAt = prepareAt;
            this.startsAt = startsAt;
        }
    }

    private static final Band[] BANDS = Band.values();

    public static Band active(double y) {
        for (int i = BANDS.length - 1; i >= 0; i--) if (y >= BANDS[i].startsAt) return BANDS[i];
        return null;
    }

    public static Band upcoming(double y) {
        for (Band band : BANDS) if (y < band.startsAt) return y >= band.prepareAt ? band : null;
        return null;
    }

    public static double ramp(double y, double start, double end) {
        return Math.max(0, Math.min(1, (y - start) / (end - start)));
    }

    public static double liftLoss(double y) { return ramp(y, 1200, 1500); }
    public static double rarefaction(double y) { return ramp(y, 2000, SPACE); }
    public static double gravityLoss(double y) { return 0.98 * rarefaction(y); }
    public static double muffling(double y) { return ramp(y, 3500, SPACE); }
    public static int hypoxiaInterval(double y) {
        return y < 1500 ? 0 : 80 - (int)(60 * ramp(y, 1500, 2000));
    }

    private Atmosphere() {}
}
