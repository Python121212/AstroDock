package io.github.python121212.astrodock.client;

import io.github.python121212.astrodock.ship.Ship;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 搭乗中に周囲256m以内の他艦を探知してHUDに表示するレーダー。 */
@Mod.EventBusSubscriber(modid = "astrodock", value = Dist.CLIENT)
public final class RadarHud {
    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !(player.getVehicle() instanceof Ship own)) return;

        Ship nearest = null;
        double bestDist2 = 256.0 * 256.0;
        for (var other : player.level().getEntitiesOfClass(Ship.class, own.getBoundingBox().inflate(256))) {
            if (other == own) continue;
            double d2 = other.distanceToSqr(own);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                nearest = other;
            }
        }
        if (nearest == null) return;
        int dist = (int) Math.round(Math.sqrt(bestDist2));
        double dx = nearest.getX() - own.getX();
        double dz = nearest.getZ() - own.getZ();
        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "東" : "西") : (dz > 0 ? "南" : "北");
        event.getGuiGraphics().drawString(mc.font, "[レーダー] 最寄りの艦船: " + dist + "m (" + dir + ")", 8, 34, 0x7FE7FF);
    }

    private RadarHud() {}
}
