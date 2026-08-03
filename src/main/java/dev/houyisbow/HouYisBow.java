package dev.houyisbow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod(HouYisBow.MOD_ID)
public final class HouYisBow {
    public static final String MOD_ID = "houyis_bow";
    public static final double ARROW_SPEED_MULTIPLIER = 3.0D;
    public static final int VANILLA_FULL_DRAW_TICKS = 20;
    public static final int FULL_DRAW_TICKS = 4;
    public static final int CHARGE_TICK_MULTIPLIER = VANILLA_FULL_DRAW_TICKS / FULL_DRAW_TICKS;
    private static final double MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-12D;

    public HouYisBow(IEventBus modEventBus) {
        modEventBus.addListener(HouYisBow::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("0.0.1")
                .optional()
                .playToClient(
                        ArrowStatePayload.TYPE,
                        ArrowStatePayload.STREAM_CODEC,
                        ArrowStatePayload::handle
                );
    }

    @EventBusSubscriber(modid = MOD_ID)
    static final class GameplayEvents {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onArrowLoose(ArrowLooseEvent event) {
            if (!(event.getBow().getItem() instanceof BowItem)) {
                return;
            }

            int originalChargeTicks = event.getCharge();
            if (event.getLevel() instanceof ServerLevel serverLevel
                    && isFullyDrawn(originalChargeTicks)) {
                playFullDrawEffects(serverLevel, event.getEntity());
            }

            event.setCharge(adjustChargeTicks(originalChargeTicks));
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
                return;
            }

            if (event.getEntity() instanceof AbstractArrow arrow
                    && arrow.getWeaponItem() != null
                    && arrow.getWeaponItem().getItem() instanceof BowItem) {
                aimArrowAtShooterCrosshair(arrow);
            }
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (!(event.getEntity() instanceof ServerPlayer player)
                    || !(event.getTarget() instanceof AbstractArrow arrow)
                    || arrow.getWeaponItem() == null
                    || !(arrow.getWeaponItem().getItem() instanceof BowItem)
                    || !NetworkRegistry.hasChannel(player.connection, ArrowStatePayload.TYPE.id())) {
                return;
            }

            Vec3 position = arrow.position();
            Vec3 velocity = arrow.getDeltaMovement();
            PacketDistributor.sendToPlayer(
                    player,
                    new ArrowStatePayload(
                            arrow.getId(),
                            position.x, position.y, position.z,
                            velocity.x, velocity.y, velocity.z
                    )
            );
        }
    }

    static int adjustChargeTicks(int originalChargeTicks) {
        int clampedChargeTicks = Math.max(0, Math.min(FULL_DRAW_TICKS, originalChargeTicks));
        return clampedChargeTicks * CHARGE_TICK_MULTIPLIER;
    }

    static boolean isFullyDrawn(int originalChargeTicks) {
        return originalChargeTicks >= FULL_DRAW_TICKS;
    }

    static Vec3 calculateTunedVelocity(Vec3 originalVelocity, Vec3 requestedAimDirection) {
        if (originalVelocity.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            return originalVelocity;
        }

        Vec3 aimDirection = requestedAimDirection.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED
                ? originalVelocity.normalize()
                : requestedAimDirection.normalize();
        return aimDirection.scale(originalVelocity.length() * ARROW_SPEED_MULTIPLIER);
    }

    private static void playFullDrawEffects(ServerLevel level, Entity shooter) {
        Vec3 lookDirection = shooter.getLookAngle();
        if (lookDirection.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            return;
        }

        Vec3 muzzle = new Vec3(shooter.getX(), shooter.getEyeY(), shooter.getZ())
                .add(lookDirection.normalize().scale(0.8D));
        level.playSound(
                null,
                muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        level.sendParticles(
                ParticleTypes.FIREWORK,
                muzzle.x, muzzle.y, muzzle.z,
                24,
                0.12D, 0.12D, 0.12D,
                0.08D
        );
    }

    private static void aimArrowAtShooterCrosshair(AbstractArrow arrow) {
        Vec3 originalVelocity = arrow.getDeltaMovement();
        if (originalVelocity.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            return;
        }

        Entity shooter = arrow.getOwner();
        Vec3 requestedAimDirection = shooter == null ? originalVelocity : shooter.getLookAngle();
        Vec3 tunedVelocity = calculateTunedVelocity(originalVelocity, requestedAimDirection);
        arrow.shoot(
                tunedVelocity.x,
                tunedVelocity.y,
                tunedVelocity.z,
                (float) tunedVelocity.length(),
                0.0F
        );
    }

    record ArrowStatePayload(
            int entityId,
            double positionX,
            double positionY,
            double positionZ,
            double velocityX,
            double velocityY,
            double velocityZ
    ) implements CustomPacketPayload {
        private static final int MAX_PENDING_TICKS = 20;
        private static final Map<Integer, PendingArrowState> PENDING_STATES = new HashMap<>();
        static final Type<ArrowStatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "arrow_state")
        );
        static final StreamCodec<RegistryFriendlyByteBuf, ArrowStatePayload> STREAM_CODEC =
                CustomPacketPayload.codec(ArrowStatePayload::write, ArrowStatePayload::decode);

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(entityId);
            buffer.writeDouble(positionX);
            buffer.writeDouble(positionY);
            buffer.writeDouble(positionZ);
            buffer.writeDouble(velocityX);
            buffer.writeDouble(velocityY);
            buffer.writeDouble(velocityZ);
        }

        private static ArrowStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new ArrowStatePayload(
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        private static void handle(ArrowStatePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!apply(context.player().level(), payload)) {
                    PENDING_STATES.put(
                            payload.entityId(),
                            new PendingArrowState(payload, MAX_PENDING_TICKS)
                    );
                }
            });
        }

        static void flushPending(Level level) {
            if (level == null) {
                PENDING_STATES.clear();
                return;
            }

            Iterator<Map.Entry<Integer, PendingArrowState>> iterator = PENDING_STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, PendingArrowState> entry = iterator.next();
                PendingArrowState pending = entry.getValue();
                if (apply(level, pending.payload()) || pending.remainingTicks() <= 1) {
                    iterator.remove();
                } else {
                    entry.setValue(new PendingArrowState(pending.payload(), pending.remainingTicks() - 1));
                }
            }
        }

        private static boolean apply(Level level, ArrowStatePayload payload) {
            if (!(level.getEntity(payload.entityId()) instanceof AbstractArrow arrow)) {
                return false;
            }

            arrow.setPos(payload.positionX(), payload.positionY(), payload.positionZ());
            arrow.setOldPosAndRot();
            arrow.setDeltaMovement(payload.velocityX(), payload.velocityY(), payload.velocityZ());
            return true;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private record PendingArrowState(ArrowStatePayload payload, int remainingTicks) {
        }
    }
}

