package io.github.python121212.astrodock.client;

import io.github.python121212.astrodock.ship.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CoreScreen extends AbstractContainerScreen<CoreMenu> {
    private Button confirm;
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(ShipParts.CORE_MENU.get(),CoreScreen::new));}
    CoreScreen(CoreMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=224;imageHeight=146;}
    private void action(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private Button button(String name,int x,int width,int id){return addRenderableWidget(Button.builder(Component.translatable("core.astrodock."+name),b->action(id)).bounds(leftPos+x,topPos+116,width,20).build());}
    @Override protected void init(){super.init();if(menu.cabin())button("apply",10,204,2);else {button("scan",10,116,0);confirm=button("confirm",131,83,1);confirm.active=false;}}
    @Override protected void containerTick(){super.containerTick();if(confirm!=null)confirm.active=menu.value(0)==1;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,0xff3a7ca5);
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff0f1722);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+26,0xff1b2c40);
        g.fill(leftPos+2,topPos+26,leftPos+imageWidth-2,topPos+27,0xff4cc9f0);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,10,10,0xe9f4ff);int s=menu.value(0);
        if(s==1){g.drawString(font,Component.translatable("core.astrodock.count",menu.value(1),menu.value(2)),10,39,0x9df3c5);
            var lo=menu.core.offset(menu.value(3),menu.value(4),menu.value(5));var hi=menu.core.offset(menu.value(6),menu.value(7),menu.value(8));
            g.drawString(font,lo.toShortString(),10,57,0xffffff);g.drawString(font,"→ "+hi.toShortString(),10,73,0xffffff);
        }else g.drawWordWrap(font,Component.translatable("core.astrodock."+(s==0?(menu.cabin()?"work":"start"):"status"+s)),10,39,204,0xe1eef5);
    }
    @Override public void render(GuiGraphics g,int x,int y,float f){renderBackground(g);super.render(g,x,y,f);renderTooltip(g,x,y);}
}
