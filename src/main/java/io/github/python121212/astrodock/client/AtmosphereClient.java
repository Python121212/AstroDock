package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.python121212.astrodock.*;
import io.github.python121212.astrodock.Atmosphere.Band;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="astrodock", value=Dist.CLIENT)
public final class AtmosphereClient {
    private static final EnumMap<Band,SkyLayer> layers = new EnumMap<>(Band.class);
    private static Band visible, pending;
    private static FlightSound wind, engine;
    private static double altitude(Entity e, double y) {
        return e != null && AstroDock.supported(e.level()) ? AstroDock.altitude(e.level(), y) : -1;
    }
    // 高度帯が変わったときだけ生成・解放。全帯のジオメトリを一括生成しない。
    private static SkyLayer select(double y) {
        Band active = Atmosphere.active(y), next = Atmosphere.upcoming(y);
        if (active != visible || pending != next) {
            layers.keySet().removeIf(b -> b != active && b != next);
            if (active != null) layers.computeIfAbsent(active, SkyLayer::new);
            if (next != null) layers.computeIfAbsent(next, SkyLayer::new);
            visible = active; pending = next;
        }
        return active == null ? null : layers.get(active);
    }

    @SubscribeEvent public static void sky(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var camera = e.getCamera();
        double y = altitude(camera.getEntity(), camera.getPosition().y);
        SkyLayer layer = select(y);
        if (layer == null || camera.getFluidInCamera() != FogType.NONE) return;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull(); RenderSystem.depthMask(false);
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            layer.draw(b, e.getPoseStack().last().pose(), y);
            BufferUploader.drawWithShader(b.end());
            if (camera.getEntity().level().dimension()==AstroDock.ORBIT) {
                OrbitalGlobe.EARTH.draw(e);OrbitalGlobe.RUBRA.draw(e);
            }
        } finally {
            RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
    }

    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e) {
        var c = e.getCamera();
        double y = altitude(c.getEntity(), c.getPosition().y);
        if (y <= 320 || c.getFluidInCamera() != FogType.NONE) return;
        float air = (float)(1 - Atmosphere.ramp(y, 320, 5000));
        e.setRed(e.getRed()*air*air); e.setGreen(e.getGreen()*air*air); e.setBlue(e.getBlue()*air);
    }

    private static Vec3 motion(Entity e) { return e.getRootVehicle().getDeltaMovement(); }
    @SubscribeEvent public static void shake(ViewportEvent.ComputeCameraAngles e) {
        Entity entity = e.getCamera().getEntity();
        double y = altitude(entity, e.getCamera().getPosition().y);
        if (y < 800 || y >= 5000) return;
        double strength = Atmosphere.ramp(y, 800, 1100) * (1 - Atmosphere.rarefaction(y))
                * Atmosphere.ramp(motion(entity).length(), 0.6, 3);
        e.setRoll(e.getRoll() + (float)(0.65 * strength * Math.sin((entity.tickCount + e.getPartialTick()) * 1.7)));
    }

    @SubscribeEvent public static void effects(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance(); var p = mc.player;
        if (p == null || p.level().dimension()!=AstroDock.ORBIT) { OrbitalGlobe.EARTH.release();OrbitalGlobe.RUBRA.release(); }
        if (p == null) { select(-1); wind = engine = null; return; }
        double y = altitude(p, p.getY());
        if (mc.isPaused() || y < 320) return;
        Vec3 v = motion(p);
        float speed = (float)Atmosphere.ramp(v.length(), 0.5, 3);
        if (FlightSound.audible(p, y, false) && (wind == null || !mc.getSoundManager().isActive(wind))) {
            mc.getSoundManager().play(wind = new FlightSound(p, false));
        }
        if (FlightSound.audible(p, y, true) && (engine == null || !mc.getSoundManager().isActive(engine))) {
            mc.getSoundManager().play(engine = new FlightSound(p, true));
        }
        if (y >= 2000 && y < 5000 && v.y < -0.7) {
            int count = (int)Math.ceil(4 * speed * (1 - Atmosphere.rarefaction(y)));
            for (int i = 0; i < count; i++) mc.level.addParticle(ParticleTypes.SMALL_FLAME,
                    p.getX() + mc.level.random.nextDouble() - 0.5, p.getY() + 0.3, p.getZ() + mc.level.random.nextDouble() - 0.5,
                    -v.x * 0.12, 0.08, -v.z * 0.12);
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) {
        var mc = Minecraft.getInstance(); var p = mc.player;
        if (mc.options.hideGui || p == null || !AstroDock.supported(p.level())) return;
        double y = AstroDock.altitude(p.level(), p.getY());
        if (p.level().dimension()==AstroDock.ORBIT) {
            int remaining=(int)Math.max(0, Math.ceil(p.position().distanceTo(PlanetTravel.CENTER)-PlanetTravel.ENTRY));
            e.getGuiGraphics().drawString(mc.font,Component.translatable("hud.astrodock.rubra_distance",remaining),8,20,0xffb98e);
        } else if (p.level().dimension()==AstroDock.RUBRA) {
            e.getGuiGraphics().drawString(mc.font,Component.translatable("hud.astrodock.rubra_surface"),8,20,0xffb98e);
        }
        if (y >= 200) e.getGuiGraphics().drawString(mc.font, Component.translatable("hud.astrodock.altitude", (long)Math.floor(y)), 8, 8, 0xdce8ff);
    }
}
