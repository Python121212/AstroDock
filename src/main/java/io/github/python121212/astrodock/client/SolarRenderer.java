package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.python121212.astrodock.AstroDock;
import io.github.python121212.astrodock.SolarActivity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 宇宙空間（ORBIT）の空に太陽本体・コロナ・プロミネンスを描画する。 */
@Mod.EventBusSubscriber(modid = "astrodock", value = Dist.CLIENT)
public final class SolarRenderer {
    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != AstroDock.ORBIT) return;

        long time = mc.level.getGameTime();
        boolean flare = SolarActivity.prominenceActive(time);
        float pulse = (float) Math.sin(time * 0.08) * 0.6f;

        var pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(25f));
        pose.mulPose(Axis.XP.rotationDegrees(-32f));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        var mat = pose.last().pose();
        var buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 外側コロナ（淡い金色）
        quad(buf, mat, -95f, 18f + pulse, 0.28f, 1.0f, 0.72f, 0.25f, flare ? 0.35f : 0.20f);
        // 内側コロナ（明るいオレンジ）
        quad(buf, mat, -95f, 12f + pulse * 0.5f, 0.22f, 1.0f, 0.85f, 0.40f, 0.55f);
        // 太陽本体（白熱コア）
        quad(buf, mat, -95f, 7.5f, 0.1f, 1.0f, 0.98f, 0.85f, 1.0f);

        // プロミネンス（太陽縁のアーチ状プラズマ）
        if (flare) {
            float pSize = 3.2f + (float) Math.sin(time * 0.2) * 0.9f;
            quadOffset(buf, mat, -95f, 8.2f, 1.5f, pSize, 1.0f, 0.35f, 0.1f, 0.8f);
            quadOffset(buf, mat, -95f, -7.8f, -2.0f, pSize * 0.8f, 1.0f, 0.45f, 0.15f, 0.75f);
        }

        BufferUploader.drawWithShader(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void quad(BufferBuilder buf, org.joml.Matrix4f mat, float z, float r, float zOff, float cr, float cg, float cb, float ca) {
        quadOffset(buf, mat, z + zOff, 0, 0, r, cr, cg, cb, ca);
    }

    private static void quadOffset(BufferBuilder buf, org.joml.Matrix4f mat, float z, float ox, float oy, float r, float cr, float cg, float cb, float ca) {
        buf.vertex(mat, ox - r, oy - r, z).color(cr, cg, cb, ca).endVertex();
        buf.vertex(mat, ox + r, oy - r, z).color(cr, cg, cb, ca).endVertex();
        buf.vertex(mat, ox + r, oy + r, z).color(cr, cg, cb, ca).endVertex();
        buf.vertex(mat, ox - r, oy + r, z).color(cr, cg, cb, ca).endVertex();
    }
}
