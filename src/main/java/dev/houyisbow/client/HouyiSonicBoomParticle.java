package dev.houyisbow.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SonicBoomParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Vanilla sonic-boom geometry and animation, recolored to the bow's gold palette. */
@OnlyIn(Dist.CLIENT)
public final class HouyiSonicBoomParticle extends SonicBoomParticle {
    private HouyiSonicBoomParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double quadSizeMultiplier,
            SpriteSet sprites,
            float red,
            float green,
            float blue
    ) {
        super(level, x, y, z, quadSizeMultiplier, sprites);
        this.setColor(red, green, blue);
    }

    public static ParticleProvider<SimpleParticleType> provider(
            SpriteSet sprites,
            float red,
            float green,
            float blue
    ) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new HouyiSonicBoomParticle(
                level,
                x,
                y,
                z,
                xSpeed,
                sprites,
                red,
                green,
                blue
        );
    }
}
