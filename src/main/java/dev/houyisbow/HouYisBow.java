package dev.houyisbow;

import dev.houyisbow.entity.HouyisArrowEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod(HouYisBow.MOD_ID)
public final class HouYisBow {
    public static final String MOD_ID = "houyis_bow";
    private static final int SONIC_BOOM_WAVE_LENGTH = 22;
    private static final double MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-12D;

    public HouYisBow(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(HouYisBow::registerPayloadHandlers);
        modEventBus.addListener(HouYisBow::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.HOUYIS_BOW);
        }
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

    /** Sends the unclamped initial velocity which vanilla entity packets cannot represent above 3.9 blocks/tick. */
    public static void syncArrowStateTo(ServerPlayer player, HouyisArrowEntity arrow) {
        if (!NetworkRegistry.hasChannel(player.connection, ArrowStatePayload.TYPE.id())) {
            return;
        }

        PacketDistributor.sendToPlayer(player, ArrowStatePayload.from(arrow));
    }

    public static void playFullDrawEffects(ServerLevel level, Entity shooter) {
        Vec3 lookDirection = shooter.getLookAngle();
        if (lookDirection.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            return;
        }

        Vec3 aimDirection = lookDirection.normalize();
        Vec3 muzzle = new Vec3(shooter.getX(), shooter.getEyeY(), shooter.getZ())
                .add(aimDirection.scale(0.8D));
        level.playSound(
                null,
                muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                0.35F,
                1.0F
        );

        for (int distance = 1; distance < SONIC_BOOM_WAVE_LENGTH; distance++) {
            Vec3 particlePosition = calculateSonicBoomParticlePosition(muzzle, aimDirection, distance);
            SimpleParticleType particle = ModParticles.forWaveStep(distance);
            level.sendParticles(
                    particle,
                    particlePosition.x, particlePosition.y, particlePosition.z,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }

    static Vec3 calculateSonicBoomParticlePosition(Vec3 muzzle, Vec3 aimDirection, int distance) {
        return muzzle.add(aimDirection.scale(distance));
    }

    @EventBusSubscriber(modid = MOD_ID)
    static final class GameplayEvents {
        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (!(event.getEntity() instanceof ServerPlayer player)
                    || !(event.getTarget() instanceof HouyisArrowEntity arrow)) {
                return;
            }

            syncArrowStateTo(player, arrow);
        }
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

        static ArrowStatePayload from(HouyisArrowEntity arrow) {
            Vec3 position = arrow.position();
            Vec3 velocity = arrow.getDeltaMovement();
            return new ArrowStatePayload(
                    arrow.getId(),
                    position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z
            );
        }

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

        static void applyPending(Level level, int entityId) {
            PendingArrowState pending = PENDING_STATES.remove(entityId);
            if (pending != null && !apply(level, pending.payload())) {
                PENDING_STATES.put(entityId, pending);
            }
        }

        private static boolean apply(Level level, ArrowStatePayload payload) {
            if (!(level.getEntity(payload.entityId()) instanceof HouyisArrowEntity arrow)) {
                return false;
            }

            arrow.setPos(payload.positionX(), payload.positionY(), payload.positionZ());
            Vec3 velocity = new Vec3(payload.velocityX(), payload.velocityY(), payload.velocityZ());
            arrow.shoot(
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    (float) velocity.length(),
                    0.0F
            );
            arrow.setOldPosAndRot();
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
