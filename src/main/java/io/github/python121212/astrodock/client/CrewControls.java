package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.python121212.astrodock.ship.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT)
public final class CrewControls {
    private static final KeyMapping ENGINE=key("engine",GLFW.GLFW_KEY_G), SEAT=key("seat",GLFW.GLFW_KEY_V), CABIN=key("cabin",GLFW.GLFW_KEY_H);
    private static boolean usedPanel;
    private static KeyMapping key(String name,int code) {
        return new KeyMapping("key.astrodock."+name,KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,code,"key.categories.astrodock");
    }
    @Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){ e.register(ENGINE);e.register(SEAT);e.register(CABIN); }
    }
    @SubscribeEvent public static void input(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        Ship ship=mc.screen==null && mc.player!=null && mc.player.getVehicle() instanceof Ship s?s:null;
        if(!mc.options.keyUse.isDown() || ship==null) usedPanel=false;
        while(CABIN.consumeClick()) if(mc.screen==null && mc.player!=null && (ship!=null || mc.player.level().dimension()==CabinRooms.LEVEL)) ShipCommand.send(ship,ShipCommand.Action.CABIN);
        while(ENGINE.consumeClick()) if(ship!=null && ship.isOwner(mc.player)) ShipCommand.send(ship,ShipCommand.Action.ENGINE);
        while(SEAT.consumeClick()) if(ship!=null && ship.isOwner(mc.player)) ShipCommand.send(ship,ShipCommand.Action.SEAT);
    }
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered e) {
        var p=Minecraft.getInstance().player;
        if(!e.isUseItem() || p==null || !(p.getVehicle() instanceof Ship ship) || !ship.walking(p)) return;
        // 船体はワールドのブロックではない。艦内の右クリックを船外のブロックに通さない。
        e.setCanceled(true); e.setSwingHand(false);
        if(e.getHand()==InteractionHand.MAIN_HAND && !usedPanel && ship.hasPanelInSight(p)) {
            usedPanel=true;ShipCommand.send(ship,ShipCommand.Action.USE_PANEL);
        }
    }
}
