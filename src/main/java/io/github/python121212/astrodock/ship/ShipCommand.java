package io.github.python121212.astrodock.ship;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** クライアントから座標や推力は受け取らない。操作の可否と照準はサーバーで決める。 */
public record ShipCommand(int shipId, Action action) {
    public enum Action { ENGINE, SEAT, USE_PANEL }
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("astrodock","crew"),()->"1","1"::equals,"1"::equals);
    public static void register() {
        CHANNEL.messageBuilder(ShipCommand.class,0,NetworkDirection.PLAY_TO_SERVER)
                .encoder((m,b)->{b.writeVarInt(m.shipId);b.writeEnum(m.action);})
                .decoder(b->new ShipCommand(b.readVarInt(),b.readEnum(Action.class)))
                .consumerMainThread((m,ctx)->{
                    var p=ctx.get().getSender();
                    if(p!=null && p.getVehicle() instanceof Ship ship && ship.getId()==m.shipId) ship.command(p,m.action);
                }).add();
    }
    public static void send(Ship ship,Action action) {CHANNEL.sendToServer(new ShipCommand(ship.getId(),action));}
}
