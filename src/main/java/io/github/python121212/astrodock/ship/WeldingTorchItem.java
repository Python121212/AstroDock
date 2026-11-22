package io.github.python121212.astrodock.ship;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class WeldingTorchItem extends Item {
    public WeldingTorchItem(Properties properties) {
        super(properties.durability(256));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var ships = level.getEntitiesOfClass(Ship.class, new AABB(player.blockPosition()).inflate(8.0));
            if (!ships.isEmpty()) {
                Ship ship = ships.get(0);
                ship.extinguish();
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
