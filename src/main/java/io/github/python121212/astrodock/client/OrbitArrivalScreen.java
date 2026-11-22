package io.github.python121212.astrodock.client;

import io.github.python121212.astrodock.AstroDock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT)
public final class OrbitArrivalScreen {
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ProgressScreen || event.getNewScreen() instanceof ReceivingLevelScreen)) return;
        var mc=Minecraft.getInstance();
        if (event.getCurrentScreen()!=null || mc.player==null || !mc.player.isAlive() || mc.getConnection()==null) return;
        // handleRespawn 内では接続先の level が先に更新され、プレイヤーはまだ出発元。
        // この短い区間だけ画面を省き、初回ログイン・死亡・他ディメンションの画面には触れない。
        var next=mc.getConnection().getLevel();
        var from=mc.player.level().dimension();
        if(next!=null && next.dimension()==AstroDock.ORBIT && (from==Level.OVERWORLD || from==AstroDock.MARS)) event.setCanceled(true);
    }
}
