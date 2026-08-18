package dev.houyisbow;

import dev.houyisbow.item.HouyisBowItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HouYisBow.MOD_ID);
    public static final DeferredItem<HouyisBowItem> HOUYIS_BOW = ITEMS.register(
            "houyis_bow",
            () -> new HouyisBowItem(new Item.Properties().durability(1000))
    );

    private ModItems() {
    }
}
