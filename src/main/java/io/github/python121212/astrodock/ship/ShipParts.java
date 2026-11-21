package io.github.python121212.astrodock.ship;

import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ShipParts {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "astrodock");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "astrodock");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "astrodock");
    public static final RegistryObject<Block> CORE = part("ship_core"), ENGINE = part("thruster");
    public static final RegistryObject<Item> WRENCH = ITEMS.register("ship_wrench", ShipWrench::new);
    public static final RegistryObject<EntityType<Ship>> SHIP = ENTITIES.register("ship", () -> EntityType.Builder.<Ship>of(Ship::new, MobCategory.MISC)
            .sized(1, 1).clientTrackingRange(12).updateInterval(1).fireImmune().build("astrodock:ship"));

    private static RegistryObject<Block> part(String id) {
        var block = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops()));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); }
}
