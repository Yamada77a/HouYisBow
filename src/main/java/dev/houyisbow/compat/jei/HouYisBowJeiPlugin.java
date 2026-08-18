package dev.houyisbow.compat.jei;

import dev.houyisbow.HouYisBow;
import dev.houyisbow.ModItems;
import dev.houyisbow.recipe.HouyisBowSmithingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@JeiPlugin
public final class HouYisBowJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(HouYisBow.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(
                HouyisBowSmithingRecipe.class,
                new ISmithingCategoryExtension<>() {
                    @Override
                    public <T extends IIngredientAcceptor<T>> void setTemplate(
                            HouyisBowSmithingRecipe recipe, T ingredients) {
                        ingredients.addItemStack(new ItemStack(
                                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                                HouyisBowSmithingRecipe.TEMPLATE_COST
                        ));
                    }

                    @Override
                    public <T extends IIngredientAcceptor<T>> void setBase(
                            HouyisBowSmithingRecipe recipe, T ingredients) {
                        ingredients.addItemStack(new ItemStack(Items.BOW));
                    }

                    @Override
                    public <T extends IIngredientAcceptor<T>> void setAddition(
                            HouyisBowSmithingRecipe recipe, T ingredients) {
                        ingredients.addItemStack(new ItemStack(
                                Items.NETHERITE_INGOT,
                                HouyisBowSmithingRecipe.NETHERITE_INGOT_COST
                        ));
                    }

                    @Override
                    public <T extends IIngredientAcceptor<T>> void setOutput(
                            HouyisBowSmithingRecipe recipe, T ingredients) {
                        ingredients.addItemStack(new ItemStack(ModItems.HOUYIS_BOW.get()));
                    }
                }
        );
    }
}
