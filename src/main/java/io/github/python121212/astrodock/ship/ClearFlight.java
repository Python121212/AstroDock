package io.github.python121212.astrodock.ship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** 空と証明できる場合だけ詳細衝突を省略。不明なチャンクをロードしない。 */
final class ClearFlight {
    static boolean clear(ServerLevel level, Ship ship, AABB swept) {
        var border=level.getWorldBorder();
        if (swept.minX<=border.getMinX() || swept.maxX>=border.getMaxX()
                || swept.minZ<=border.getMinZ() || swept.maxZ>=border.getMaxZ()) return false;
        // 隣接セルへ張り出すフェンス・ピストンなども含めて調べる。
        int low=Math.max(level.getMinBuildHeight(), Mth.floor(swept.minY)-1);
        int high=Math.min(level.getMaxBuildHeight()-1, Mth.floor(swept.maxY)+1);
        if (low<=high) {
            int first=level.getSectionIndex(low), last=level.getSectionIndex(high);
            int x0=(Mth.floor(swept.minX)-1)>>4, x1=(Mth.floor(swept.maxX)+1)>>4;
            int z0=(Mth.floor(swept.minZ)-1)>>4, z1=(Mth.floor(swept.maxZ)+1)>>4;
            for(int x=x0;x<=x1;x++) for(int z=z0;z<=z1;z++) {
                var chunk=level.getChunkSource().getChunkNow(x,z);
                if(chunk==null) return false;
                for(int s=first;s<=last;s++) if(!chunk.getSection(s).hasOnlyAir()) return false;
            }
        }
        return level.getEntityCollisions(ship,swept).isEmpty();
    }
}
