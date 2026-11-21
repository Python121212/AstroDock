package io.github.python121212.astrodock.ship;

import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** ボタンIDのみを受信し、対象と確認済みの選択はサーバーのメニューに保持する。 */
public final class CoreMenu extends AbstractContainerMenu {
    public final BlockPos core;private final Level level;private final ContainerData info=new SimpleContainerData(9);
    private ShellSurvey preview;private long nextScan;
    public CoreMenu(int id,Inventory inventory,BlockPos core){super(ShipParts.CORE_MENU.get(),id);this.core=core;level=inventory.player.level();addDataSlots(info);}
    public boolean cabin(){return level.dimension()==CabinRooms.LEVEL;}
    public int value(int i){return info.get(i);}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return p.level()==level && p.isAlive() && !p.isSpectator() && p.getAbilities().mayBuild && !p.isPassenger()
            && p.distanceToSqr(core.getX()+.5,core.getY()+.5,core.getZ()+.5)<=64 && level.hasChunkAt(core)
            && level.getBlockState(core).is(ShipParts.CORE.get()) && level.mayInteract(p,core) && (cabin()||AstroDock.supported(level));}
    @Override public boolean clickMenuButton(Player who,int action){
        if(!(who instanceof ServerPlayer p)||!stillValid(p))return false;
        if(cabin()){
            if(action!=2 || level.getGameTime()<nextScan)return false;nextScan=level.getGameTime()+5;
            var room=CabinRooms.visitedCore(p,core);
            if(room==null){info.set(0,5);return false;}
            var dest=p.serverLevel().getServer().getLevel(room.dimension);
            if(dest==null || !(dest.getEntity(room.shipId) instanceof Ship ship)){info.set(0,6);return false;}
            if(!ship.isOwner(p)){info.set(0,5);return false;}
            info.set(0,ship.refit(p,room));return true;
        }
        if(action==0){
            if(level.getGameTime()<nextScan)return false;nextScan=level.getGameTime()+5;preview=ShellSurvey.scan(p.serverLevel(),core);
            if(preview==null){info.set(0,2);return true;}
            info.set(0,1);info.set(1,preview.blocks().size());info.set(2,preview.interior());
            var a=preview.lo().subtract(core);var b=preview.hi().subtract(core);
            info.set(3,a.getX());info.set(4,a.getY());info.set(5,a.getZ());info.set(6,b.getX());info.set(7,b.getY());info.set(8,b.getZ());return true;
        }
        if(action!=1 || preview==null)return false;
        var prior=preview;preview=null;
        if(!prior.equals(ShellSurvey.scan(p.serverLevel(),core))){info.set(0,3);return true;}
        if(ShipWrench.assemble(p.serverLevel(),p,prior.selection(core)))p.closeContainer();else info.set(0,4);return true;
    }
}
