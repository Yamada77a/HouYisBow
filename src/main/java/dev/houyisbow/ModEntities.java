package dev.houyisbow;

import dev.houyisbow.entity.HouyisArrowEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, HouYisBow.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<HouyisArrowEntity>> HOUYIS_ARROW = ENTITIES.register(
            "houyis_arrow",
            id -> EntityType.Builder.<HouyisArrowEntity>of(HouyisArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.13F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(id.toString())
    );

    private ModEntities() {
    }
}
