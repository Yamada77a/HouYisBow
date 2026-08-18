package dev.houyisbow;

import dev.houyisbow.client.HouyiSonicBoomParticle;
import dev.houyisbow.client.HouyisArrowRenderer;
import dev.houyisbow.item.HouyisBowItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value = HouYisBow.MOD_ID, dist = Dist.CLIENT)
public final class HouYisBowClient {
    private static final ResourceLocation PULL = ResourceLocation.withDefaultNamespace("pull");
    private static final ResourceLocation PULLING = ResourceLocation.withDefaultNamespace("pulling");

    public HouYisBowClient(IEventBus modEventBus) {
        modEventBus.addListener(HouYisBowClient::onClientSetup);
        modEventBus.addListener(HouYisBowClient::registerRenderers);
        modEventBus.addListener(HouYisBowClient::registerParticleProviders);
        NeoForge.EVENT_BUS.addListener(HouYisBowClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(HouYisBowClient::onEntityJoinLevel);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.HOUYIS_BOW.get(), PULL, (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }

                int usedTicks = stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
                return Math.min(1.0F, (float) usedTicks / HouyisBowItem.getFullDrawTicks(stack, entity));
            });
            ItemProperties.register(ModItems.HOUYIS_BOW.get(), PULLING, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
            );
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOUYIS_ARROW.get(), HouyisArrowRenderer::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.SONIC_BOOM_GOLD_LIGHT.get(),
                sprites -> HouyiSonicBoomParticle.provider(sprites, 1.0F, 0.96F, 0.62F)
        );
        event.registerSpriteSet(
                ModParticles.SONIC_BOOM_GOLD.get(),
                sprites -> HouyiSonicBoomParticle.provider(sprites, 0.99F, 0.66F, 0.04F)
        );
        event.registerSpriteSet(
                ModParticles.SONIC_BOOM_AMBER.get(),
                sprites -> HouyiSonicBoomParticle.provider(sprites, 0.97F, 0.40F, 0.01F)
        );
        event.registerSpriteSet(
                ModParticles.SONIC_BOOM_DEEP_GOLD.get(),
                sprites -> HouyiSonicBoomParticle.provider(sprites, 0.83F, 0.47F, 0.04F)
        );
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        HouYisBow.ArrowStatePayload.flushPending(Minecraft.getInstance().level);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            HouYisBow.ArrowStatePayload.applyPending(event.getLevel(), event.getEntity().getId());
        }
    }
}
