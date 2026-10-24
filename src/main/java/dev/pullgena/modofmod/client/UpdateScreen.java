package dev.pullgena.modofmod.client;

import dev.pullgena.modofmod.ReleaseUpdater;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local-only updater, available from the title screen and OP panel. */
public final class UpdateScreen extends Screen {
    private final Screen previous;
    private String status = "공식 저장소의 최신 정식 릴리스를 검사할 수 있습니다.";
    private boolean running;
    public UpdateScreen(Screen previous) {
        super(Component.literal("Mod of Mod - 업데이트"));
        this.previous = previous;
    }
    @Override protected void init() {
        int cx = width / 2, cy = height / 2;
        addRenderableWidget(Button.builder(Component.literal("GitHub Releases 확인 및 업데이트"),
            b -> {
                if (running) return;
                running = true;
                status = "다운로드 및 SHA-256 검증 중...";
                ReleaseUpdater.updateAsync(ReleaseUpdater.REPO).thenAccept(result ->
                    minecraft.execute(() -> { status = result; running = false; }));
            }).bounds(cx-125,cy+22,250,24).build());
        addRenderableWidget(Button.builder(Component.literal("뒤로"),
            b -> minecraft.gui.setScreen(previous)).bounds(cx-55,cy+54,110,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractRenderState(g,mx,my,delta);
        int cx=width/2, cy=height/2;
        g.fill(cx-163,cy-90,cx+163,cy+8,0xEA1B2937);
        g.fill(cx-162,cy-89,cx+162,cy-85,0xFF6AB4F6);
        g.centeredText(font,"MOD OF MOD   ·   RELEASE CENTER",cx,cy-72,0xFF8BCBFF);
        g.centeredText(font,"공식 저장소: pullgena/mod-of-mod",cx,cy-50,0xFFFFFFFF);
        String shortMessage = status.length()>57 ? status.substring(0,57)+"..." : status;
        g.centeredText(font,shortMessage,cx,cy-23,0xFFE6E6E6);
        g.centeredText(font,"적용하려면 게임/서버를 다시 시작해야 합니다.",cx,cy-9,0xFFB5C5D5);
    }
    @Override public boolean isPauseScreen() { return false; }
}
