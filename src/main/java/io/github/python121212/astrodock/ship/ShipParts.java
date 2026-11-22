package io.github.python121212.astrodock.ship;

import java.util.function.Function;
import io.github.python121212.astrodock.AstroDock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ShipParts {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "astrodock");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "astrodock");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "astrodock");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"astrodock");
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"astrodock");
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<CoreMenu>> CORE_MENU=MENUS.register("ship_core",()->
            net.minecraftforge.common.extensions.IForgeMenuType.create((id,inv,buf)->new CoreMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<Block> CORE = part("ship_core", ShipCoreBlock::new), ENGINE = part("thruster", Block::new);
    public static final RegistryObject<Block> PLANT=part("engine_core",EngineCoreBlock::new), DRIVE=part("power_unit",Block::new),
            GENERATOR=part("generator",Block::new), COOLER=part("cooling_unit",Block::new);
    public static final RegistryObject<Block> STRATASTEEL_ORE = part("stratasteel_ore", p -> new Block(p.strength(3,3).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BLACKSHELL_ORE = part("blackshell_ore", p -> new Block(p.strength(4.5f,4.5f).sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> LAMINATED_ARMOR = part("laminated_armor", p -> new Block(p.strength(4,16).sound(SoundType.METAL)));
    public static final RegistryObject<Block> BLACKSHELL_ARMOR = part("blackshell_armor", p -> new Block(p.strength(8,48).sound(SoundType.METAL)));
    public static final RegistryObject<Item> STRATASTEEL_FLAKE = ITEMS.register("stratasteel_flake", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_BLACKSHELL = ITEMS.register("raw_blackshell", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACKSHELL_INGOT = ITEMS.register("blackshell_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STRATASTEEL_PLATE = ITEMS.register("stratasteel_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACKSHELL_PLATE = ITEMS.register("blackshell_plate", () -> new Item(new Item.Properties()));
    static final RegistryObject<BlockEntityType<EngineAnchor>> ANCHOR=TILES.register("engine_core",()->BlockEntityType.Builder.of(EngineAnchor::new,PLANT.get()).build(null));
    public static final RegistryObject<Block> CONSOLE = part("engine_console", Block::new);
    public static final RegistryObject<Block> GRAVITY_CONTROLLER = part("gravity_controller", Block::new);
    public static final RegistryObject<Block> HELM = part("helm", HelmBlock::new);
    public static final RegistryObject<Block> AIRLOCK = part("airlock_door", p -> new DoorBlock(p.noOcclusion(), BlockSetType.STONE) {});
    public static final RegistryObject<Block> HATCH = part("exterior_hatch", p -> new TrapDoorBlock(p.noOcclusion(), BlockSetType.STONE) {});
    public static final RegistryObject<Block> CANNON = part("main_cannon", MainCannonBlock::new), MISSILES = part("missile_launcher", Block::new), TORPEDOES = part("torpedo_tube", Block::new);
    public static final RegistryObject<BlockEntityType<CannonMount>> CANNON_MOUNT=TILES.register("main_cannon",()->
            BlockEntityType.Builder.of(CannonMount::new,CANNON.get()).build(null));
    public static final RegistryObject<EntityType<ShipShot>> SHELL = shot("cannon_shot"), MISSILE = shot("missile"), TORPEDO = shot("torpedo");
    private static RegistryObject<EntityType<ShipShot>> shot(String id) {
        return ENTITIES.register(id, () -> EntityType.Builder.<ShipShot>of(ShipShot::new,MobCategory.MISC)
                .sized(.3f,.3f).clientTrackingRange(8).updateInterval(1).noSummon().noSave().build("astrodock:"+id));
    }
    public static final RegistryObject<Item> WRENCH = ITEMS.register("ship_wrench", ShipWrench::new);
    public static final RegistryObject<EntityType<Ship>> SHIP = ENTITIES.register("ship", () -> EntityType.Builder.<Ship>of(Ship::new, MobCategory.MISC)
            .sized(1, 1).clientTrackingRange(12).updateInterval(1).fireImmune().build("astrodock:ship"));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "astrodock");
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("shipyard", () -> CreativeModeTab.builder()
            .title(Component.translatable("tab.astrodock.shipyard")).icon(() -> new ItemStack(CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(CORE.get()); output.accept(ENGINE.get()); output.accept(HELM.get()); output.accept(CONSOLE.get()); output.accept(AIRLOCK.get()); output.accept(HATCH.get()); output.accept(CANNON.get()); output.accept(MISSILES.get()); output.accept(TORPEDOES.get()); output.accept(WRENCH.get()); output.accept(AstroDock.CAPSULE.get());
                output.accept(PLANT.get());output.accept(DRIVE.get());output.accept(GENERATOR.get());output.accept(COOLER.get());output.accept(GRAVITY_CONTROLLER.get());
                output.accept(STRATASTEEL_ORE.get());
                output.accept(BLACKSHELL_ORE.get());
                output.accept(LAMINATED_ARMOR.get());
                output.accept(BLACKSHELL_ARMOR.get());
                output.accept(STRATASTEEL_FLAKE.get());
                output.accept(RAW_BLACKSHELL.get());
                output.accept(BLACKSHELL_INGOT.get());
                output.accept(IRON_PLATE.get());
                output.accept(STRATASTEEL_PLATE.get());
                output.accept(BLACKSHELL_PLATE.get());
            }).build());

    private static RegistryObject<Block> part(String id, Function<BlockBehaviour.Properties,Block> factory) {
        var block = BLOCKS.register(id, () -> factory.apply(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops()));
        ITEMS.register(id, () -> block.get() instanceof DoorBlock ? new DoubleHighBlockItem(block.get(), new Item.Properties()) : new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TILES.register(bus); MENUS.register(bus); TABS.register(bus); }
}
