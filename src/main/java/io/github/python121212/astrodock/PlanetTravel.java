package io.github.python121212.astrodock;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 太陽系の各惑星への降下・帰還を扱う。通常の接近判定は座標だけを参照する。 */
public final class PlanetTravel {
    public static final Vec3 CENTER = new Vec3(1024, 384, 640);
    public static final int RADIUS = 96, ENTRY = 108, EXIT = 120;
    public static final int ARRIVAL_Y = 480, DEPARTURE_Y = 512;
    private static final int MAP_SCALE = 2048;

    public record Body(ResourceKey<Level> dimension, Vec3 center, double gravity) {}

    public static final List<Body> PLANETS = List.of(
        new Body(AstroDock.MERCURY, new Vec3(-640, 320, -480), 0.38),
        new Body(AstroDock.VENUS,   new Vec3(-320, 352,  640), 0.90),
        new Body(AstroDock.MARS,    CENTER,                    0.38),
        new Body(AstroDock.JUPITER, new Vec3(1800, 440, -900), 2.53),
        new Body(AstroDock.SATURN,  new Vec3(2400, 460,  800), 1.06),
        new Body(AstroDock.URANUS,  new Vec3(-1600, 420, 1400), 0.89),
        new Body(AstroDock.NEPTUNE, new Vec3(-2200, 440, -1400), 1.14),
        new Body(AstroDock.PLUTO,   new Vec3(3000, 480, -2000), 0.06)
    );

    public record Passage(ResourceKey<Level> dimension, Vec3 position, Quaternionf frame) {
        public Vec3 rotate(Vec3 v) {
            if (frame == null) return v;
            Vector3f r = frame.transform(v.toVector3f());
            return new Vec3(r.x, r.y, r.z);
        }
        public float yaw(Vec3 look, float fallback) {
            return frame==null || look.horizontalDistanceSqr()<1e-8 ? fallback : (float)(-Math.atan2(look.x,look.z)*Mth.RAD_TO_DEG);
        }
        public float pitch(Vec3 look, float fallback) {
            return frame==null ? fallback : (float)(-Math.asin(Mth.clamp(look.y,-1,1))*Mth.RAD_TO_DEG);
        }
    }

    public static double gravityFor(ResourceKey<Level> dimension) {
        for (Body b : PLANETS) if (b.dimension == dimension) return b.gravity;
        return 1.0;
    }

    public static Passage at(ResourceKey<Level> dimension, Vec3 p) {
        if (dimension==Level.OVERWORLD && p.y>=Atmosphere.SPACE)
            return new Passage(AstroDock.ORBIT,p.add(0,-Atmosphere.SPACE,0),null);
        if (dimension==AstroDock.ORBIT) {
            if (p.y<0) return new Passage(Level.OVERWORLD,p.add(0,Atmosphere.SPACE,0),null);
            for (Body b : PLANETS) {
                double d2=b.center.distanceToSqr(p);
                if (d2<=ENTRY*ENTRY) {
                    double dx=p.x-b.center.x, dz=p.z-b.center.z;
                    double lon=Math.atan2(dx,dz), lat=Math.atan2(p.y-b.center.y,Math.sqrt(dx*dx+dz*dz));
                    return new Passage(b.dimension,new Vec3(lon*MAP_SCALE,ARRIVAL_Y,-lat*MAP_SCALE),localFrame(lon,lat).conjugate());
                }
            }
            return null;
        }
        for (Body b : PLANETS) {
            if (dimension==b.dimension && p.y>=DEPARTURE_Y) {
                double lon=p.x/MAP_SCALE, lat=-p.z/MAP_SCALE, ring=EXIT*Math.cos(lat);
                return new Passage(AstroDock.ORBIT,b.center.add(ring*Math.sin(lon),EXIT*Math.sin(lat),ring*Math.cos(lon)),localFrame(lon,lat));
            }
        }
        return null;
    }

    private static Quaternionf localFrame(double lon,double lat) {
        return new Quaternionf().rotationY((float)lon).rotateX((float)(Math.PI/2-lat));
    }

    public static boolean approaching(ResourceKey<Level> dimension,Vec3 p) {
        if (dimension==AstroDock.ORBIT) {
            if (p.y<32) return true;
            for (Body b : PLANETS) if (b.center.distanceToSqr(p)<(ENTRY+32)*(ENTRY+32)) return true;
            return false;
        }
        for (Body b : PLANETS) if (dimension==b.dimension) return p.y>DEPARTURE_Y-32;
        return dimension==Level.OVERWORLD && p.y>Atmosphere.SPACE-32;
    }
    private PlanetTravel() {}
}
