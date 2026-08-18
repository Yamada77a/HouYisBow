package dev.houyisbow.entity;

import dev.houyisbow.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A normal-damage arrow that uses the vanilla spectral-arrow visual without its glowing effect. */
public final class HouyisArrowEntity extends AbstractArrow {
    private static final double TRAIL_SPACING = 0.9D;
    private static final int MAX_TRAIL_PARTICLES_PER_TICK = 12;
    private static final int GROUND_DESPAWN_TICKS = 100;
    private static final double BASE_KNOCKBACK = 4.0D;
    private boolean trailStopped;

    public HouyisArrowEntity(EntityType<? extends HouyisArrowEntity> type, Level level) {
        super(type, level);
    }

    public HouyisArrowEntity(Level level, LivingEntity owner, ItemStack pickupItemStack, ItemStack firedFromWeapon) {
        super(ModEntities.HOUYIS_ARROW.get(), owner, level, pickupItemStack, firedFromWeapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.trailStopped
                && !this.isRemoved()
                && !this.inGround
                && this.level() instanceof ServerLevel serverLevel) {
            this.emitFireworkTrail(serverLevel);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        this.trailStopped = true;
        super.onHit(result);
    }

    public void disableTrail() {
        this.trailStopped = true;
    }

    @Override
    protected void tickDespawn() {
        if (++this.inGroundTime >= GROUND_DESPAWN_TICKS) {
            this.discard();
        }
    }

    @Override
    protected void doKnockback(LivingEntity target, DamageSource damageSource) {
        double knockback = BASE_KNOCKBACK;
        ItemStack weapon = this.getWeaponItem();
        if (weapon != null && this.level() instanceof ServerLevel serverLevel) {
            knockback += EnchantmentHelper.modifyKnockback(serverLevel, weapon, target, damageSource, 0.0F);
        }

        double resistance = Math.max(0.0D, 1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(knockback * 0.6D * resistance);
        if (push.lengthSqr() > 0.0D) {
            target.push(push.x, 0.1D, push.z);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TrailStopped", this.trailStopped);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.trailStopped = tag.getBoolean("TrailStopped");
    }

    private void emitFireworkTrail(ServerLevel level) {
        Vec3 velocity = this.getDeltaMovement();
        int particleCount = Math.min(
                MAX_TRAIL_PARTICLES_PER_TICK,
                Math.max(1, (int) Math.ceil(velocity.length() / TRAIL_SPACING))
        );
        for (int index = 0; index < particleCount; index++) {
            Vec3 particlePosition = this.position().subtract(velocity.scale((double) index / particleCount));
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    particlePosition.x, particlePosition.y, particlePosition.z,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }
}
