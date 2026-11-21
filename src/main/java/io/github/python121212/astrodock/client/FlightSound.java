package io.github.python121212.astrodock.client;

import io.github.python121212.astrodock.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.*;

/** 速度・気圧で連続的に変化する音。長いサンプルを重ねて再生しない。 */
final class FlightSound extends AbstractTickableSoundInstance {
    private final LocalPlayer pilot;
    private final boolean engine;

    static boolean audible(LocalPlayer pilot, double y, boolean engine) {
        return (engine ? y >= 320 && (pilot.getUseItem().is(AstroDock.CAPSULE.get()) || pilot.getVehicle() instanceof io.github.python121212.astrodock.ship.Ship ship && ship.engineOn() && !ship.walking() && (pilot.input.up || pilot.input.down || pilot.input.jumping)) : y >= 800 && y < 5000)
                && pilot.getRootVehicle().getDeltaMovement().lengthSqr() > 0.25;
    }
    FlightSound(LocalPlayer pilot, boolean engine) {
        super(engine ? SoundEvents.BEACON_AMBIENT : SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.pilot = pilot;
        this.engine = engine;
        looping = true;
        relative = true;
        attenuation = Attenuation.NONE;
        tick();
    }
    @Override public void tick() {
        double y = AstroDock.altitude(pilot.level(), pilot.getY());
        if (Minecraft.getInstance().player != pilot || !AstroDock.supported(pilot.level()) || !audible(pilot, y, engine)) {
            stop();
            return;
        }
        float speed = (float)Atmosphere.ramp(pilot.getRootVehicle().getDeltaMovement().length(), 0.5, 3);
        float air = (float)(1 - Atmosphere.muffling(y));
        volume = speed * (engine ? 0.03f + 0.15f * air : 0.18f * air);
        pitch = engine ? 0.5f + air * 0.5f : 0.55f + speed * air;
    }
}
