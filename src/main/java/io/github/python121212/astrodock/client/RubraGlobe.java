package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.python121212.astrodock.PlanetTravel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** 宇宙側にはエンティティも球殻ブロックも置かない。距離で精度を選ぶ静的メッシュ。 */
final class RubraGlobe {
    private static VertexBuffer near, far;

    static void draw(RenderLevelStageEvent event) {
        Vec3 delta=PlanetTravel.CENTER.subtract(event.getCamera().getPosition());
        double distance=delta.length(), angularRadius=PlanetTravel.RADIUS/Math.max(1,distance);
        if (angularRadius<0.0005 || distance<1) return;
        boolean close=angularRadius>0.12;
        VertexBuffer mesh=close?near:far;
        if (mesh==null) {
            mesh=create(close?32:12);
            if(close) near=mesh; else far=mesh;
        }
        // 方向・視直径は実座標に一致させ、投影だけを空のクリップ範囲に収める。
        double scale=8/distance;
        var pose=event.getPoseStack();pose.pushPose();
        pose.translate(delta.x*scale,delta.y*scale,delta.z*scale);
        float radius=(float)(PlanetTravel.RADIUS*scale);pose.scale(radius,radius,radius);
        RenderSystem.enableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            mesh.bind();mesh.drawWithShader(pose.last().pose(),event.getProjectionMatrix(),RenderSystem.getShader());
        } finally { VertexBuffer.unbind();pose.popPose(); }
    }
    private static VertexBuffer create(int sides) {
        var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for (int j=0;j<sides/2;j++) for (int i=0;i<sides;i++) {
            vertex(b,i,j,sides);vertex(b,i+1,j,sides);vertex(b,i+1,j+1,sides);vertex(b,i,j+1,sides);
        }
        var mesh=new VertexBuffer(VertexBuffer.Usage.STATIC);mesh.bind();mesh.upload(b.end());VertexBuffer.unbind();return mesh;
    }
    private static void vertex(BufferBuilder b,int i,int j,int sides) {
        double lon=i*2*Math.PI/sides, lat=j*2*Math.PI/sides-Math.PI/2;
        double ring=Math.cos(lat), x=ring*Math.sin(lon), y=Math.sin(lat), z=ring*Math.cos(lon);
        double sun=0.21+0.79*Math.max(0,-0.66*x+0.54*y-0.52*z);
        double bands=0.5+0.5*Math.sin(15*y+2*Math.sin(4*x+2*z));
        b.vertex(x,y,z).color((int)((180+59*bands)*sun),(int)((69+82*bands)*sun),(int)((45+48*bands)*sun),255).endVertex();
    }
    static void release() {
        if(near!=null){near.close();near=null;} if(far!=null){far.close();far=null;}
    }
    private RubraGlobe() {}
}
