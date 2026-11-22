package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** アイテムのShift長押しから開く、一新されたCreate風の3D組み方ガイド（Ponder）画面。 */
@Mod.EventBusSubscriber(modid = "astrodock", value = Dist.CLIENT)
public final class ShipGuideScreen extends Screen {
    private static final int HOLD_TICKS = 16;
    private static Item hoveredItem;
    private static long lastHoverTick;
    private static int holdTicks;

    private final ShipGuide.Entry entry;
    private final Screen parent;
    private int stepIndex;
    private float yaw = -35f;
    private float pitch = 25f;
    private float zoom = 28f;
    private boolean dragging;

    public ShipGuideScreen(ShipGuide.Entry entry, Screen parent) {
        super(Component.literal(entry.title()));
        this.entry = entry;
        this.parent = parent;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var item = event.getItemStack().getItem();
        if (ShipGuide.get(item) == null) return;
        var mc = Minecraft.getInstance();
        long tick = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50;
        if (hoveredItem != item) {
            hoveredItem = item;
            holdTicks = 0;
        }
        lastHoverTick = tick;
        int filled = Math.min(10, holdTicks * 10 / HOLD_TICKS);
        String bar = "█".repeat(filled) + "░".repeat(10 - filled);
        event.getToolTip().add(Component.literal("[Shift] 長押しで3D組み方ガイド " + bar)
                .withStyle(Screen.hasShiftDown() ? ChatFormatting.YELLOW : ChatFormatting.AQUA));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.screen == null || mc.screen instanceof ShipGuideScreen) {
            holdTicks = 0;
            return;
        }
        long tick = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50;
        if (hoveredItem != null && tick - lastHoverTick <= 2 && Screen.hasShiftDown()) {
            if (++holdTicks >= HOLD_TICKS) {
                var entry = ShipGuide.get(hoveredItem);
                holdTicks = 0;
                hoveredItem = null;
                if (entry != null) mc.setScreen(new ShipGuideScreen(entry, mc.screen));
            }
        } else {
            holdTicks = 0;
        }
    }

    @Override
    protected void init() {
        int cy = height - 30;
        addRenderableWidget(Button.builder(Component.literal("◀ 前へ"), b -> {
            if (stepIndex > 0) stepIndex--;
        }).bounds(width / 2 - 140, cy, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("次へ ▶"), b -> {
            if (stepIndex < entry.steps().size() - 1) stepIndex++;
        }).bounds(width / 2 - 56, cy, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                .bounds(width / 2 + 44, cy, 88, 20).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && stepIndex > 0) { stepIndex--; return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && stepIndex < entry.steps().size() - 1) { stepIndex++; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        zoom = Math.max(14f, Math.min(48f, zoom + (float) delta * 2.5f));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && my < height - 44) dragging = true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 0.8f;
            pitch = Math.max(-80f, Math.min(80f, pitch + (float) dy * 0.8f));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xE80A111C, 0xF0121E2E);

        var step = entry.steps().get(stepIndex);
        g.fill(0, 0, width, 42, 0xCC0D1624);
        g.fill(0, 42, width, 43, 0xFF4CC9F0);
        g.drawCenteredString(font, entry.title(), width / 2, 8, 0x4CC9F0);
        g.drawCenteredString(font, step.title(), width / 2, 23, 0xFFFFFF);

        // ステップ進捗ドット
        int total = entry.steps().size();
        int dotsW = total * 12;
        int dotX = (width - dotsW) / 2;
        for (int i = 0; i < total; i++) {
            g.fill(dotX + i * 12, 49, dotX + i * 12 + 8, 53, i == stepIndex ? 0xFF4CC9F0 : 0xFF32455A);
        }

        if (!dragging) yaw += partialTick * 0.45f;
        renderStructure(g, step);

        g.drawString(font, "ドラッグ: 回転  ホイール: 拡大縮小  ←/→: ステップ", 12, height - 16, 0x7A8FA6);

        int boxW = Math.min(440, width - 32);
        int boxX = (width - boxW) / 2;
        int boxY = height - 96;
        g.fill(boxX, boxY, boxX + boxW, boxY + 56, 0xCC0D1624);
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF4CC9F0);
        g.drawWordWrap(font, Component.literal(step.detail()), boxX + 10, boxY + 7, boxW - 20, 0xE6EDF3);
        g.drawWordWrap(font, Component.literal(step.tip()), boxX + 10, boxY + 34, boxW - 20, 0xFFD166);

        super.render(g, mx, my, partialTick);
    }

    private void renderStructure(GuiGraphics g, ShipGuide.Step step) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(width / 2f, height * 0.43f, 200f);
        pose.scale(zoom, -zoom, zoom);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5f, -0.5f, -0.5f);

        RenderSystem.enableDepthTest();
        var mc = Minecraft.getInstance();
        var dispatcher = mc.getBlockRenderer();
        var buffer = mc.renderBuffers().bufferSource();
        for (var part : step.parts()) {
            pose.pushPose();
            pose.translate(part.pos().getX(), part.pos().getY(), part.pos().getZ());
            dispatcher.renderSingleBlock(part.state(), pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffer.endBatch();
        RenderSystem.disableDepthTest();
        pose.popPose();
    }
}
