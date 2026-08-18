package dev.houyisbow;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, HouYisBow.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BOOM_GOLD_LIGHT = PARTICLES.register(
            "sonic_boom_gold_light", () -> new SimpleParticleType(false)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BOOM_GOLD = PARTICLES.register(
            "sonic_boom_gold", () -> new SimpleParticleType(false)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BOOM_AMBER = PARTICLES.register(
            "sonic_boom_amber", () -> new SimpleParticleType(false)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BOOM_DEEP_GOLD = PARTICLES.register(
            "sonic_boom_deep_gold", () -> new SimpleParticleType(false)
    );

    public static SimpleParticleType forWaveStep(int distance) {
        return switch (Math.floorMod(distance, 4)) {
            case 0 -> SONIC_BOOM_GOLD_LIGHT.get();
            case 1 -> SONIC_BOOM_GOLD.get();
            case 2 -> SONIC_BOOM_AMBER.get();
            default -> SONIC_BOOM_DEEP_GOLD.get();
        };
    }

    private ModParticles() {
    }
}
