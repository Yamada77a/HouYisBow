package dev.houyisbow.recipe;

import com.mojang.serialization.MapCodec;
import dev.houyisbow.ModItems;
import dev.houyisbow.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** Smithing table recipe with stack-count requirements that vanilla ingredients cannot express. */
public final class HouyisBowSmithingRecipe implements SmithingRecipe {
    public static final int TEMPLATE_COST = 3;
    public static final int NETHERITE_INGOT_COST = 12;
    static final HouyisBowSmithingRecipe INSTANCE = new HouyisBowSmithingRecipe();

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                && input.template().getCount() >= TEMPLATE_COST
                && input.base().is(Items.BOW)
                && input.base().getCount() >= 1
                && input.addition().is(Items.NETHERITE_INGOT)
                && input.addition().getCount() >= NETHERITE_INGOT_COST;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return input.base().transmuteCopy(ModItems.HOUYIS_BOW.get(), 1);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.HOUYIS_BOW.get());
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.is(Items.BOW);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(Items.NETHERITE_INGOT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HOUYIS_BOW_SMITHING.get();
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    public static final class Serializer implements RecipeSerializer<HouyisBowSmithingRecipe> {
        private static final MapCodec<HouyisBowSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, HouyisBowSmithingRecipe> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<HouyisBowSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HouyisBowSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
