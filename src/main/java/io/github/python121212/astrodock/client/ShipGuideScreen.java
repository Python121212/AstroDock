package io.github.python121212.astrodock.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
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
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        var step = entry.steps().get(stepIndex);
        g.drawCenteredString(font, entry.title(), width / 2, 14, 0x7FE7FF);
        g.drawCenteredString(font, step.title(), width / 2, 28, 0xFFFFFF);
        super.render(g, mx, my, partialTick);
    }
}
