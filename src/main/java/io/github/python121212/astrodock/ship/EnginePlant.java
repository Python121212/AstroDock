package io.github.python121212.astrodock.ship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** 船体構築時だけ走査。形ではなく範囲内の6面接続で機関を決める。 */
record EnginePlant(boolean custom,int output,int cooling,String error) {
    record Range(BlockPos a,BlockPos b) {
        boolean contains(BlockPos p){return ShipWrench.validRange(a,b,p);}
        Range rotate(Rotation r){return new Range(a.rotate(r),b.rotate(r));}
    }
    static EnginePlant build(List<Hull.Part> parts) {
        var modules=new HashMap<BlockPos,BlockState>();var cores=new ArrayList<Hull.Part>();
        for(var part:parts) {
            var s=part.state();if(s.is(ShipParts.PLANT.get())) cores.add(part);
            else if(!s.is(ShipParts.DRIVE.get()) && !s.is(ShipParts.GENERATOR.get()) && !s.is(ShipParts.COOLER.get()))continue;
            modules.put(part.pos(),s);
        }
        var claimed=new HashSet<BlockPos>();int output=0,cooling=0;
        for(var core:cores) {
            int motors=0,gens=0,rads=0;var seen=new HashSet<BlockPos>();var q=new ArrayDeque<BlockPos>();q.add(core.pos());seen.add(core.pos());
            while(!q.isEmpty()) {
                var p=q.remove();for(var d:Direction.values()) {
                    var n=p.relative(d);var state=modules.get(n);
                    if(state==null || !seen.add(n) || core.range()!=null && !core.range().contains(n.subtract(core.pos())))continue;
                    if(state.is(ShipParts.PLANT.get()))continue;
                    if(!claimed.add(n))return new EnginePlant(true,0,0,"2個のエンジンコアが同じ部品を使用しています。機関部の範囲を分けてください");
                    if(state.is(ShipParts.DRIVE.get()))motors++;else if(state.is(ShipParts.GENERATOR.get()))gens++;else rads++;
                    q.add(n);
                }
            }
            if(motors==0 || gens==0)return new EnginePlant(true,0,0,"エンジンコアに動力部と発電部を面でつないでください");
            output+=Math.min(4*motors,3*gens);cooling+=1+2*rads;
        }
        return new EnginePlant(!cores.isEmpty(),output,cooling,null);
    }
    int generation(boolean hot){return hot?output/2:output;}
    int nextHeat(int heat,int used){return Math.max(0,Math.min(100,heat+used-cooling));}
}
