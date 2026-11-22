package io.github.python121212.astrodock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod("astrodock")
public final class AstroDock {
    public static final ResourceKey<Level> ORBIT = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "orbit"));
    public static final ResourceKey<Level> MARS = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "mars"));
    public static final ResourceKey<Level> MERCURY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "mercury"));
    public static final ResourceKey<Level> VENUS = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "venus"));
    public static final ResourceKey<Level> JUPITER = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "jupiter"));
    public static final ResourceKey<Level> SATURN = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "saturn"));
    public static final ResourceKey<Level> URANUS = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "uranus"));
    public static final ResourceKey<Level> NEPTUNE = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "neptune"));
    public static final ResourceKey<Level> PLUTO = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("astrodock", "pluto"));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "astrodock");
    public static final RegistryObject<Item> CAPSULE = ITEMS.register("flight_capsule", FlightCapsule::new);

    public AstroDock() { io.github.python121212.astrodock.ship.ShipCommand.register(); io.github.python121212.astrodock.ship.ShipParts.register(FMLJavaModLoadingContext.get().getModEventBus()); ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus()); }

    public static boolean supported(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == ORBIT || level.dimension() == MARS || level.dimension() == MERCURY || level.dimension() == VENUS || level.dimension() == JUPITER || level.dimension() == SATURN || level.dimension() == URANUS || level.dimension() == NEPTUNE || level.dimension() == PLUTO;
    }
    public static double altitude(Level level, double y) {
        return level.dimension() == ORBIT ? Atmosphere.SPACE + y : y;
    }
}
