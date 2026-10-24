package dev.pullgena.modofmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Mod data on the server thread; disk writes are atomic where supported. */
public final class ServerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Root root;

    private static class Root {
        Map<String, Record> players = new HashMap<>();
    }

    private static class Record {
        int coins;
        boolean spawned;
        int mapId = -1;
        Set<String> awarded = new HashSet<>();
        int spawnX;
        int spawnZ;
    }

    private ServerData(Path file, Root root) {
        this.file = file;
        this.root = root;
    }

    public static ServerData load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("modofmod-players.json");
        Root data = new Root();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Root loaded = GSON.fromJson(reader, Root.class);
                if (loaded != null && loaded.players != null) data = loaded;
            } catch (Exception ex) {
                ModOfMod.LOGGER.error("Player data load failed; do not overwrite until repaired: " + path, ex);
                throw new IllegalStateException("Player data corrupted, restore from backup before starting", ex);
            }
        }
        return new ServerData(path, data);
    }

    private Record player(UUID uuid) {
        Record r = root.players.computeIfAbsent(uuid.toString(), key -> new Record());
        if (r.awarded == null) r.awarded = new HashSet<>();
        return r;
    }

    public int balance(UUID id) {
        return player(id).coins;
    }

    public boolean award(UUID id, String advancement, int amount) {
        if (amount < 3 || amount > 1000) return false;
        Record r = player(id);
        if (!r.awarded.add(advancement)) return false;
        int previous = r.coins;
        r.coins = Math.addExact(r.coins, amount);
        try {
            save();
            return true;
        } catch (RuntimeException ex) {
            r.coins = previous;
            r.awarded.remove(advancement);
            ModOfMod.LOGGER.error("Failed to save advancement reward", ex);
            return false;
        }
    }

    public boolean spend(UUID id, int coins) {
        if (coins <= 0) return false;
        Record r = player(id);
        if (r.coins < coins) return false;
        r.coins -= coins;
        try {
            save();
            return true;
        } catch (RuntimeException ex) {
            r.coins += coins;
            ModOfMod.LOGGER.error("Failed to save store purchase", ex);
            return false;
        }
    }

    public void refund(UUID id, int coins) {
        Record r = player(id);
        r.coins = Math.addExact(r.coins, coins);
        save();
    }

    /** Spawn each new player at a safe, different, random Overworld coordinate. */
    public void firstJoin(ServerPlayer player, ServerLevel level) {
        Record r = player(player.getUUID());
        if (r.spawned) {
            ensureMap(player, level);
            return;
        }
        BlockPos point = findSafeRandomSpawn(level, player.getUUID());
        if (point != null) {
            player.teleportTo(level, point.getX() + 0.5, point.getY(), point.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot(), false);
            r.spawnX = point.getX();
            r.spawnZ = point.getZ();
        } else {
            ModOfMod.LOGGER.warn("Unable to find a safe random spawn, using world spawn for {}", player.getGameProfile().name());
        }
        r.spawned = true;
        ensureMap(player, level);
        save();
    }

    private BlockPos findSafeRandomSpawn(ServerLevel level, UUID id) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            int x = random.nextInt(-2200, 2201);
            int z = random.nextInt(-2200, 2201);
            boolean nearSomeone = root.players.entrySet().stream().anyMatch(entry -> {
                if (entry.getKey().equals(id.toString()) || !entry.getValue().spawned) return false;
                Record other = entry.getValue();
                long dx = (long) x - other.spawnX;
                long dz = (long) z - other.spawnZ;
                return dx * dx + dz * dz < 700L * 700L;
            });
            if (nearSomeone) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (y < 64 || y > level.getMaxY() - 3 || !level.getWorldBorder().isWithinBounds(at)) continue;
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
            if (!level.getFluidState(at.below()).isEmpty()) continue;
            if (!level.getBlockState(at.below()).blocksMotion()) continue;
            return at;
        }
        return null;
    }

    /** Issue a filled vanilla map; its pixels update as the player explores with the map. */
    public void ensureMap(ServerPlayer player, ServerLevel level) {
        Record r = player(player.getUUID());
        if (r.mapId >= 0 && player.getInventory().contains(stack -> {
            if (!stack.is(Items.FILLED_MAP)) return false;
            MapId id = stack.get(DataComponents.MAP_ID);
            return id != null && id.id() == r.mapId;
        })) return;

        ItemStack map;
        if (r.mapId < 0) {
            map = MapItem.create(level, player.getBlockX(), player.getBlockZ(), (byte) 3, true, true);
            MapId id = map.get(DataComponents.MAP_ID);
            if (id == null) return;
            r.mapId = id.id();
        } else {
            map = new ItemStack(Items.FILLED_MAP);
            map.set(DataComponents.MAP_ID, new MapId(r.mapId));
        }
        map.set(DataComponents.CUSTOM_NAME, Component.literal("모험가의 지도"));
        if (!player.getInventory().add(map)) {
            player.sendSystemMessage(Component.literal("[모험] 지도를 지급할 인벤토리 공간이 필요합니다."));
        }
        save();
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = Files.createTempFile(file.getParent(), "modofmod-", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                    GSON.toJson(root, writer);
                }
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write mod-of-mod save: " + file, error);
        }
    }
}
