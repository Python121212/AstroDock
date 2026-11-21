package io.github.python121212.astrodock.ship;

import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

/** 数値は Minecraft のブロック・tick・ダメージ点。 */
enum ShipWeapon {
    CANNON(ShipParts.CANNON,ShipParts.SHELL,null,3.2,30,60,8,ParticleTypes.ELECTRIC_SPARK,SoundEvents.BEACON_POWER_SELECT),
    MISSILE(ShipParts.MISSILES,ShipParts.MISSILE,Items.FIREWORK_ROCKET,1.4,70,45,10,ParticleTypes.FIREWORK,SoundEvents.FIREWORK_ROCKET_LAUNCH),
    TORPEDO(ShipParts.TORPEDOES,ShipParts.TORPEDO,Items.PRISMARINE_CRYSTALS,.9,100,60,14,ParticleTypes.END_ROD,SoundEvents.TRIDENT_THROW);
    final RegistryObject<Block> mount;
    final RegistryObject<EntityType<ShipShot>> type;
    final Item ammo;final double speed;final int lifetime,delay,damage;
    final SimpleParticleType trail;final SoundEvent sound;
    ShipWeapon(RegistryObject<Block> mount,RegistryObject<EntityType<ShipShot>> type,Item ammo,double speed,int lifetime,int delay,int damage,SimpleParticleType trail,SoundEvent sound) {
        this.mount=mount;this.type=type;this.ammo=ammo;this.speed=speed;this.lifetime=lifetime;this.delay=delay;this.damage=damage;this.trail=trail;this.sound=sound;
    }
}
