package dev.pullgena.modofmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Only client visuals and preferences; real money is held on the server. */
public final class ClientState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS = FabricLoader.getInstance().getConfigDir().resolve("modofmod-client.json");
    private static ClientPreferences preferences = loadPreferences();
    public static int coins = -1;
    private static String achievementName = "";
    private static String achievementMessage = "";
    private static int reward;
    private static long popupUntil;

    private static class ClientPreferences {
        boolean showMapHelp = true;
    }

    private ClientState() { }

    private static ClientPreferences loadPreferences() {
        if (!Files.exists(SETTINGS)) return new ClientPreferences();
        try (Reader reader = Files.newBufferedReader(SETTINGS, StandardCharsets.UTF_8)) {
            ClientPreferences loaded = GSON.fromJson(reader, ClientPreferences.class);
            return loaded != null ? loaded : new ClientPreferences();
        } catch (Exception error) {
            return new ClientPreferences();
        }
    }

    public static void toggleMap() {
        preferences.showMapHelp = !preferences.showMapHelp;
        try {
            Files.createDirectories(SETTINGS.getParent());
            try (Writer writer = Files.newBufferedWriter(SETTINGS, StandardCharsets.UTF_8)) {
                GSON.toJson(preferences, writer);
            }
        } catch (Exception error) {
            ModOfModClient.LOGGER.error("Could not persist client map preference", error);
        }
    }

    public static boolean isMapShown() { return preferences.showMapHelp; }

    public static void achievement(String title, String message, int coins) {
        achievementName = title.length() > 60 ? title.substring(0, 60) : title;
        achievementMessage = message.length() > 100 ? message.substring(0, 100) : message;
        reward = coins;
        popupUntil = System.currentTimeMillis() + 5000;
    }

    public static void drawHud(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (popupUntil > System.currentTimeMillis()) {
            int width = Math.min(290, graphics.guiWidth() - 22);
            int x = (graphics.guiWidth() - width) / 2;
            int y = graphics.guiHeight() / 2 - 65;
            drawPaper(graphics, x, y, width, 94);
            graphics.centeredText(minecraft.font, "✦ 발전과제 달성 ✦", graphics.guiWidth() / 2, y + 12, 0xFF985B2C);
            graphics.centeredText(minecraft.font, achievementName, graphics.guiWidth() / 2, y + 31, 0xFF2D261E);
            graphics.centeredText(minecraft.font, achievementMessage, graphics.guiWidth() / 2, y + 49, 0xFF634A37);
            graphics.centeredText(minecraft.font, "[" + reward + "코인]+", graphics.guiWidth() / 2, y + 69, 0xFFC08121);
        }
        // Live vanilla maps are kept in inventory. This optional helper shows
        // position; terrain rendering in a full minimap is a future milestone.
        if (preferences.showMapHelp && !(minecraft.gui.screen() instanceof ShopScreen)) {
            graphics.fill(graphics.guiWidth() - 133, 4, graphics.guiWidth() - 4, 25, 0xB02B332B);
            graphics.text(minecraft.font,
                "지도  X:" + minecraft.player.getBlockX() + " Z:" + minecraft.player.getBlockZ(),
                graphics.guiWidth() - 128, 10, 0xFFEDE2BF, false);
        }
    }

    /** A jagged paper edge without per-frame texture uploads or external assets. */
    public static void drawPaper(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x + 5, y + 5, x + w + 3, y + h + 3, 0x77000000);
        graphics.fill(x + 4, y + 3, x + w - 3, y + h - 3, 0xFFE8D3AA);
        graphics.fill(x + 9, y + 7, x + w - 8, y + h - 7, 0xFFF3E6C9);
        for (int i = 0; i < w - 12; i += 9) {
            int jagged = (i * 17 / 13) % 4;
            graphics.fill(x + 6 + i, y + jagged, Math.min(x + w - 5, x + 14 + i), y + 7, 0xFFE8D3AA);
            graphics.fill(x + 6 + i, y + h - 7, Math.min(x + w - 5, x + 14 + i), y + h - jagged, 0xFFE8D3AA);
        }
        for (int i = 1; i < h - 8; i += 8) {
            int jagged = (i * 19 / 11) % 4;
            graphics.fill(x + jagged, y + 5 + i, x + 8, Math.min(y + h - 5, y + i + 12), 0xFFE8D3AA);
            graphics.fill(x + w - 7, y + 5 + i, x + w - jagged, Math.min(y + h - 5, y + i + 12), 0xFFE8D3AA);
        }
        graphics.fill(x + 11, y + 11, x + w - 11, y + 12, 0x80B99B6D);
    }
}
