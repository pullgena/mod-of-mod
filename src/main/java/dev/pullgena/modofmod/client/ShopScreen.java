package dev.pullgena.modofmod.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.pullgena.modofmod.ModPackets;
import dev.pullgena.modofmod.ShopCatalog;

/** Dedicated shop UI; paper-shaped coin balance is visible only here. */
public final class ShopScreen extends Screen {
    public ShopScreen() { super(Component.literal("모험가 상점")); }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = height / 2 - 79;
        for (int index = 0; index < ShopCatalog.OFFERS.size(); index++) {
            final int item = index;
            ShopCatalog.Offer offer = ShopCatalog.OFFERS.get(index);
            int x = cx - 154 + (index % 2) * 157;
            int y = top + 31 + (index / 2) * 27;
            addRenderableWidget(Button.builder(
                Component.literal(offer.name() + "  " + offer.price() + "C"),
                button -> ClientPlayNetworking.send(new ModPackets.ShopRequest(item)))
                .bounds(x, y, 150, 22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("상점 닫기"),
            button -> minecraft.gui.setScreen(null))
            .bounds(cx - 45, top + 148, 90, 20).build());
        ClientPlayNetworking.send(new ModPackets.ShopRequest(-1));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Super renders widgets. The paper design sits around, not over, their clickable surfaces.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = width / 2;
        int top = height / 2 - 79;
        graphics.centeredText(font, "✦ 모험가 상점 ✦", cx, top + 13, 0xFFFFE3A0);
        // Top-left torn-paper coin display is exclusive to the open shop.
        ClientState.drawPaper(graphics, 8, 8, 152, 45);
        graphics.text(font, "✦ 보유 코인", 21, 18, 0xFF65503B, true);
        graphics.text(font, (ClientState.coins < 0 ? "동기화 중..." : ClientState.coins + " 코인"), 23, 35,
            0xFFAA7825, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
