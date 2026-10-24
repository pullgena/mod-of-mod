package dev.pullgena.modofmod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Registered on both physical sides; contains no client-only classes. */
public final class ModPackets {
    private ModPackets() { }

    public record Balance(int coins) implements CustomPacketPayload {
        public static final Type<Balance> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModOfMod.ID, "balance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Balance> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, Balance::coins, Balance::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Achievement(String title, String sentence, int reward) implements CustomPacketPayload {
        public static final Type<Achievement> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModOfMod.ID, "achievement"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Achievement> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Achievement::title,
                ByteBufCodecs.STRING_UTF8, Achievement::sentence,
                ByteBufCodecs.VAR_INT, Achievement::reward, Achievement::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** itemIndex -1 means 'request current balance', 0...n means purchase. */
    public record ShopRequest(int itemIndex) implements CustomPacketPayload {
        public static final Type<ShopRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModOfMod.ID, "shop_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopRequest> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ShopRequest::itemIndex, ShopRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Balance.TYPE, Balance.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Achievement.TYPE, Achievement.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShopRequest.TYPE, ShopRequest.CODEC);
    }
}
