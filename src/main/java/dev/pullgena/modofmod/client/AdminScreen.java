package dev.pullgena.modofmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AdminScreen extends Screen {
    private long entered;
    public AdminScreen() { super(Component.literal("모험 관리자")); }
    @Override protected void init() {
        entered = System.currentTimeMillis();
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal("공식 업데이트 확인"),
            b -> minecraft.gui.setScreen(new UpdateScreen(this)))
            .bounds(cx - 90, height / 2 + 28, 180, 23).build());
        addRenderableWidget(Button.builder(Component.literal("돌아가기"),
            b -> minecraft.gui.setScreen(null))
            .bounds(cx - 55, height / 2 + 60, 110, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int cx=width/2, cy=height/2;
        g.fill(cx-153,cy-96,cx+153,cy+94,0xE9122432);
        g.fill(cx-151,cy-94,cx+151,cy-91,0xFF35CAAB);
        g.centeredText(font,"◆ 관리자 제어실 ◆",cx,cy-77,0xFF8CF3DA);
        g.centeredText(font,"OP 권한이 확인된 플레이어만 사용할 수 있습니다.",cx,cy-51,0xFFE5EEF6);
        g.centeredText(font,"레이더: 256블록  |  서버 관리 명령: /업데이트 <링크>",cx,cy-28,0xFFC4DDEB);
        g.centeredText(font,"버전 확인 및 안전한 교체는 하단 업데이트 메뉴",cx,cy-11,0xFF9BBFCB);
        super.extractRenderState(g,mx,my,dt);
    }
    @Override public boolean isPauseScreen() { return false; }
}
