package io.github.python121212.astrodock.ship;

import java.util.*;
import org.joml.Matrix3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 船体は組み立て・ロード時のみ走査し、質量特性とエンジンの腕の長さを集約する。 */
public final class Hull {
    public record Part(BlockPos pos, BlockState state, double mass) {}
    public final List<Part> parts;
    public final double mass, cx, cy, cz, radiusX, radiusZ, pivotHeight;
    final float armX, armY, armZ;
    final Matrix3f inertiaTensor, inverseInertia;
    final CabinDeck deck;
    public final boolean hasHelm;
    public final int engines, floor, height;
    public Hull(List<Part> parts) {
        if (parts.isEmpty() || parts.size() > ShipWrench.MAX_PARTS) throw new IllegalArgumentException("船体サイズが不正です");
        this.parts = List.copyOf(parts);
        deck=new CabinDeck(this.parts);
        double m=0, x=0, y=0, z=0;
        int low=Integer.MAX_VALUE, high=Integer.MIN_VALUE;
        for (Part p : parts) {
            m+=p.mass; x+=p.mass*p.pos.getX(); y+=p.mass*p.pos.getY(); z+=p.mass*p.pos.getZ();
            low=Math.min(low,p.pos.getY()); high=Math.max(high,p.pos.getY());
        }
        mass=m; cx=x/m; cy=y/m; cz=z/m; pivotHeight=cy-low+0.5; floor=low; height=high-low+1;
        double xx=0, yy=0, zz=0, xy=0, xz=0, yz=0, ax=0, ay=0, az=0, rx=0, rz=0; int engines=0; boolean helm=false;
        for (Part p : parts) {
            double dx=p.pos.getX()-cx, dy=p.pos.getY()-cy, dz=p.pos.getZ()-cz;
            // 単位立方体自身の慣性 + 平行軸の定理（非対称船体の積慣性も保持）。
            xx+=p.mass*(dy*dy+dz*dz+1.0/6); yy+=p.mass*(dx*dx+dz*dz+1.0/6); zz+=p.mass*(dx*dx+dy*dy+1.0/6);
            xy-=p.mass*dx*dy; xz-=p.mass*dx*dz; yz-=p.mass*dy*dz;
            rx=Math.max(rx,Math.abs(dx)+0.5); rz=Math.max(rz,Math.abs(dz)+0.5);
            if (p.state.is(ShipParts.HELM.get())) helm=true;
            if (p.state.is(ShipParts.ENGINE.get())) { engines++; ax+=dx; ay+=dy; az+=dz; }
        }
        hasHelm=helm; this.engines=engines; armX=(float)ax; armY=(float)ay; armZ=(float)az;
        inertiaTensor=new Matrix3f((float)xx,(float)xy,(float)xz,(float)xy,(float)yy,(float)yz,(float)xz,(float)yz,(float)zz);
        inverseInertia=new Matrix3f(inertiaTensor).invert(); radiusX=rx; radiusZ=rz;
    }
    ListTag save() {
        ListTag list=new ListTag();
        for (Part p:parts) {
            CompoundTag t=new CompoundTag(); t.putLong("pos",p.pos.asLong()); t.putDouble("mass",p.mass);
            t.put("state",NbtUtils.writeBlockState(p.state)); list.add(t);
        }
        return list;
    }
    static Hull load(ListTag list, Level level) {
        List<Part> parts=new ArrayList<>();
        for (int i=0;i<list.size();i++) {
            CompoundTag t=list.getCompound(i);
            parts.add(new Part(BlockPos.of(t.getLong("pos")),NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), t.getCompound("state")),Math.max(0.25,t.getDouble("mass"))));
        }
        return new Hull(parts);
    }
}
