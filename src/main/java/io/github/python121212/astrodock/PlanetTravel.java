package io.github.python121212.astrodock;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 惑星ルブラ。通常の接近判定は座標だけを読み、ワールドやチャンクを参照しない。 */
public final class PlanetTravel {
    public static final Vec3 CENTER = new Vec3(1024, 384, 640);
    public static final int RADIUS = 96, ENTRY = 108, EXIT = 120;
    public static final int ARRIVAL_Y = 480, DEPARTURE_Y = 512;
    private static final int MAP_SCALE = 2048; // 地表地図のブロック数 / ラジアン

    public record Passage(ResourceKey<Level> dimension, Vec3 position, Quaternionf frame) {
        public Vec3 rotate(Vec3 v) {
            if (frame == null) return v; // 地球⇔宇宙は同じ座標軸
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

    public static Passage at(ResourceKey<Level> dimension, Vec3 p) {
        if (dimension==Level.OVERWORLD && p.y>=Atmosphere.SPACE)
            return new Passage(AstroDock.ORBIT,p.add(0,-Atmosphere.SPACE,0),null);
        if (dimension==AstroDock.ORBIT) {
            if (p.y<0) return new Passage(Level.OVERWORLD,p.add(0,Atmosphere.SPACE,0),null);
            double d2=CENTER.distanceToSqr(p);
            if (d2>ENTRY*ENTRY) return null;
            double dx=p.x-CENTER.x, dz=p.z-CENTER.z;
            double lon=Math.atan2(dx,dz), lat=Math.atan2(p.y-CENTER.y,Math.sqrt(dx*dx+dz*dz));
            return new Passage(AstroDock.RUBRA,new Vec3(lon*MAP_SCALE,ARRIVAL_Y,-lat*MAP_SCALE),localFrame(lon,lat).conjugate());
        }
        if (dimension==AstroDock.RUBRA && p.y>=DEPARTURE_Y) {
            double lon=p.x/MAP_SCALE, lat=-p.z/MAP_SCALE, ring=EXIT*Math.cos(lat);
            return new Passage(AstroDock.ORBIT,CENTER.add(ring*Math.sin(lon),EXIT*Math.sin(lat),ring*Math.cos(lon)),localFrame(lon,lat));
        }
        return null;
    }
    private static Quaternionf localFrame(double lon,double lat) {
        // 地表の +X=東、+Y=天頂、+Z=南 を宇宙側の軸へ写す。
        return new Quaternionf().rotationY((float)lon).rotateX((float)(Math.PI/2-lat));
    }
    public static boolean approaching(ResourceKey<Level> dimension,Vec3 p) {
        if (dimension==AstroDock.ORBIT) return p.y<32 || CENTER.distanceToSqr(p)<(ENTRY+32)*(ENTRY+32);
        return dimension==AstroDock.RUBRA ? p.y>DEPARTURE_Y-32 : dimension==Level.OVERWORLD && p.y>Atmosphere.SPACE-32;
    }
    private PlanetTravel() {}
}
