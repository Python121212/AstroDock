package io.github.python121212.astrodock.client;

import io.github.python121212.astrodock.ship.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="astrodock",value=Dist.CLIENT)
public final class SelectionPreview {
    @SubscribeEvent public static void draw(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
        var stack=mc.player.getMainHandItem();if(!stack.is(ShipParts.WRENCH.get()))return;
        var t=stack.getTagElement(ShipWrench.EDIT);if(t==null || !t.contains("a") || !mc.level.dimension().location().toString().equals(t.getString("dim")))return;
        BlockPos a=BlockPos.of(t.getLong("a")), b=t.contains("b")?BlockPos.of(t.getLong("b")):a;
        if(!ShipWrench.validRange(a,b,a) || !mc.player.blockPosition().closerThan(a,48))return;
        var poses=e.getPoseStack(); Vec3 camera=e.getCamera().getPosition();poses.pushPose();poses.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        var box=new AABB(a).minmax(new AABB(b));LevelRenderer.renderLineBox(poses,lines,box.inflate(0.012),1,0.78f,0.15f,1);
        if(t.contains("b"))for(BlockPos p:BlockPos.betweenClosed(a,b)) {
            if(mc.level.isEmptyBlock(p))continue;
            boolean on=ShipWrench.selected(t,p);
            LevelRenderer.renderLineBox(poses,lines,on?new AABB(p).inflate(0.004):new AABB(p).deflate(0.17),on?0.1f:0.58f,on?0.9f:0.58f,on?1:0.58f,1);
        }
        buffers.endBatch(RenderType.lines());poses.popPose();
    }
}
