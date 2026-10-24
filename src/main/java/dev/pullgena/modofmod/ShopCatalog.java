package dev.pullgena.modofmod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** All prices and counts are server-authoritative. Client displays a static copy. */
public final class ShopCatalog {
    public record Offer(String name, Item item, int count, int price) {
        public ItemStack createStack() { return new ItemStack(item, count); }
    }

    public static final List<Offer> OFFERS = List.of(
        new Offer("빵 x8", Items.BREAD, 8, 8),
        new Offer("횃불 x16", Items.TORCH, 16, 10),
        new Offer("철 곡괭이", Items.IRON_PICKAXE, 1, 24),
        new Offer("다이아몬드 x2", Items.DIAMOND, 2, 60),
        new Offer("엔더 진주 x8", Items.ENDER_PEARL, 8, 80),
        new Offer("황금 사과 x2", Items.GOLDEN_APPLE, 2, 95),
        new Offer("불사의 토템", Items.TOTEM_OF_UNDYING, 1, 220),
        new Offer("겉날개", Items.ELYTRA, 1, 400)
    );

    private ShopCatalog() { }

    public static Offer get(int id) {
        if (id < 0 || id >= OFFERS.size()) return null;
        return OFFERS.get(id);
    }
}
