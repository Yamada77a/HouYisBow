package dev.houyisbow.client;

import dev.houyisbow.entity.HouyisArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HouyisArrowRenderer extends ArrowRenderer<HouyisArrowEntity> {
    private static final ResourceLocation SPECTRAL_ARROW_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/projectiles/spectral_arrow.png"
    );

    public HouyisArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HouyisArrowEntity arrow) {
        return SPECTRAL_ARROW_TEXTURE;
    }
}
