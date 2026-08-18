package com.local.sgmhelper;

import java.util.List;

/**
 * 30 级副本没有随机推进，武当整条路线都是固定路点。每个路点只找副本 BOSS，
 * 不先攻击普通敌人。两个 first 标记复刻武当的 flag570/flag420：
 * 进入该区间的第一拍先回到区间起点，之后才继续往下一个路点走。
 */
final class Dungeon30Action extends BaseDungeonAction {
    /** 武当 part3 的 `FindEnemyAction(names)`。 */
    private static final List<String> BANNERS = List.of("董军阵旗");

    private boolean firstBand570 = true;
    private boolean firstBand420 = true;

    @Override int level() { return 30; }
    @Override String dungeonName() { return "三公战华雄"; }
    @Override String entryNpcName() { return "黄巾太平道长|乱军太平道长"; }
    @Override String exitNpcName() { return "黄巾太平道长|乱军太平道长"; }
    @Override String campName() { return "黄巾营寨"; }
    @Override int campNpcX() { return 58; }
    @Override int[] entryNpcRows() { return new int[] {2, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {24, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        boolean atGate = (x >= 451 && x < 510)
                || (x >= 301 && x < 350)
                || (x >= 151 && x < 205);
        int gateAttempt = nextWudangGateAttempt(x, atGate);
        if (x >= 451) {
            return part0(x, gateAttempt);
        }
        if (x >= 301) {
            return part1(x, gateAttempt);
        }
        if (x >= 151) {
            return part2(x, gateAttempt);
        }
        if (x >= 33) {
            return part3(x);
        }
        return DungeonBattleAutomation.RouteDecision.exit(24, 27);
    }

    /** part0（599..451）：570 → 500 → part1 门点 (458,4)。武当这两个路点在 y=25。 */
    private DungeonBattleAutomation.RouteDecision part0(int x, int gateAttempt) {
        if (x >= 580) {
            return DungeonBattleAutomation.RouteDecision.searchBoss(570, 25, 570);
        }
        if (x >= 510) {
            if (firstBand570) {
                firstBand570 = false;
                return DungeonBattleAutomation.RouteDecision.searchBossRoute(
                        new int[][] {{570, 25}, {500, 25}});
            }
            return DungeonBattleAutomation.RouteDecision.searchBoss(500, 25, 570);
        }
        int[] gate = wudangGate(458, 4, gateAttempt);
        return DungeonBattleAutomation.RouteDecision.searchBoss(
                gate[0], gate[1], 570);
    }

    /** part1（448..301）：410 → 340 → part2 门点 (304,27)。 */
    private DungeonBattleAutomation.RouteDecision part1(int x, int gateAttempt) {
        if (x >= 420) {
            return DungeonBattleAutomation.RouteDecision.searchBoss(410, 25, 430);
        }
        if (x >= 350) {
            if (firstBand420) {
                firstBand420 = false;
                return DungeonBattleAutomation.RouteDecision.searchBossRoute(
                        new int[][] {{410, 25}, {340, 25}});
            }
            return DungeonBattleAutomation.RouteDecision.searchBoss(340, 25, 430);
        }
        int[] gate = wudangGate(304, 27, gateAttempt);
        return DungeonBattleAutomation.RouteDecision.searchBoss(
                gate[0], gate[1], 430);
    }

    /** part2（297..151）：只找副本 BOSS，235 → 195 → part3 门点 (174,4)。 */
    private DungeonBattleAutomation.RouteDecision part2(int x, int gateAttempt) {
        if (x >= 245) {
            return DungeonBattleAutomation.RouteDecision.searchBoss(235, 25, 0);
        }
        if (x >= 205) {
            return DungeonBattleAutomation.RouteDecision.searchBoss(195, 25, 0);
        }
        int[] gate = wudangGate(174, 4, gateAttempt);
        return DungeonBattleAutomation.RouteDecision.searchBoss(gate[0], gate[1], 0);
    }

    /**
     * part3（148..33）：85 → 45 → 出口点 (24,27)。这一段和其他分段不同——武当先用
     * `FindEnemyAction(["董军阵旗"])` 打阵旗，打完才走 `FindDungeonBossAction` 找红名。
     */
    private DungeonBattleAutomation.RouteDecision part3(int x) {
        if (x >= 150) {
            return banners(scanLeft(x, 15), 27);
        }
        if (x >= 95) {
            return banners(85, 25);
        }
        if (x >= 55) {
            return banners(45, 25);
        }
        return banners(24, 27);
    }

    private static DungeonBattleAutomation.RouteDecision banners(int x, int y) {
        return DungeonBattleAutomation.RouteDecision.searchNamedThenBoss(
                x, y, 0, BANNERS);
    }
}
