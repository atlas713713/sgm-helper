package com.local.sgmhelper;

final class Dungeon50Action extends BaseDungeonAction {
    @Override int level() { return 50; }
    @Override String dungeonName() { return "孙坚与玉玺"; }
    @Override String entryNpcName() { return "历战老兵"; }
    @Override String exitNpcName() { return "孙坚"; }
    /**
     * 中央段是武当的特殊 partCenter：只识别并点击“交给我吧”，不先校验 NPC 标题。
     * 原版也是直接在 NPCDialog 选项区找按钮；标题 OCR 在副本中央并不稳定。
     */
    @Override String interactionNpcName() { return null; }
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
            // part0：武当按当前 (x,y) 三选一走一个点，不是一次走完三个——
            // 在 (46,45) 复活时直接接着走 (60,45)，不会被送回 (46,6)。
            if (x < 42) {
                return DungeonBattleAutomation.RouteDecision.move(46, 6, 0);
            }
            if (y < 40) {
                return DungeonBattleAutomation.RouteDecision.move(46, 45, 0);
            }
            return DungeonBattleAutomation.RouteDecision.move(60, 45, 0);
        }
        if (x >= 50 && x <= 110) {
            return rightwardRoute(x, 110);
        }
        if (x >= 112 && x <= 194) {
            return rightwardRoute(x, 194);
        }
        if (x >= 205 && x <= 287) {
            return rightwardRoute(x, 287);
        }
        if (x >= 291 && x <= 308) {
            // partCenter：先点 NPC 快捷键，再和“年轻时的历战老兵”点“交给我吧”。
            return DungeonBattleAutomation.RouteDecision.interactNpc("交给我吧");
        }
        if (x >= 312 && x <= 388) {
            return rightwardRoute(x, 388);
        }
        if (x >= 399 && x <= 484) {
            return rightwardRoute(x, 484);
        }
        if (x >= 495 && x <= 581) {
            return rightwardRoute(x, 581);
        }
        if (x >= 583) {
            // part7：切换离场。
            return DungeonBattleAutomation.RouteDecision.exit(570, 25);
        }
        // 武当在分段之间的几个空档坐标不执行新的动作，下一轮重新读坐标。
        // 当前执行器没有“等待”类型，沿用右移探测，避免 OCR 抖动时停死。
        return rightwardRoute(x, 581);
    }

    private static DungeonBattleAutomation.RouteDecision rightwardRoute(int x, int endX) {
        int nextX = stepRight(x);
        // 武当 toRight 的卡点处理：分段终点只用于生成脱困点，正常目标仍是
        // LocationUtil 按 30 格计算出的 nextX。越过分段终点才退回 endX-10，
        // 还没走到终点则再往前顶 20 格。
        int unstuckX = nextX > endX ? endX - 10 : nextX + 20;
        return DungeonBattleAutomation.RouteDecision
                .search(nextX, 27, 0, false)
                .unstuckAt(unstuckX, 27);
    }

    /** 复刻武当 toRight：+30 一步，超过 570 就落在 575。 */
    private static int stepRight(int x) {
        int next = x + 30;
        return next > 570 ? 575 : next;
    }
}
