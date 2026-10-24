package dev.pullgena.modofmod;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;

/** Reward amounts follow in-game advancement difficulty; at least 3 coins. */
public final class AdvancementMessages {
    private AdvancementMessages() { }

    public static int coins(AdvancementHolder holder) {
        String id = holder.id().toString();
        if (id.equals("minecraft:end/kill_dragon")) return 100;
        if (id.equals("minecraft:end/dragon_egg")) return 75;
        if (id.equals("minecraft:nether/all_potions")) return 90;
        if (id.equals("minecraft:adventure/adventuring_time")) return 120;
        if (id.equals("minecraft:nether/all_effects")) return 150;
        if (id.equals("minecraft:husbandry/balanced_diet")) return 95;
        if (id.equals("minecraft:adventure/kill_all_mobs")) return 125;
        if (id.equals("minecraft:story/mine_stone")) return 5;
        if (holder.value().display().isPresent()) {
            AdvancementType type = holder.value().display().get().type();
            return switch (type) {
                case CHALLENGE -> 50;
                case GOAL -> 15;
                default -> 3;
            };
        }
        return 3;
    }

    public static String name(AdvancementHolder holder) {
        return holder.value().display().map(display -> display.title().getString())
            .orElseGet(() -> holder.id().getPath());
    }

    public static String message(AdvancementHolder holder) {
        String id = holder.id().toString();
        return switch (id) {
            case "minecraft:story/root" -> "드디어 모험이 시작됐습니다. 끝까지 함께해요!";
            case "minecraft:story/mine_stone" -> "첫 걸음! 엔딩까지 응원합니다!";
            case "minecraft:story/upgrade_tools" -> "더 단단한 장비로 한 걸음 더 나아가세요!";
            case "minecraft:story/smelt_iron" -> "철의 시대입니다. 모험할 준비 되셨나요?";
            case "minecraft:story/mine_diamond" -> "반짝이는 보물! 이제 진짜 시작이군요.";
            case "minecraft:story/enter_the_nether" -> "뜨거운 여행이 시작되었습니다. 조심하세요!";
            case "minecraft:story/enter_the_end" -> "마지막 관문입니다. 준비되셨나요?";
            case "minecraft:end/kill_dragon" -> "엔딩이 해제됐다고 해서 바로 나갈 건 아니죠?";
            case "minecraft:adventure/kill_a_mob" -> "몬스터들도 이제 당신을 조심해야겠네요!";
            case "minecraft:adventure/adventuring_time" -> "세상은 넓고, 가볼 곳은 정말 많네요!";
            case "minecraft:nether/all_effects" -> "이 정도면 물약 연구원이셔도 되겠어요!";
            default -> {
                int reward = coins(holder);
                if (reward >= 100) yield "엄청난 도전이었어요. 전설이 하나 추가됐네요!";
                if (reward >= 50) yield "쉽지 않은 도전이었죠? 훌륭하게 해내셨습니다!";
                if (reward >= 15) yield "좋아요! 더 멀리 나아갈 준비가 되었습니다.";
                yield "차근차근 전진 중! 다음 목표도 기대할게요.";
            }
        };
    }
}
