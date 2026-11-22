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
    public record Part(BlockPos pos, BlockState state, double mass,EnginePlant.Range range) {
        public Part(BlockPos pos,BlockState state,double mass){this(pos,state,mass,null);}
    }
    final EnginePlant plant;
    public final List<Part> parts;
    public final double mass, cx, cy, cz, radiusX, radiusZ, pivotHeight;
    final double verticalDrag;
    final float armX, armY, armZ;
    final Matrix3f inertiaTensor, inverseInertia;
    final CabinDeck deck;
    final List<Walkway> walkways;
    final List<Part> doors;
    public final boolean hasHelm, hasGravityController, watertight;
    public final int engines, floor, height, pointDefenseLasers, salvageBeams, miningLasers, lifeSupports, warpDrives, shieldGenerators, cannonBarrels, cannonTurrets, cannonBreeches;
    public final double effectiveEngines, streamline, ricochetChance, displacementCapacity;
    public Hull(List<Part> parts) {
        if (parts.isEmpty() || parts.size() > ShipWrench.MAX_PARTS) throw new IllegalArgumentException("船体サイズが不正です");
        this.parts = List.copyOf(parts);
        doors=this.parts.stream().filter(part->HullDoors.airlock(part.state())).toList();
        walkways=doors.isEmpty()?List.of():Walkway.find(this.parts);
        plant=EnginePlant.build(this.parts);
        deck=new CabinDeck(this.parts);
        double m=0, x=0, y=0, z=0;
        int low=Integer.MAX_VALUE, high=Integer.MIN_VALUE;
        for (Part p : parts) {
            m+=p.mass; x+=p.mass*p.pos.getX(); y+=p.mass*p.pos.getY(); z+=p.mass*p.pos.getZ();
            low=Math.min(low,p.pos.getY()); high=Math.max(high,p.pos.getY());
        }
        // 同じ形なら密度が高いほど鉛直速度に対する空気抵抗の効きが小さい。
        verticalDrag=.055*Math.sqrt(4*parts.size()/m);
        mass=m; cx=x/m; cy=y/m; cz=z/m; pivotHeight=cy-low+0.5; floor=low; height=high-low+1;
        double xx=0, yy=0, zz=0, xy=0, xz=0, yz=0, ax=0, ay=0, az=0, rx=0, rz=0; int engines=0; boolean helm=false, stabilizer=false;
        for (Part p : parts) {
            double dx=p.pos.getX()-cx, dy=p.pos.getY()-cy, dz=p.pos.getZ()-cz;
            // 単位立方体自身の慣性 + 平行軸の定理（非対称船体の積慣性も保持）。
            xx+=p.mass*(dy*dy+dz*dz+1.0/6); yy+=p.mass*(dx*dx+dz*dz+1.0/6); zz+=p.mass*(dx*dx+dy*dy+1.0/6);
            xy-=p.mass*dx*dy; xz-=p.mass*dx*dz; yz-=p.mass*dy*dz;
            rx=Math.max(rx,Math.abs(dx)+0.5); rz=Math.max(rz,Math.abs(dz)+0.5);
            if (p.state.is(ShipParts.GRAVITY_CONTROLLER.get())) stabilizer=true;
            if (p.state.is(ShipParts.HELM.get())) helm=true;
            if (p.state.is(ShipParts.ENGINE.get())) { engines++; ax+=dx; ay+=dy; az+=dz; }
        }
        hasGravityController=stabilizer; hasHelm=helm; this.engines=engines; armX=(float)ax; armY=(float)ay; armZ=(float)az;
        double nozzleBonus = 0;
        if (engines > 0) {
            Set<BlockPos> thrusterSet = new HashSet<>();
            Set<BlockPos> casingSet = new HashSet<>();
            for (Part pt : parts) {
                if (pt.state.is(ShipParts.ENGINE.get())) thrusterSet.add(pt.pos);
                else if (pt.state.is(ShipParts.NOZZLE_CASING.get())) casingSet.add(pt.pos);
            }
            if (!casingSet.isEmpty()) {
                for (BlockPos tPos : thrusterSet) {
                    if (casingSet.contains(tPos.south())) nozzleBonus -= 0.2; // 真後ろを塞ぐと排気詰まりで遅くなる
                    Deque<BlockPos> q = new ArrayDeque<>();
                    Set<BlockPos> visited = new HashSet<>();
                    q.add(tPos);
                    visited.add(tPos);
                    int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
                    int local = 0;
                    while (!q.isEmpty() && local < 6) {
                        BlockPos cur = q.poll();
                        for (int[] dir : dirs) {
                            BlockPos nxt = cur.offset(dir[0], dir[1], dir[2]);
                            if (casingSet.contains(nxt) && visited.add(nxt)) {
                                local++;
                                int dz = nxt.getZ() - tPos.getZ();
                                int lat = Math.abs(nxt.getX() - tPos.getX()) + Math.abs(nxt.getY() - tPos.getY());
                                if (dz > 0 && lat > 0) nozzleBonus += 0.15; // 後方に広がるノズルは推力向上
                                else if (dz < 0) nozzleBonus -= 0.1;       // 前方に組むと逆効果
                                else if (!nxt.equals(tPos.south())) nozzleBonus += 0.08;
                                q.add(nxt);
                            }
                        }
                    }
                }
            }
        }
        this.effectiveEngines = engines == 0 ? 0 : Math.max(engines * 0.5, Math.min(engines * 1.5, engines + nozzleBonus));

        double lenZ = Math.max(1, 2 * rz);
        double cross = Math.max(1, 2 * rx * height);
        this.streamline = Math.max(0.55, Math.min(1.0, 1.0 - 0.15 * (lenZ / Math.sqrt(cross) - 1.0)));

        Set<BlockPos> solid = new HashSet<>();
        for (Part pt : parts) solid.add(pt.pos);
        int sloped = 0, pd = 0, sb = 0, ml = 0, ls = 0, wd = 0, sg = 0, cb = 0, ct = 0, cbr = 0;
        for (Part pt : parts) {
            if (pt.state.is(ShipParts.POINT_DEFENSE_LASER.get())) pd++;
            if (pt.state.is(ShipParts.SALVAGE_BEAM.get())) sb++;
            if (pt.state.is(ShipParts.MINING_LASER.get())) ml++;
            if (pt.state.is(ShipParts.LIFE_SUPPORT.get())) ls++;
            if (pt.state.is(ShipParts.WARP_DRIVE.get())) wd++;
            if (pt.state.is(ShipParts.SHIELD_GENERATOR.get())) sg++;
            if (pt.state.is(ShipParts.CANNON_BARREL.get())) cb++;
            if (pt.state.is(ShipParts.CANNON_TURRET.get())) ct++;
            if (pt.state.is(ShipParts.CANNON_BREECH.get())) cbr++;
            boolean hasFront = solid.contains(pt.pos.north());
            boolean hasBack = solid.contains(pt.pos.south());
            boolean hasLeft = solid.contains(pt.pos.west());
            boolean hasRight = solid.contains(pt.pos.east());
            var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(pt.state.getBlock());
            String path = key != null ? key.getPath() : "";
            boolean shaped = (key != null && "framedblocks".equals(key.getNamespace()))
                    || path.contains("slope") || path.contains("stairs") || path.contains("slab")
                    || path.contains("wedge") || path.contains("corner") || path.contains("prism");
            if (((hasFront ^ hasBack) && (hasLeft ^ hasRight)) || shaped) sloped++;
        }
        this.pointDefenseLasers = pd;
        this.salvageBeams = sb;
        this.miningLasers = ml;
        this.lifeSupports = ls;
        this.warpDrives = wd;
        this.shieldGenerators = sg;
        this.cannonBarrels = cb;
        this.cannonTurrets = ct;
        this.cannonBreeches = cbr;
        this.ricochetChance = parts.isEmpty() ? 0 : Math.min(0.45, (double) sloped / parts.size() * 0.9);

        int sealedCells = countSealedCells(parts, solid);
        this.watertight = sealedCells > 0;
        this.displacementCapacity = sealedCells * 7.0;
        inertiaTensor=new Matrix3f((float)xx,(float)xy,(float)xz,(float)xy,(float)yy,(float)yz,(float)xz,(float)yz,(float)zz);
        inverseInertia=new Matrix3f(inertiaTensor).invert(); radiusX=rx; radiusZ=rz;
    }
    private static int countSealedCells(List<Part> parts, Set<BlockPos> solid) {
        if (parts.isEmpty()) return 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Part pt : parts) {
            minX = Math.min(minX, pt.pos.getX()); maxX = Math.max(maxX, pt.pos.getX());
            minY = Math.min(minY, pt.pos.getY()); maxY = Math.max(maxY, pt.pos.getY());
            minZ = Math.min(minZ, pt.pos.getZ()); maxZ = Math.max(maxZ, pt.pos.getZ());
        }
        if (maxX - minX < 2 || maxY - minY < 2 || maxZ - minZ < 2) return parts.size();
        Set<BlockPos> outside = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        BlockPos start = new BlockPos(minX - 1, minY - 1, minZ - 1);
        outside.add(start);
        q.add(start);
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!q.isEmpty()) {
            BlockPos cur = q.poll();
            for (int[] dir : dirs) {
                BlockPos nxt = cur.offset(dir[0], dir[1], dir[2]);
                if (nxt.getX() < minX - 1 || nxt.getX() > maxX + 1
                        || nxt.getY() < minY - 1 || nxt.getY() > maxY + 1
                        || nxt.getZ() < minZ - 1 || nxt.getZ() > maxZ + 1) continue;
                if (!solid.contains(nxt) && outside.add(nxt)) q.add(nxt);
            }
        }
        int interior = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    BlockPos inner = new BlockPos(x, y, z);
                    if (!solid.contains(inner)) {
                        if (outside.contains(inner)) return 0;
                        interior++;
                    }
                }
            }
        }
        return parts.size() + interior;
    }
    static double partMass(BlockState state,float hardness) {
        if(state.is(ShipParts.LAMINATED_ARMOR.get())) return 3;
        if(state.is(ShipParts.BLACKSHELL_ARMOR.get())) return 12;
        return 1+Math.min(8,hardness);
    }
    ListTag save() {
        ListTag list=new ListTag();
        for (Part p:parts) {
            CompoundTag t=new CompoundTag(); t.putLong("pos",p.pos.asLong()); t.putDouble("mass",p.mass);
            if(p.range!=null){t.putLong("plantA",p.range.a().asLong());t.putLong("plantB",p.range.b().asLong());}
            t.put("state",NbtUtils.writeBlockState(p.state)); list.add(t);
        }
        return list;
    }
    static Hull load(ListTag list, Level level) {
        List<Part> parts=new ArrayList<>();
        for (int i=0;i<list.size();i++) {
            CompoundTag t=list.getCompound(i);
            parts.add(new Part(BlockPos.of(t.getLong("pos")),NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), t.getCompound("state")),Math.max(0.25,t.getDouble("mass")),
                    t.contains("plantA")?new EnginePlant.Range(BlockPos.of(t.getLong("plantA")),BlockPos.of(t.getLong("plantB"))):null));
        }
        return new Hull(parts);
    }
}
