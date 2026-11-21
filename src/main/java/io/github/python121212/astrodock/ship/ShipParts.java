package io.github.python121212.astrodock.ship;

import java.util.function.Function;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ShipParts {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "astrodock");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "astrodock");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "astrodock");
    public static final RegistryObject<Block> CORE = part("ship_core", ShipCoreBlock::new), ENGINE = part("thruster", Block::new);
    public static final RegistryObject<Block> CONSOLE = part("engine_console", Block::new);
    public static final RegistryObject<Block> HELM = part("helm", HelmBlock::new);
    public static final RegistryObject<Block> AIRLOCK = part("airlock_door", p -> new DoorBlock(p.noOcclusion(), BlockSetType.STONE) {});
    public static final RegistryObject<Block> HATCH = part("exterior_hatch", p -> new TrapDoorBlock(p.noOcclusion(), BlockSetType.STONE) {});
    public static final RegistryObject<Item> WRENCH = ITEMS.register("ship_wrench", ShipWrench::new);
    public static final RegistryObject<EntityType<Ship>> SHIP = ENTITIES.register("ship", () -> EntityType.Builder.<Ship>of(Ship::new, MobCategory.MISC)
            .sized(1, 1).clientTrackingRange(12).updateInterval(1).fireImmune().build("astrodock:ship"));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "astrodock");
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("shipyard", () -> CreativeModeTab.builder()
            .title(Component.translatable("tab.astrodock.shipyard")).icon(() -> new ItemStack(CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(CORE.get()); output.accept(ENGINE.get()); output.accept(HELM.get()); output.accept(CONSOLE.get()); output.accept(AIRLOCK.get()); output.accept(HATCH.get()); output.accept(WRENCH.get()); output.accept(AstroDock.CAPSULE.get());
            }).build());

    private static RegistryObject<Block> part(String id, Function<BlockBehaviour.Properties,Block> factory) {
        var block = BLOCKS.register(id, () -> factory.apply(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops()));
        ITEMS.register(id, () -> block.get() instanceof DoorBlock ? new DoubleHighBlockItem(block.get(), new Item.Properties()) : new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus); }
}
