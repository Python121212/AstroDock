package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.vertex.*;
import io.github.python121212.astrodock.Atmosphere;
import io.github.python121212.astrodock.Atmosphere.Band;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** 観測者中心の空。星は天体エンティティではなく、固定シードの小さな四角形。 */
final class SkyLayer {
    final Band band;
    private final float[][] details;
    private final int from, to, end;
    SkyLayer(Band band) {
        this.band = band;
        switch (band) {
            case LOWER -> { from = 0x659be2; to = 0x254681; end = 800; }
            case STRATOSPHERE -> { from = 0x254681; to = 0x11142d; end = 2000; }
            case THERMOSPHERE -> { from = 0x11142d; to = 0x040610; end = 3500; }
            case EXOSPHERE -> { from = 0x040610; to = 0x000000; end = Atmosphere.SPACE; }
            default -> throw new AssertionError();
        }
        boolean cloud = band == Band.STRATOSPHERE;
        details = new float[band == Band.LOWER ? 0 : cloud ? 16 : 240][];
        Random random = new Random(7442);
        for (int i = 0; i < details.length; i++) {
            double z = cloud ? random.nextDouble() * 0.25 - 0.12 : random.nextDouble() * 2 - 1;
            double phi = random.nextDouble() * 2 * Math.PI, r = Math.sqrt(1 - z*z);
            Vec3 n = new Vec3(r*Math.cos(phi), z, r*Math.sin(phi));
            Vec3 side = n.cross(new Vec3(0, 1, 0)).normalize();
            double size = 0.007 + random.nextDouble() * 0.018;
            Vec3 up = side.cross(n).scale(cloud ? 0.035 : size);
            side = side.scale(cloud ? 1.4 : size);
            Vec3 center = n.scale(12);
            float[] quad = details[i] = new float[12];
            for (int k = 0; k < 4; k++) {
                Vec3 point = center.add(side.scale(k == 0 || k == 3 ? -1 : 1)).add(up.scale(k < 2 ? -1 : 1));
                quad[k*3] = (float)point.x; quad[k*3+1] = (float)point.y; quad[k*3+2] = (float)point.z;
            }
        }
    }

    private static final int[][] CUBE = {{0,1,3,2},{4,5,7,6},{0,1,5,4},{2,3,7,6},{0,2,6,4},{1,3,7,5}};
    private static int blend(int shift, int a, int b, double t) {
        int x = (a >> shift) & 255, y = (b >> shift) & 255;
        return (int)(x + (y - x) * t);
    }
    void draw(BufferBuilder b, Matrix4f m, double y) {
        double t = Atmosphere.ramp(y, band.startsAt, end);
        int r = blend(16, from, to, t), g = blend(8, from, to, t), blue = blend(0, from, to, t);
        int alpha = (int)(255 * Atmosphere.ramp(y, 320, 2400));
        for (int[] face : CUBE) for (int i : face) {
            int h = (i & 2) == 0 ? -16 : 16;
            double light = h < 0 ? 1 : 0.64;
            b.vertex(m, (i & 1) == 0 ? -16 : 16, h, (i & 4) == 0 ? -16 : 16)
                    .color((int)(r*light), (int)(g*light), (int)(blue*light), alpha).endVertex();
        }
        boolean cloud = band == Band.STRATOSPHERE;
        int intensity = cloud ? (int)(38 * Math.sin(t * Math.PI)) : (int)(240 * Atmosphere.ramp(y, 2000, 4200));
        for (float[] q : details) for (int j = 0; j < 12; j += 3) {
            b.vertex(m, q[j], q[j+1], q[j+2]).color(215, 226, 250, intensity).endVertex();
        }
    }
}
