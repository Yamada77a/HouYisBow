package dev.houyisbow.item;

import dev.houyisbow.HouYisBow;
import dev.houyisbow.entity.HouyisArrowEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/** The independent bow item; vanilla BowItem behavior remains untouched. */
public final class HouyisBowItem extends BowItem {
    static final int BASE_PROJECTILE_COUNT = 3;
    static final int MAX_PROJECTILE_COUNT = 12;
    static final int BASE_DRAW_TICKS = 10;
    static final int MIN_DRAW_TICKS = 2;
    static final float VANILLA_BOW_SPEED = 3.0F;
    static final float ARROW_SPEED_MULTIPLIER = 3.5F;

    public HouyisBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void releaseUsing(ItemStack weapon, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) {
            return;
        }

        ItemStack ammo = player.getProjectile(weapon);
        if (ammo.isEmpty()) {
            return;
        }

        int usedTicks = this.getUseDuration(weapon, user) - timeLeft;
        usedTicks = EventHooks.onArrowLoose(weapon, level, player, usedTicks, true);
        if (usedTicks < 0) {
            return;
        }

        int fullDrawTicks = getFullDrawTicks(weapon, user);
        int vanillaChargeTicks = toVanillaChargeTicks(usedTicks, fullDrawTicks);
        float power = getPowerForTime(vanillaChargeTicks);
        if (power < 0.1F) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            List<ItemStack> projectiles = drawProjectiles(serverLevel, weapon, ammo, player);
            if (!projectiles.isEmpty()) {
                this.shoot(
                        serverLevel,
                        player,
                        player.getUsedItemHand(),
                        weapon,
                        projectiles,
                        projectileVelocityForPower(power),
                        0.0F,
                        power == 1.0F,
                        null
                );
                if (usedTicks >= fullDrawTicks) {
                    HouYisBow.playFullDrawEffects(serverLevel, player);
                }
            }
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    protected void shoot(
            ServerLevel level,
            LivingEntity shooter,
            InteractionHand hand,
            ItemStack weapon,
            List<ItemStack> projectileItems,
            float velocity,
            float inaccuracy,
            boolean isCrit,
            @Nullable LivingEntity target
    ) {
        int fired = 0;
        for (int index = 0; index < projectileItems.size(); index++) {
            ItemStack ammo = projectileItems.get(index);
            if (ammo.isEmpty()) {
                continue;
            }

            Projectile projectile = this.createProjectile(level, shooter, weapon, ammo, isCrit);
            if (fired > 0 && projectile instanceof HouyisArrowEntity arrow) {
                arrow.disableTrail();
            }
            this.shootProjectile(shooter, projectile, index, velocity, inaccuracy, 0.0F, target);
            level.addFreshEntity(projectile);
            if (projectile instanceof HouyisArrowEntity arrow && shooter instanceof ServerPlayer player) {
                HouYisBow.syncArrowStateTo(player, arrow);
            }
            fired++;
        }

        if (fired > 0) {
            weapon.hurtAndBreak(
                    durabilityUseForProjectileCount(fired),
                    shooter,
                    LivingEntity.getSlotForHand(hand)
            );
        }
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        ItemStack pickupArrow = new ItemStack(Items.ARROW);
        if (ammo.has(DataComponents.INTANGIBLE_PROJECTILE)) {
            pickupArrow.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
        }

        HouyisArrowEntity arrow = new HouyisArrowEntity(level, shooter, pickupArrow, weapon);
        if (isCrit) {
            arrow.setCritArrow(true);
        }
        return arrow;
    }

    @Override
    protected void shootProjectile(
            LivingEntity shooter,
            Projectile projectile,
            int index,
            float velocity,
            float inaccuracy,
            float angle,
            @Nullable LivingEntity target
    ) {
        // The three (or more) arrows deliberately share one exact crosshair direction.
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity, 0.0F);
    }

    public static int getFullDrawTicks(ItemStack stack, @Nullable LivingEntity user) {
        if (user == null) {
            return BASE_DRAW_TICKS;
        }

        Holder<Enchantment> quickCharge = user.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.QUICK_CHARGE);
        return getFullDrawTicksForQuickCharge(stack.getEnchantmentLevel(quickCharge));
    }

    public static int getFullDrawTicksForQuickCharge(int quickChargeLevel) {
        return Math.max(MIN_DRAW_TICKS, BASE_DRAW_TICKS - Math.max(0, quickChargeLevel) * 2);
    }

    public static int toVanillaChargeTicks(int usedTicks, int fullDrawTicks) {
        if (fullDrawTicks <= 0) {
            return 20;
        }
        int clampedUsedTicks = Math.max(0, Math.min(usedTicks, fullDrawTicks));
        return clampedUsedTicks * 20 / fullDrawTicks;
    }

    public static int durabilityUseForProjectileCount(int projectileCount) {
        return Math.max(0, (projectileCount + BASE_PROJECTILE_COUNT - 1) / BASE_PROJECTILE_COUNT);
    }

    public static int clampProjectileCount(int projectileCount) {
        return Math.max(0, Math.min(MAX_PROJECTILE_COUNT, projectileCount));
    }

    public static float projectileVelocityForPower(float power) {
        return power * VANILLA_BOW_SPEED * ARROW_SPEED_MULTIPLIER;
    }

    private static List<ItemStack> drawProjectiles(
            ServerLevel level,
            ItemStack weapon,
            ItemStack ammo,
            LivingEntity shooter
    ) {
        int projectileCount = clampProjectileCount(
                EnchantmentHelper.processProjectileCount(level, weapon, shooter, BASE_PROJECTILE_COUNT)
        );
        if (projectileCount <= 0) {
            return List.of();
        }

        List<ItemStack> projectiles = new ArrayList<>(projectileCount);
        ItemStack extraAmmo = ammo.copy();
        for (int index = 0; index < projectileCount; index++) {
            ItemStack projectile = useAmmo(weapon, index == 0 ? ammo : extraAmmo, shooter, index > 0);
            if (!projectile.isEmpty()) {
                projectiles.add(projectile);
            }
        }
        return projectiles;
    }
}
