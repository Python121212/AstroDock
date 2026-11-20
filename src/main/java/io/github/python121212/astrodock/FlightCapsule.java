package io.github.python121212.astrodock;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** 手持ちの一人用飛行装置。別エンティティを作らず境界を越えられる。 */
public final class FlightCapsule extends Item {
    public FlightCapsule() { super(new Item.Properties().stacksTo(1)); }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!AstroDock.supported(level) || player.isPassenger()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
