package dev.houyisbow.mixin;

import dev.houyisbow.recipe.HouyisBowSmithingRecipe;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the remaining cost before vanilla SmithingMenu consumes one item from each input slot. */
@Mixin(SmithingMenu.class)
abstract class SmithingMenuMixin {
    @Shadow
    @Nullable
    private RecipeHolder<SmithingRecipe> selectedRecipe;

    @Inject(
            method = "onTake",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V",
                    ordinal = 0
            )
    )
    private void houyisBow$consumeExtraSmithingMaterials(Player player, ItemStack output, CallbackInfo callback) {
        if (!(this.selectedRecipe != null && this.selectedRecipe.value() instanceof HouyisBowSmithingRecipe)) {
            return;
        }

        SmithingMenu menu = (SmithingMenu) (Object) this;
        Slot templateSlot = menu.getSlot(SmithingMenu.TEMPLATE_SLOT);
        Slot additionSlot = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT);
        templateSlot.remove(HouyisBowSmithingRecipe.TEMPLATE_COST - 1);
        additionSlot.remove(HouyisBowSmithingRecipe.NETHERITE_INGOT_COST - 1);
        templateSlot.setChanged();
        additionSlot.setChanged();
    }
}
