package io.github.python121212.astrodock;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 軌道への有人飛行だけを対象にする。チャンク生成完了をサーバースレッドで待たない。 */
public final class OrbitArrival {
    private static final int LEAD = 96;
    private static final int RADIUS = 2; // 傾いた最大サイズの艦の衝突枠も収める
    private static final TicketType<ChunkPos> ARRIVAL = TicketType.create("astrodock_orbit_arrival", Comparator.comparingLong(ChunkPos::toLong), 80);

    public static boolean nearLaunch(ResourceKey<Level> dimension, double y) {
        double limit = dimension == Level.OVERWORLD ? Atmosphere.SPACE : PlanetTravel.DEPARTURE_Y;
        return (dimension == Level.OVERWORLD || dimension == AstroDock.MARS) && y >= limit - LEAD && y < limit;
    }

    public static void prefetch(ServerLevel from, Vec3 p, int tick) {
        if (tick % 10 != 0 || !nearLaunch(from.dimension(),p.y)) return;
        // 実際の境界移動と同じ経度・緯度変換を使う。
        double line = from.dimension() == Level.OVERWORLD ? Atmosphere.SPACE : PlanetTravel.DEPARTURE_Y;
        var passage = PlanetTravel.at(from.dimension(),new Vec3(p.x,line,p.z));
        var orbit = from.getServer().getLevel(AstroDock.ORBIT);
        if (orbit != null) request(orbit, passage.position());
    }

    private static ChunkPos request(ServerLevel orbit, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        if (!orbit.getWorldBorder().isWithinBounds(pos)) return null;
        ChunkPos center = new ChunkPos(pos);
        orbit.getChunkSource().addRegionTicket(ARRIVAL,center,RADIUS,center);
        return center;
    }

    /** /tp や高速移動で予告帯を飛び越した場合も、元の空を表示したまま次 tick に再判定。 */
    public static boolean ready(ServerLevel orbit, Vec3 at) {
        ChunkPos c=request(orbit,at);
        if (c==null) return false;
        var source=orbit.getChunkSource();
        for(int dx=-RADIUS;dx<=RADIUS;dx++) for(int dz=-RADIUS;dz<=RADIUS;dz++)
            if(source.getChunkNow(c.x+dx,c.z+dz)==null) return false;
        return true;
    }
    private OrbitArrival() {}
}
