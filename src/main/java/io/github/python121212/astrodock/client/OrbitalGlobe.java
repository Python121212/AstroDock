package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.python121212.astrodock.PlanetTravel;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** 宇宙側にはエンティティも球殻ブロックも置かない。距離で精度を選ぶ静的メッシュ。 */
enum OrbitalGlobe {
    EARTH, RUBRA;
    private static final ResourceLocation EARTH_MAP=new ResourceLocation("astrodock","textures/environment/earth.png");
    private VertexBuffer near, far;

    void draw(RenderLevelStageEvent event) {
        Vec3 camera=event.getCamera().getPosition();
        // 帰還境界は平面なので、地球は宇宙のどの X/Z からも天底に見える。
        double physicalRadius=this==EARTH?1800:PlanetTravel.RADIUS;
        Vec3 delta=this==EARTH?new Vec3(0,-physicalRadius-180-Math.max(0,camera.y),0):PlanetTravel.CENTER.subtract(camera);
        double distance=delta.length(), angularRadius=physicalRadius/Math.max(1,distance);
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
        float radius=(float)(physicalRadius*scale);pose.scale(radius,radius,radius);
        RenderSystem.enableCull();
        if(this==EARTH) {
            pose.mulPose(Axis.XP.rotationDegrees(-55));pose.mulPose(Axis.YP.rotationDegrees(40));
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0,EARTH_MAP);
        } else RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            mesh.bind();mesh.drawWithShader(pose.last().pose(),event.getProjectionMatrix(),RenderSystem.getShader());
        } finally { VertexBuffer.unbind();pose.popPose(); }
    }
    private VertexBuffer create(int sides) {
        var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,this==EARTH?DefaultVertexFormat.POSITION_TEX_COLOR:DefaultVertexFormat.POSITION_COLOR);
        for (int j=0;j<sides/2;j++) for (int i=0;i<sides;i++) {
            vertex(b,i,j,sides);vertex(b,i+1,j,sides);vertex(b,i+1,j+1,sides);vertex(b,i,j+1,sides);
        }
        var mesh=new VertexBuffer(VertexBuffer.Usage.STATIC);mesh.bind();mesh.upload(b.end());VertexBuffer.unbind();return mesh;
    }
    private void vertex(BufferBuilder b,int i,int j,int sides) {
        double lon=i*2*Math.PI/sides, lat=j*2*Math.PI/sides-Math.PI/2;
        double ring=Math.cos(lat), x=ring*Math.sin(lon), y=Math.sin(lat), z=ring*Math.cos(lon);
        if(this==EARTH) {
            int lit=(int)(255*(0.20+0.8*Math.max(0,-0.65*x+0.55*y+0.52*z)));
            b.vertex(x,y,z).uv((float)i/sides,1f-2f*j/sides).color(lit,lit,lit,255).endVertex();return;
        }
        double sun=0.21+0.79*Math.max(0,-0.66*x+0.54*y-0.52*z);
        double bands=0.5+0.5*Math.sin(15*y+2*Math.sin(4*x+2*z));
        b.vertex(x,y,z).color((int)((180+59*bands)*sun),(int)((69+82*bands)*sun),(int)((45+48*bands)*sun),255).endVertex();
    }
    void release() {
        if(near!=null){near.close();near=null;} if(far!=null){far.close();far=null;}
    }
}
