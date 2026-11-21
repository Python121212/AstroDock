package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 船体は組み立て・ロード時のみ走査し、質量特性とエンジンの腕の長さを集約する。 */
public final class Hull {
    public record Part(BlockPos pos, BlockState state, double mass) {}
    public final List<Part> parts;
    public final double mass, cx, cz, inertia, engineArm, radiusX, radiusZ;
    public final int engines, floor, height;
    public Hull(List<Part> parts) {
        if (parts.isEmpty() || parts.size() > ShipWrench.MAX_PARTS) throw new IllegalArgumentException("船体サイズが不正です");
        this.parts = List.copyOf(parts);
        double m=0, x=0, z=0;
        int low=Integer.MAX_VALUE, high=Integer.MIN_VALUE;
        for (Part p : parts) {
            m+=p.mass; x+=p.mass*p.pos.getX(); z+=p.mass*p.pos.getZ();
            low=Math.min(low,p.pos.getY()); high=Math.max(high,p.pos.getY());
        }
        mass=m; cx=x/m; cz=z/m; floor=low; height=high-low+1;
        double i=0, arm=0, rx=0, rz=0; int engines=0;
        for (Part p : parts) {
            double dx=p.pos.getX()-cx, dz=p.pos.getZ()-cz;
            i+=p.mass*(dx*dx+dz*dz+1.0/6);
            rx=Math.max(rx,Math.abs(dx)+0.5); rz=Math.max(rz,Math.abs(dz)+0.5);
            if (p.state.is(ShipParts.ENGINE.get())) { engines++; arm+=dx; }
        }
        this.engines=engines; engineArm=arm; inertia=i; radiusX=rx; radiusZ=rz;
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
