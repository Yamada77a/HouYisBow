package dev.houyisbow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HouYisBow.MOD_ID, dist = Dist.CLIENT)
public final class HouYisBowClient {
    public HouYisBowClient(IEventBus modEventBus) {
        modEventBus.addListener(HouYisBowClient::onClientSetup);
        NeoForge.EVENT_BUS.addListener(HouYisBowClient::onClientTick);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                Items.BOW,
                ResourceLocation.withDefaultNamespace("pull"),
                (ItemPropertyFunction) (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }

                    int usedTicks = stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
                    return Math.min(1.0F, (float) usedTicks / HouYisBow.FULL_DRAW_TICKS);
                }
        ));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        HouYisBow.ArrowStatePayload.flushPending(Minecraft.getInstance().level);
    }
}

