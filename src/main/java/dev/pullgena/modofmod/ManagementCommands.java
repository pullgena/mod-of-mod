package dev.pullgena.modofmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** OP-gated management commands. Console uses no leading slash. */
public final class ManagementCommands {
    private ManagementCommands() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(Commands.literal("관리자")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    ServerPlayer p = src.getPlayer();
                    if (p != null && ServerPlayNetworking.canSend(p, ModPackets.AdminOpen.TYPE)) {
                        ServerPlayNetworking.send(p, new ModPackets.AdminOpen(1));
                        src.sendSuccess(() -> Component.literal("[모험] 관리자 패널을 엽니다."), false);
                    } else {
                        src.sendSuccess(() -> Component.literal("[모험] 관리자 상태: "
                            + src.getServer().getPlayerList().getPlayerCount() + "명 접속 | 버전 "
                            + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ModOfMod.ID)
                                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")), false);
                    }
                    return 1;
                }));
            dispatcher.register(Commands.literal("업데이트")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .then(Commands.argument("github", StringArgumentType.greedyString()).executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    String github = StringArgumentType.getString(ctx, "github");
                    try { ReleaseUpdater.requireKnownRepo(github); }
                    catch (IllegalArgumentException error) {
                        src.sendFailure(Component.literal(error.getMessage()));
                        return 0;
                    }
                    src.sendSuccess(() -> Component.literal("[모험] 공식 릴리스 검사 및 검증을 시작합니다."), false);
                    ReleaseUpdater.updateAsync(github).thenAccept(result -> src.getServer().execute(() ->
                        src.sendSuccess(() -> Component.literal("[모험] " + result), false)));
                    return 1;
                })));
        });
    }
}
