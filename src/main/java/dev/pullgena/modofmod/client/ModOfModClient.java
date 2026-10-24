package dev.pullgena.modofmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pullgena.modofmod.ModOfMod;
import dev.pullgena.modofmod.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModOfModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModOfMod.ID + "-client");
    private final KeyMapping.Category category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(ModOfMod.ID, "general")
    );
    private final KeyMapping shop = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.modofmod.shop", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category)
    );
    private final KeyMapping map = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.modofmod.radar", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, category)
    );

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.Balance.TYPE, (payload, context) ->
            context.client().execute(() -> ClientState.coins = Math.max(0, payload.coins())));
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.Achievement.TYPE, (payload, context) ->
            context.client().execute(() -> ClientState.achievement(payload.title(), payload.sentence(), payload.reward())));
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.Radar.TYPE, (payload, context) ->
            context.client().execute(() -> ClientState.setRadar(payload.points())));
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.AdminOpen.TYPE, (payload, context) ->
            context.client().execute(() -> {
                if (payload.authorized() == 1) context.client().gui.setScreen(new AdminScreen());
            }));
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                Screens.getWidgets(screen).add(Button.builder(
                    Component.literal("Mod of Mod 업데이트"), b -> minecraft.gui.setScreen(new UpdateScreen(screen)))
                    .bounds(width-162,height-27,154,20).build());
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ModOfMod.ID, "adventure_overlay"),
            (graphics, delta) -> ClientState.drawHud(graphics));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (shop.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    if (!ClientPlayNetworking.canSend(ModPackets.ShopRequest.TYPE)) {
                        client.player.sendSystemMessage(Component.literal("[상점] 서버에도 Mod of Mod가 필요합니다."));
                        continue;
                    }
                    client.gui.setScreen(new ShopScreen());
                }
            }
            while (map.consumeClick()) {
                if (client.player != null) {
                    ClientState.toggleRadar();
                    client.player.sendSystemMessage(Component.literal("[레이더] " +
                        (ClientState.isRadarShown() ? "켜짐" : "꺼짐")));
                }
            }
        });
        LOGGER.info("Client UI, store key B and radar key J registered.");
    }
}
