package dev.pullgena.modofmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.ArrayList;
import java.util.List;

public final class RadarService {
    private RadarService() {}
    /** Players in the same dimension within 256 blocks, sent once per second. */
    public static void sync(MinecraftServer server) {
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(viewer, ModPackets.Radar.TYPE)) continue;
            List<ModPackets.RadarPoint> points = new ArrayList<>();
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (viewer == other || viewer.level() != other.level()) continue;
                double dx = other.getX() - viewer.getX();
                double dz = other.getZ() - viewer.getZ();
                if (dx * dx + dz * dz > 256d * 256d) continue;
                points.add(new ModPackets.RadarPoint((int)Math.round(dx), (int)Math.round(dz),
                    other.getGameProfile().name().substring(0, Math.min(16, other.getGameProfile().name().length()))));
                if (points.size() == 8) break;
            }
            ServerPlayNetworking.send(viewer, new ModPackets.Radar(points));
        }
    }
}
