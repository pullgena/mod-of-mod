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
    private static long popupStart;
    private static java.util.List<dev.pullgena.modofmod.ModPackets.RadarPoint> radar = java.util.List.of();
    public static void setRadar(java.util.List<dev.pullgena.modofmod.ModPackets.RadarPoint> points) { radar = java.util.List.copyOf(points); }

    private static class ClientPreferences {
        boolean showRadar = true;
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

    public static void toggleRadar() {
        preferences.showRadar = !preferences.showRadar;
        try {
            Files.createDirectories(SETTINGS.getParent());
            try (Writer writer = Files.newBufferedWriter(SETTINGS, StandardCharsets.UTF_8)) {
                GSON.toJson(preferences, writer);
            }
        } catch (Exception error) {
            ModOfModClient.LOGGER.error("Could not persist client map preference", error);
        }
    }

    public static boolean isRadarShown() { return preferences.showRadar; }

    public static void achievement(String title, String message, int coins) {
        achievementName = title.length() > 60 ? title.substring(0, 60) : title;
        achievementMessage = message.length() > 100 ? message.substring(0, 100) : message;
        reward = coins;
        popupStart = System.currentTimeMillis();
        popupUntil = popupStart + 5500;
    }

    public static void drawHud(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        long now = System.currentTimeMillis();
        if (popupUntil > now) {
            double elapsed = now - popupStart;
            double slide = Math.min(1.0, elapsed / 420.0);
            double exit = Math.min(1.0, (popupUntil - now) / 360.0);
            int width = Math.min(330, graphics.guiWidth() - 18);
            int x = (graphics.guiWidth() - width) / 2;
            int y = graphics.guiHeight() / 2 - 65 + (int)(24*(1.0 - Math.min(slide, exit)));
            graphics.fill(x+4,y+4,x+width+4,y+111,0x99000000);
            graphics.fill(x,y,x+width,y+108,0xF5152537);
            graphics.fill(x,y,x+width,y+5,0xFFF4CB64);
            graphics.fill(x+10,y+16,x+width-10,y+17,0xFFB89C60);
            graphics.centeredText(minecraft.font, "✦  발전과제 달성  ✦", graphics.guiWidth()/2, y+23, 0xFFFFDD78);
            graphics.centeredText(minecraft.font, achievementName, graphics.guiWidth()/2, y+43, 0xFFFFFFFF);
            graphics.centeredText(minecraft.font, achievementMessage, graphics.guiWidth()/2, y+61, 0xFFE7F4FA);
            int pulse=(int)(Math.sin(elapsed/130.0)*2);
            graphics.fill(x+width/2-57,y+73,x+width/2+57,y+101,0xFF514125);
            graphics.centeredText(minecraft.font,"[" + reward + "코인]+",graphics.guiWidth()/2,y+82+pulse,0xFFFFD15A);
        }
        if (preferences.showRadar && !(minecraft.gui.screen() instanceof ShopScreen)) {
            int cx=graphics.guiWidth()-66,cy=69;
            graphics.fill(cx-57,cy-57,cx+57,cy+57,0xD1132432);
            graphics.fill(cx-54,cy-54,cx+54,cy+54,0xD9223946);
            graphics.fill(cx-1,cy-49,cx+1,cy+49,0x7765D5B9);
            graphics.fill(cx-49,cy-1,cx+49,cy+1,0x7765D5B9);
            double spin=(now%3200)/3200.0*Math.PI*2;
            int sx=cx+(int)(Math.cos(spin)*43), sy=cy+(int)(Math.sin(spin)*43);
            graphics.fill(sx-2,sy-2,sx+2,sy+2,0xFF5BE6C2);
            graphics.fill(cx-3,cy-3,cx+4,cy+4,0xFFFFFFFF);
            for (var point : radar) {
                int px=cx+Math.max(-47,Math.min(47,point.dx()*47/256));
                int py=cy+Math.max(-47,Math.min(47,point.dz()*47/256));
                graphics.fill(px-3,py-3,px+4,py+4,0xFFFFC56E);
            }
            graphics.centeredText(minecraft.font,"RADAR · 256m",cx,cy-53,0xFFAEFFE0);
            graphics.centeredText(minecraft.font,radar.size()+" PLAYER",cx,cy+48,0xFFE6F7F2);
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
