package dev.houyisbow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.houyisbow.item.HouyisBowItem;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class HouYisBowTest {
    @Test
    void quickChargeNeverReducesTheDrawBelowTwoTicks() {
        assertEquals(10, HouyisBowItem.getFullDrawTicksForQuickCharge(0));
        assertEquals(8, HouyisBowItem.getFullDrawTicksForQuickCharge(1));
        assertEquals(6, HouyisBowItem.getFullDrawTicksForQuickCharge(2));
        assertEquals(4, HouyisBowItem.getFullDrawTicksForQuickCharge(3));
        assertEquals(2, HouyisBowItem.getFullDrawTicksForQuickCharge(4));
        assertEquals(2, HouyisBowItem.getFullDrawTicksForQuickCharge(30));
    }

    @Test
    void normalizesEveryFullDrawToVanillaFullPower() {
        assertEquals(0, HouyisBowItem.toVanillaChargeTicks(-1, 10));
        assertEquals(10, HouyisBowItem.toVanillaChargeTicks(5, 10));
        assertEquals(20, HouyisBowItem.toVanillaChargeTicks(10, 10));
        assertEquals(20, HouyisBowItem.toVanillaChargeTicks(99, 10));
        assertEquals(20, HouyisBowItem.toVanillaChargeTicks(2, 2));
    }

    @Test
    void projectileCountAndDurabilityFollowTheThreeArrowRule() {
        assertEquals(3, HouyisBowItem.clampProjectileCount(3));
        assertEquals(12, HouyisBowItem.clampProjectileCount(12));
        assertEquals(12, HouyisBowItem.clampProjectileCount(43));
        assertEquals(0, HouyisBowItem.clampProjectileCount(-1));
        assertEquals(1, HouyisBowItem.durabilityUseForProjectileCount(3));
        assertEquals(2, HouyisBowItem.durabilityUseForProjectileCount(4));
        assertEquals(4, HouyisBowItem.durabilityUseForProjectileCount(12));
    }

    @Test
    void usesThreeAndAHalfTimesVanillaArrowSpeed() {
        assertEquals(10.5F, HouyisBowItem.projectileVelocityForPower(1.0F), 1.0E-6F);
        assertEquals(5.25F, HouyisBowItem.projectileVelocityForPower(0.5F), 1.0E-6F);
    }

    @Test
    void placesSonicBoomParticlesAlongTheAimDirection() {
        Vec3 particlePosition = HouYisBow.calculateSonicBoomParticlePosition(
                new Vec3(1.0D, 2.0D, 3.0D),
                new Vec3(0.0D, 0.0D, 1.0D),
                4
        );

        assertEquals(1.0D, particlePosition.x, 1.0E-9D);
        assertEquals(2.0D, particlePosition.y, 1.0E-9D);
        assertEquals(7.0D, particlePosition.z, 1.0E-9D);
    }

    @Test
    void sonicBoomMasksAreNeutralAndVisible() throws IOException {
        for (int frame = 0; frame < 16; frame++) {
            String path = "/assets/houyis_bow/textures/particle/sonic_boom_mask_" + frame + ".png";
            try (var stream = HouYisBowTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var image = ImageIO.read(stream);
                int visiblePixels = 0;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        if ((argb >>> 24) != 0) {
                            visiblePixels++;
                            assertEquals(0xFFFFFF, argb & 0xFFFFFF, path);
                        }
                    }
                }
                assertTrue(visiblePixels > 0, path);
            }
        }
    }
}
