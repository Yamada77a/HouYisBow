package dev.houyisbow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class HouYisBowTest {
    @Test
    void convertsFourRealTicksToVanillaFullCharge() {
        assertEquals(0, HouYisBow.adjustChargeTicks(-1));
        assertEquals(0, HouYisBow.adjustChargeTicks(0));
        assertEquals(5, HouYisBow.adjustChargeTicks(1));
        assertEquals(10, HouYisBow.adjustChargeTicks(2));
        assertEquals(15, HouYisBow.adjustChargeTicks(3));
        assertEquals(20, HouYisBow.adjustChargeTicks(4));
        assertEquals(20, HouYisBow.adjustChargeTicks(5));
        assertEquals(20, HouYisBow.adjustChargeTicks(Integer.MAX_VALUE));
    }

    @Test
    void detectsTheFullDrawBoundary() {
        assertFalse(HouYisBow.isFullyDrawn(3));
        assertTrue(HouYisBow.isFullyDrawn(4));
        assertEquals(4, HouYisBow.FULL_DRAW_TICKS);
    }

    @Test
    void aimsAtTheRequestedDirectionAndTriplesSpeed() {
        Vec3 tuned = HouYisBow.calculateTunedVelocity(new Vec3(3.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D));

        assertEquals(0.0D, tuned.x, 1.0E-9D);
        assertEquals(9.0D, tuned.y, 1.0E-9D);
        assertEquals(0.0D, tuned.z, 1.0E-9D);
        assertEquals(9.0D, tuned.length(), 1.0E-9D);
    }

    @Test
    void keepsTheOriginalDirectionWhenAimIsUnavailable() {
        Vec3 tuned = HouYisBow.calculateTunedVelocity(new Vec3(0.0D, 0.0D, 2.0D), Vec3.ZERO);

        assertEquals(0.0D, tuned.x, 1.0E-9D);
        assertEquals(0.0D, tuned.y, 1.0E-9D);
        assertEquals(6.0D, tuned.z, 1.0E-9D);
    }
}

