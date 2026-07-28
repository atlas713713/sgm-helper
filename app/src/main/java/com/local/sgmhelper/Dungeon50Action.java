package com.local.sgmhelper;

final class Dungeon50Action extends BaseDungeonAction {
    @Override int level() { return 50; }
    @Override String dungeonName() { return "孙坚与玉玺"; }
    @Override String entryNpcName() { return "历战老兵"; }
    @Override String exitNpcName() { return "孙坚"; }
    @Override String interactionNpcName() { return "年轻时的历战老兵"; }
    @Override String campName() { return "追忆之地"; }
    @Override int campNpcX() { return 89; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {570, 25}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        if (x <= 49) {
            // part0：出生点必须依次走三个点，不能直接点最后一个。
            return DungeonBattleAutomation.RouteDecision.moveRoute(
                    new int[][] {{46, 6}, {46, 45}, {60, 45}});
        }
        if (x >= 291 && x <= 308) {
            // partCenter：先点 NPC 快捷键，再和“年轻时的历战老兵”点“交给我吧”。
            return DungeonBattleAutomation.RouteDecision.interactNpc("交给我吧");
        }
        if (x >= 583) {
            // part7：切换离场。
            return DungeonBattleAutomation.RouteDecision.exit(570, 25);
        }
        // part1..part6 全部走武当的 attackPart：先找红名副本 BOSS，
        // 没有目标就固定右移 30 格（y=27），上限 570，越界后落在 575。
        return DungeonBattleAutomation.RouteDecision.search(stepRight(x), 27, 0, false);
    }

    /** 复刻武当 toRight：+30 一步，超过 570 就落在 575。 */
    private static int stepRight(int x) {
        int next = x + 30;
        return next > 570 ? 575 : next;
    }
}
