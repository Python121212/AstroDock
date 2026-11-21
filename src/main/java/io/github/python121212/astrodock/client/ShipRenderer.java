package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.python121212.astrodock.ship.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ShipRenderer extends EntityRenderer<Ship> {
    ShipRenderer(EntityRendererProvider.Context ctx) { super(ctx); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) {e.registerEntityRenderer(ShipParts.SHIP.get(),ShipRenderer::new);
        for(var shot:java.util.List.of(ShipParts.SHELL,ShipParts.MISSILE,ShipParts.TORPEDO))
            e.registerEntityRenderer(shot.get(),ctx->new ThrownItemRenderer<>(ctx,1.3f,true));}
    @Override public ResourceLocation getTextureLocation(Ship ship) { return TextureAtlas.LOCATION_BLOCKS; }
    @Override public void render(Ship ship,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light) {
        Hull hull=ship.hull(); if(hull==null)return;
        poses.pushPose();poses.translate(0,hull.pivotHeight,0);poses.mulPose(ship.renderRotation(partial));
        poses.translate(-hull.cx-0.5,-hull.cy-0.5,-hull.cz-0.5);
        var renderer=Minecraft.getInstance().getBlockRenderer();
        for(Hull.Part part:hull.parts){poses.pushPose();poses.translate(part.pos().getX(),part.pos().getY(),part.pos().getZ());
            renderer.renderSingleBlock(part.state(),poses,buffers,light,OverlayTexture.NO_OVERLAY);poses.popPose();}
        poses.popPose();
    }
}
