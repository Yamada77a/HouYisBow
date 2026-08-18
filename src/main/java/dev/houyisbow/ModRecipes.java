package dev.houyisbow;

import dev.houyisbow.recipe.HouyisBowSmithingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER,
            HouYisBow.MOD_ID
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HouyisBowSmithingRecipe>> HOUYIS_BOW_SMITHING =
            RECIPE_SERIALIZERS.register("houyis_bow_smithing", HouyisBowSmithingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
