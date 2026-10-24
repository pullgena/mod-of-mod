package dev.pullgena.modofmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModOfMod implements ModInitializer {
    public static final String ID = "modofmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static ServerData data;
    private static int tickCounter;

    @Override
    public void onInitialize() {
        ModPackets.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> data = ServerData.load());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (data != null) data.save();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            server.execute(() -> {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (data == null || overworld == null) return;
                ServerPlayer player = handler.player;
                data.firstJoin(player, overworld);
                syncBalance(player);
            }));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (data != null) {
                ServerLevel overworld = newPlayer.level().getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) data.ensureMap(newPlayer, overworld);
                syncBalance(newPlayer);
            }
        });

        // Only 2 players and ~125 vanilla advancements; check once per second, not every tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (data == null || ++tickCounter % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkAdvancements(server, player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ShopRequest.TYPE, (packet, context) ->
            context.server().execute(() -> {
                if (data == null) return;
                int request = packet.itemIndex();
                ServerPlayer player = context.player();
                if (request == -1) syncBalance(player);
                else buy(player, request);
            }));

        LOGGER.info("Mod of Mod registered: advancement rewards, 2-player random spawn, store.");
    }

    private static void checkAdvancements(MinecraftServer server, ServerPlayer player) {
        for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
            if (!advancement.id().getNamespace().equals("minecraft")) continue;
            if (advancement.id().getPath().startsWith("recipes/")) continue;
            if (advancement.value().display().isEmpty()) continue;
            if (!player.getAdvancements().getOrStartProgress(advancement).isDone()) continue;
            String id = advancement.id().toString();
            int reward = AdvancementMessages.coins(advancement);
            if (data.award(player.getUUID(), id, reward)) {
                String title = AdvancementMessages.name(advancement);
                String description = AdvancementMessages.message(advancement);
                if (ServerPlayNetworking.canSend(player, ModPackets.Achievement.TYPE)) {
                    ServerPlayNetworking.send(player, new ModPackets.Achievement(title, description, reward));
                } else {
                    player.sendSystemMessage(Component.literal("[발전과제] " + title + " — " + description
                        + " [" + reward + "코인]+"));
                }
                syncBalance(player);
            }
        }
    }

    public static void syncBalance(ServerPlayer player) {
        if (data != null && ServerPlayNetworking.canSend(player, ModPackets.Balance.TYPE)) {
            ServerPlayNetworking.send(player, new ModPackets.Balance(data.balance(player.getUUID())));
        }
    }

    private static void buy(ServerPlayer player, int index) {
        ShopCatalog.Offer offer = ShopCatalog.get(index);
        if (offer == null) return; // Ignore malicious/invalid item IDs.
        if (data.balance(player.getUUID()) < offer.price()) {
            player.sendSystemMessage(Component.literal("[상점] 코인이 부족합니다."));
            syncBalance(player);
            return;
        }
        if (player.getInventory().getFreeSlot() < 0) {
            player.sendSystemMessage(Component.literal("[상점] 빈 인벤토리 칸이 필요합니다."));
            return;
        }
        if (!data.spend(player.getUUID(), offer.price())) {
            player.sendSystemMessage(Component.literal("[상점] 코인 차감에 실패했습니다."));
            syncBalance(player);
            return;
        }
        var stack = offer.createStack();
        if (!player.getInventory().add(stack)) {
            // Normally impossible because a free slot was checked.
            data.refund(player.getUUID(), offer.price());
            player.sendSystemMessage(Component.literal("[상점] 인벤토리 지급에 실패하여 코인을 환불했습니다."));
        } else {
            player.sendSystemMessage(Component.literal("[상점] " + offer.name() + " 구매 완료!"));
        }
        syncBalance(player);
    }
}
