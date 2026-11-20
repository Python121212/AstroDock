package com.astrodock.astrodock;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AstroDockMod.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AstroDockMod {

    public static final String MODID = "astrodock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AstroDockMod() {
        LOGGER.info("AstroDock ロード中...");
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // クライアント・サーバー共通のセットアップ処理をここに書きます。
        LOGGER.info("AstroDock セットアップ完了！");
    }
}
