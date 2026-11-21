package io.github.python121212.astrodock.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="astrodock")
public final class StarterEquipment {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p)grantOnce(p);
    }
    static void grantOnce(ServerPlayer p){
        var root=p.getPersistentData();CompoundTag flags=root.getCompound(Player.PERSISTED_NBT_TAG);
        if(flags.getBoolean("AstroDockStarterCannon"))return;
        var cannon=new ItemStack(ShipParts.CANNON.get());if(!p.getInventory().add(cannon))p.drop(cannon,false);
        flags.putBoolean("AstroDockStarterCannon",true);root.put(Player.PERSISTED_NBT_TAG,flags);
    }
}
