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
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** アイテムのShift長押しから開く、Create風の3D組み方ガイド画面。 */
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
        event.getToolTip().add(Component.literal("[Shift] 長押しで3D組み方ガイド ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(bar).withStyle(Screen.hasShiftDown() ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY)));
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
        int cy = height - 34;
        addRenderableWidget(Button.builder(Component.literal("< 前へ"), b -> {
            if (stepIndex > 0) stepIndex--;
        }).bounds(width / 2 - 130, cy, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("次へ >"), b -> {
            if (stepIndex < entry.steps().size() - 1) stepIndex++;
        }).bounds(width / 2 - 50, cy, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                .bounds(width / 2 + 40, cy, 80, 20).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
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
        renderBackground(g);
        var step = entry.steps().get(stepIndex);

        g.drawCenteredString(font, entry.title() + " (" + (stepIndex + 1) + "/" + entry.steps().size() + ")", width / 2, 14, 0x7FE7FF);
        g.drawCenteredString(font, step.title(), width / 2, 28, 0xFFFFFF);

        // 3Dブロック構造の描画（ドラッグで回転、未操作時はゆっくり自動回転）
        if (!dragging) yaw += partialTick * 0.45f;
        renderStructure(g, step);

        // 解説とヒントパネル
        int boxW = Math.min(420, width - 32);
        int boxX = (width - boxW) / 2;
        int boxY = height - 96;
        g.fill(boxX, boxY, boxX + boxW, boxY + 54, 0xAA101824);
        g.renderOutline(boxX, boxY, boxW, 54, 0xFF3A7CA5);
        g.drawWordWrap(font, Component.literal(step.detail()), boxX + 8, boxY + 6, boxW - 16, 0xE6EDF3);
        g.drawWordWrap(font, Component.literal(step.tip()).withStyle(ChatFormatting.GOLD), boxX + 8, boxY + 32, boxW - 16, 0xFFD166);

        super.render(g, mx, my, partialTick);
    }

    private void renderStructure(GuiGraphics g, ShipGuide.Step step) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(width / 2f, height * 0.44f, 200f);
        float scale = 28f;
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5f, -0.5f, -0.5f);

        RenderSystem.enableDepthTest();
        var dispatcher = minecraft.getBlockRenderer();
        var buffer = minecraft.renderBuffers().bufferSource();
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
