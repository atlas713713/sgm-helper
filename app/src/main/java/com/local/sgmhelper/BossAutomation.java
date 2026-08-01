package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BossAutomation {
    private static final Pattern MAP_COORDINATE =
            Pattern.compile("(?<!\\d)(\\d{1,3})\\s*[,，]\\s*(\\d{1,2})(?!\\d)");
    private static final Pattern CHANNEL =
            Pattern.compile("(?:第\\s*([1-8])\\s*分流|分流\\s*([1-8]))");
    private static final int MAP_LEFT = 50;
    private static final int MAP_RIGHT = 550;
    private static final int MAP_Y = 25;
    private static final long MAP_LOAD_MS = 5_000;
    private static final long CHANNEL_DIALOG_MS = 1_000;
    private static final long CHANNEL_CHECK_MS = 2_000;
    private static final long CHANNEL_READY_MS = 3_000;
    private static final long MOVE_DURATION_MS = 4_000;
    private static final long MOVE_SCAN_INTERVAL_MS = 250;
    private static final long BOSS_CHECK_MS = 5_000;
    private static final int BOSS_DEFEAT_CONFIRMATIONS = 3;
    private static final long LEADER_CHECK_MS = 10_000;
    private static final int PARTY_OCR_ATTEMPTS = 5;
    private static final int CHANNEL_OCR_ATTEMPTS = 5;
    private static final int PARTY_MANAGE_X = 190;
    private static final int PARTY_MANAGE_Y = 279;
    private static final int PARTY_DIALOG_CLOSE_X = 1260;
    private static final int PARTY_DIALOG_CLOSE_Y = 150;
    private static final int AUTO_SETTINGS_ATTEMPTS = 2;
    private static final long MAP_RECOGNITION_RESTART_DELAY_MS = 60_000;
    private static final int WORLD_MAP_NAME_ATTEMPTS = 5;
    private static final int WORLD_MAP_X = 800;
    private static final int WORLD_MAP_Y = 585;
    private static final int WORLD_LIST_SWITCH_X = 320;
    private static final int WORLD_LIST_SWITCH_Y = 34;
    private static final long WORLD_MAP_OPEN_MS = 1_000;
    private static final String PREF_BOSS_MODE = "boss_mode";
    private static final String MODE_WORLD = "world";

    private final AutomationHost host;
    private final ChannelSwitcher channelSwitcher;
    private final AntiCheatVerification antiCheatVerification;
    private TeamFollowerVision teamFollowerVision;
    private List<Integer> route = new ArrayList<>();
    private int routeIndex;
    private boolean followLeader;
    private long nextLeaderCheckAt;
    private boolean worldMode;
    private WorldBossCatalog.MapEntry worldMap;
    private WorldBossCatalog.BossEntry worldBossTarget;
    private List<WorldBossCatalog.BossEntry> activeWorldBosses = new ArrayList<>();
    private int searchLeft = MAP_LEFT;
    private int searchRight = MAP_RIGHT;
    private int searchY = MAP_Y;

    BossAutomation(AutomationHost host) {
        this.host = host;
        channelSwitcher = new ChannelSwitcher(host);
        antiCheatVerification = new AntiCheatVerification(host);
    }

    void start() {
        boolean savedWorldMode = MODE_WORLD.equals(host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_BOSS_MODE, "wilderness"));
        start(savedWorldMode);
    }

    void startWilderness() {
        saveMode(false);
        start(false);
    }

    void startWorld() {
        saveMode(true);
        start(true);
    }

    private void saveMode(boolean useWorldMode) {
        host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(PREF_BOSS_MODE, useWorldMode ? MODE_WORLD : "wilderness")
                .apply();
    }

    private void start(boolean useWorldMode) {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        worldMode = useWorldMode;
        worldMap = null;
        worldBossTarget = null;
        activeWorldBosses = new ArrayList<>();
        searchLeft = MAP_LEFT;
        searchRight = MAP_RIGHT;
        searchY = MAP_Y;
        if (worldMode && !loadWorldBossTarget()) {
            progress("请先在 BOSS 设置中选择世界王目标");
            return;
        }
        followLeader = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(
                        HelperAccessibilityService.PREF_BOSS_FOLLOW_LEADER, false);
        nextLeaderCheckAt = 0;
        host.startPrimaryAutomation(
                AutomationHost.PrimaryTask.BOSS, label() + "：打开游戏", this::prepareBoss);
    }

    private boolean loadWorldBossTarget() {
        android.content.SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        worldMap = WorldBossCatalog.findMapByName(preferences.getString(
                HelperAccessibilityService.PREF_WORLD_BOSS_MAP, ""));
        if (worldMap == null) {
            return false;
        }
        worldBossTarget = worldMap.findBoss(preferences.getString(
                HelperAccessibilityService.PREF_WORLD_BOSS_TARGET, ""));
        if (worldBossTarget == null) {
            return false;
        }
        activeWorldBosses = Collections.singletonList(worldBossTarget);
        searchLeft = worldBossTarget.searchLeft;
        searchRight = worldBossTarget.searchRight;
        searchY = 30;
        return true;
    }

    private String label() {
        return worldMode ? "世界王" : "野王";
    }

    private void progress(String value) {
        host.showProgress(label() + "：" + value);
    }

    private void fail(String value) {
        host.failAutomation(label() + "：" + value);
    }

    private void prepareBoss() {
        host.checkInventoryBeforePrimary(this::prepareBossAfterInventoryCheck);
    }

    private void prepareBossAfterInventoryCheck() {
        host.ensureGameHudVisible(this::prepareModeOne);
    }

    private void prepareModeOne() {
        openAutoSettings(AUTO_SETTINGS_ATTEMPTS);
    }

    private void openAutoSettings(int remainingAttempts) {
        progress("打开菜单");
        host.tap(1215, 58, () -> {
            progress("向上滑动菜单");
            host.swipe(1150, 600, 1150, 220,
                    () -> clickAutoSettings(remainingAttempts));
        });
    }

    private void clickAutoSettings(int remainingAttempts) {
        progress("识别自动设置");
        clickAutoSettingsTemplate(remainingAttempts, 2);
    }

    private void clickAutoSettingsTemplate(int remainingAttempts, int templateAttempts) {
        host.matchTemplates(new WudangTemplateMatcher.Template[] {
                        WudangTemplateMatcher.Template.AUTO_FUNCTION
                },
                1100, 330, 1270, 405,
                matches -> {
                    WudangTemplateMatcher.Match match = matches.get(
                            WudangTemplateMatcher.Template.AUTO_FUNCTION);
                    if (match != null && match.found()) {
                        host.tap(match.centerX(), match.centerY(), this::selectModeOne);
                    } else if (templateAttempts > 1) {
                        host.postDelayed(
                                () -> clickAutoSettingsTemplate(
                                        remainingAttempts, templateAttempts - 1),
                                500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=自动功能 source=ocr");
                        clickAutoSettingsByOcr(remainingAttempts);
                    }
                }, error -> {
                    if (templateAttempts > 1) {
                        host.postDelayed(
                                () -> clickAutoSettingsTemplate(
                                        remainingAttempts, templateAttempts - 1),
                                500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=自动功能 source=ocr error="
                                        + error.getClass().getSimpleName());
                        clickAutoSettingsByOcr(remainingAttempts);
                    }
                });
    }

    private void clickAutoSettingsByOcr(int remainingAttempts) {
        host.recognizeTextRegion(1100, 330, 1270, 405, text -> {
            DiagnosticLog.info("BOSS", "auto settings ROI Paddle OCR='"
                    + text.getText().replace('\n', ' ') + "'");
            Rect bounds = findMenuAutoBounds(text);
            if (bounds != null) {
                host.tap(bounds.centerX(), bounds.centerY(), this::selectModeOne);
            } else if (remainingAttempts > 1) {
                progress("未识别到自动，检查遮挡窗口");
                host.tap(1240, 45, () -> host.ensureGameHudVisible(
                        () -> openAutoSettings(remainingAttempts - 1)));
            } else {
                DiagnosticLog.warn("BOSS", "auto settings OCR missed twice; skipping");
                progress("未识别到自动，跳过自动设置");
                host.tap(1240, 45, this::useMarker);
            }
        });
    }

    private void selectModeOne() {
        progress("选择模式 1");
        host.tap(1025, 75, () -> host.tap(1240, 45, this::useMarker));
    }

    private static Rect findMenuAutoBounds(OcrText text) {
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (isMenuAutoCandidate(line.getText(), bounds)) {
                    return bounds;
                }
            }
        }
        return null;
    }

    static boolean isMenuAutoCandidate(String value, Rect bounds) {
        return bounds != null
                && isMenuAutoCandidate(value, bounds.centerX(), bounds.centerY());
    }

    static boolean isMenuAutoCandidate(String value, int centerX, int centerY) {
        return "自动".equals(value == null ? "" : value.replaceAll("\\s+", ""))
                && centerX >= 850 && centerX <= 1270
                && centerY >= 100 && centerY <= 650;
    }

    private void useMarker() {
        progress("停止自动攻击后前往标记点");
        host.useFirstMarker(() -> host.postDelayed(
                () -> host.waitForMapReady(20, this::afterMarkerReady), MAP_LOAD_MS));
    }

    private void afterMarkerReady() {
        if (worldMode) {
            readWorldMapName(WORLD_MAP_NAME_ATTEMPTS);
        } else {
            openEnemyPanel();
        }
    }

    private void readWorldMapName(int remainingAttempts) {
        progress("识别标记点地图");
        host.recognizeMapName(value -> {
            WorldBossCatalog.MapEntry map = WorldBossCatalog.findMap(value);
            if (map != null && map.name.equals(worldMap.name)) {
                DiagnosticLog.info("BOSS", "world target map=" + worldMap.name
                        + " boss=" + worldBossTarget.displayName());
                maybeFollowLeader(this::openWorldBossMap);
            } else if (map != null) {
                fail("标记点地图是 " + map.name + "，目标世界王在 " + worldMap.name);
            } else if (remainingAttempts > 1) {
                progress("未识别到当前地图，重新检测");
                host.postDelayed(
                        () -> readWorldMapName(remainingAttempts - 1), 1_000);
            } else {
                DiagnosticLog.warn("BOSS", "world map OCR miss: " + value);
                host.failPrimaryAndRestartAfter(
                        label() + "：未识别到标记点所在地图（地图 OCR："
                                + displayOcrValue(value) + "）",
                        MAP_RECOGNITION_RESTART_DELAY_MS);
            }
        });
    }

    static String displayOcrValue(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "空" : normalized;
    }

    private void openWorldBossMap() {
        progress("打开大地图检查世界王");
        host.tap(WORLD_MAP_X, WORLD_MAP_Y,
                () -> host.postDelayed(() -> scanWorldBossMap(false), WORLD_MAP_OPEN_MS));
    }

    private void scanWorldBossMap(boolean listToggled) {
        host.recognizeWorldBossMap(value -> {
            boolean visible = worldBossTarget.isVisible(value);
            DiagnosticLog.info("BOSS", "world list map=" + worldMap.name
                    + " target=" + worldBossTarget.displayName()
                    + " toggled=" + listToggled + " visible=" + visible);
            if (visible) {
                progress("大地图发现 " + worldBossTarget.displayName());
                closeWorldBossMap(this::openEnemyPanel);
            } else if (!listToggled) {
                progress("展开大地图世界王列表");
                host.tap(WORLD_LIST_SWITCH_X, WORLD_LIST_SWITCH_Y,
                        () -> host.postDelayed(
                                () -> scanWorldBossMap(true), WORLD_MAP_OPEN_MS));
            } else {
                progress("当前分流未发现 " + worldBossTarget.displayName());
                closeWorldBossMap(followLeader
                        ? this::followLeaderAtRouteEnd
                        : this::switchChannel);
            }
        });
    }

    private void closeWorldBossMap(Runnable next) {
        progress("关闭大地图");
        host.pressBack(() -> host.ensureGameHudVisible(next));
    }

    private static String bossNames(List<WorldBossCatalog.BossEntry> bosses) {
        List<String> names = new ArrayList<>();
        for (WorldBossCatalog.BossEntry boss : bosses) {
            names.add(boss.name);
        }
        return String.join("/", names);
    }

    private void openEnemyPanel() {
        progress("打开自动寻敌");
        host.openAutoPathPanel(
                () -> host.tap(1015, 165, this::readCurrentPosition));
    }

    private void readCurrentPosition() {
        progress("读取当前位置");
        host.recognizeMapCoordinate(value -> {
            Integer currentX = parseMapX(value);
            if (currentX == null) {
                progress("未读取到坐标，重新检测");
                host.postDelayed(this::readCurrentPosition, 1_000);
                return;
            }
            startRoute(currentX);
        });
    }

    private void startRoute(int currentX) {
        route = buildRoute(currentX, searchLeft, searchRight);
        routeIndex = 0;
        moveNext();
    }

    private void moveNext() {
        if (host.handlePendingGear(this::resumeAfterGearHandle)) {
            return;
        }
        maybeFollowLeader(this::moveNextAfterLeaderCheck);
    }

    private void resumeAfterGearHandle() {
        prepareBoss();
    }

    private void moveNextAfterLeaderCheck() {
        if (!host.isAutomationRunning()) {
            return;
        }
        if (routeIndex >= route.size()) {
            if (usesLeaderChannelAtRouteEnd(followLeader)) {
                followLeaderAtRouteEnd();
            } else {
                switchChannel();
            }
            return;
        }
        int x = route.get(routeIndex);
        progress("搜索坐标 " + x + "," + searchY);
        long deadline = System.currentTimeMillis() + MOVE_DURATION_MS;
        host.tapMapCoordinateFast(x, searchY, () -> scanDuringMove(deadline));
    }

    static boolean usesLeaderChannelAtRouteEnd(boolean followLeader) {
        return followLeader;
    }

    private void followLeaderAtRouteEnd() {
        progress("跟随队长：当前分流搜索完成，读取队长分流");
        host.captureScreenshot(bitmap -> {
            if (bitmap == null) {
                fail("跟随队长：无法截图读取当前分流");
                return;
            }
            followLeaderChannel(bitmap, this::resumeAfterLeaderChannel);
        });
    }

    private void scanDuringMove(long deadline) {
        findRedBoss(target -> {
            if (target != null) {
                attack(target);
                return;
            }
            long delay = moveScanDelayMillis(System.currentTimeMillis(), deadline);
            if (delay == 0) {
                routeIndex++;
                moveNext();
            } else {
                host.postDelayed(() -> scanDuringMove(deadline), delay);
            }
        });
    }

    private void attack(BossTarget target) {
        if (worldBossTarget == null) {
            attackNow(target);
            return;
        }
        host.recognizeMapCoordinate(value -> {
            checkFakeBossZone(parseMapX(value));
            attackNow(target);
        });
    }

    /**
     * 武当 CheckFakeBossAction3 的第一道闸。只能在站位落到刷新区间中点 ±10 之外时确认
     * 真王；落在窗口内是"排除不掉"，不是"就是假王"——那要第二道 findFakeBoss 图像判据
     * 和第三道气/血变化才能定性。所以这里只记录，不改打谁。
     */
    private void checkFakeBossZone(Integer currentX) {
        String zone = worldBossTarget.searchLeft + "-" + worldBossTarget.searchRight;
        if (currentX == null) {
            DiagnosticLog.warn("BOSS", "假王检测：读不到坐标，跳过本次判定");
        } else if (worldBossTarget.isInFakeBossZone(currentX)) {
            DiagnosticLog.warn("BOSS", "假王检测：x=" + currentX
                    + " 落在假王位（区间 " + zone + " 的中点±10），排除不掉假王");
        } else {
            DiagnosticLog.info("BOSS", "假王检测：x=" + currentX
                    + " 不在假王位（区间 " + zone + " 的中点±10），确认真王");
        }
    }

    private void attackNow(BossTarget target) {
        progress("攻击 " + target.name);
        host.tap(target.bounds.centerX(), target.bounds.centerY(),
                () -> host.postDelayed(
                        () -> waitForBossDefeated(target.name, 0), BOSS_CHECK_MS));
    }

    private void waitForBossDefeated(String currentBoss, int consecutiveMisses) {
        maybeFollowLeader(() -> waitForBossDefeatedAfterLeaderCheck(
                currentBoss, consecutiveMisses));
    }

    private void waitForBossDefeatedAfterLeaderCheck(
            String currentBoss, int consecutiveMisses) {
        findRedBoss(target -> {
            if (target == null) {
                int misses = consecutiveMisses + 1;
                if (isBossDefeatConfirmed(misses)) {
                    progress("已击败 " + currentBoss);
                    moveNext();
                } else {
                    progress("确认击败 " + currentBoss
                            + "（" + misses + "/" + BOSS_DEFEAT_CONFIRMATIONS + "）");
                    host.postDelayed(
                            () -> waitForBossDefeated(currentBoss, misses), BOSS_CHECK_MS);
                }
            } else if (!target.name.equals(currentBoss)) {
                attack(target);
            } else {
                progress("战斗中 " + currentBoss);
                host.postDelayed(
                        () -> waitForBossDefeated(currentBoss, 0), BOSS_CHECK_MS);
            }
        });
    }

    static boolean isBossDefeatConfirmed(int consecutiveMisses) {
        return consecutiveMisses >= BOSS_DEFEAT_CONFIRMATIONS;
    }

    private void findRedBoss(java.util.function.Consumer<BossTarget> result) {
        host.recognizeRedBoss(target -> {
            if (!worldMode || target == null || isActiveWorldBoss(target.name)) {
                result.accept(target);
            } else {
                DiagnosticLog.info("BOSS", "world mode ignored red target=" + target.name
                        + " expected=" + bossNames(activeWorldBosses));
                result.accept(null);
            }
        });
    }

    private boolean isActiveWorldBoss(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return false;
        }
        for (WorldBossCatalog.BossEntry boss : activeWorldBosses) {
            if (normalized.contains(boss.name) || boss.name.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void switchChannel() {
        progress("打开分流信息");
        host.tap(1215, 705,
                () -> host.postDelayed(
                        () -> readCurrentChannel(CHANNEL_OCR_ATTEMPTS, false),
                        CHANNEL_DIALOG_MS));
    }

    private void readCurrentChannel(int remainingAttempts, boolean blockerChecked) {
        progress("判断当前分流");
        host.recognizeChannelDialog(current -> {
            if (current == null) {
                if (remainingAttempts <= 1) {
                    fail("未识别到当前分流");
                    return;
                }
                if (!blockerChecked) {
                    progress("分流读取失败，检查遮挡窗口");
                    host.ensureGameHudVisible(() -> host.tap(1215, 705,
                            () -> host.postDelayed(
                                    () -> readCurrentChannel(
                                            remainingAttempts - 1, true),
                                    CHANNEL_DIALOG_MS)));
                } else {
                    progress("未读取到当前分流，重新检测");
                    host.postDelayed(() -> readCurrentChannel(
                            remainingAttempts - 1, true), 1_000);
                }
                return;
            }
            int next = ChannelSwitcher.nextChannel(current);
            progress("第 " + current + " 分流 → 第 " + next + " 分流");
            channelSwitcher.switchOpenTo(next,
                    () -> host.postDelayed(
                            () -> checkAfterChannelSwitch(worldMode
                                    ? () -> readWorldMapName(WORLD_MAP_NAME_ATTEMPTS)
                                    : this::openEnemyPanel),
                            CHANNEL_CHECK_MS));
        });
    }

    private void checkAfterChannelSwitch(Runnable next) {
        antiCheatVerification.checkThen(() -> host.postDelayed(
                () -> antiCheatVerification.checkThen(
                        () -> host.waitForMapReady(20, next)),
                CHANNEL_READY_MS));
    }

    private void maybeFollowLeader(Runnable next) {
        if (!followLeader || System.currentTimeMillis() < nextLeaderCheckAt) {
            next.run();
            return;
        }
        nextLeaderCheckAt = System.currentTimeMillis() + LEADER_CHECK_MS;
        host.captureScreenshot(bitmap -> {
            if (bitmap == null) {
                DiagnosticLog.warn("BOSS", "follow leader screenshot unavailable");
                next.run();
                return;
            }
            TeamFollowerVision.Inspection inspection;
            try {
                inspection = teamFollowerVision().inspect(bitmap);
            } catch (Exception error) {
                DiagnosticLog.error("BOSS", "follow leader vision failed", error);
                bitmap.recycle();
                next.run();
                return;
            }
            DiagnosticLog.info("BOSS", String.format(java.util.Locale.US,
                    "captain self=%s slot=%d template=%.3f brightRatio=%.3f",
                    inspection.selfCaptain, inspection.captainSlot,
                    inspection.templateScore, inspection.brightRatio));
            if (inspection.selfCaptain) {
                bitmap.recycle();
                next.run();
            } else if (!inspection.hasTeammateCaptain()) {
                bitmap.recycle();
                DiagnosticLog.warn("BOSS", "captain flag not found; skip leader check");
                next.run();
            } else if (inspection.captainIsOnSameMap()) {
                bitmap.recycle();
                next.run();
            } else {
                progress("跟随队长：队长头像变灰，检查分流");
                followLeaderChannel(bitmap, this::resumeAfterLeaderChannel);
            }
        });
    }

    private void resumeAfterLeaderChannel() {
        if (worldMode) {
            readWorldMapName(WORLD_MAP_NAME_ATTEMPTS);
        } else {
            openEnemyPanel();
        }
    }

    private TeamFollowerVision teamFollowerVision() {
        if (teamFollowerVision == null) {
            teamFollowerVision = new TeamFollowerVision(host.context().getAssets());
        }
        return teamFollowerVision;
    }

    private void followLeaderChannel(Bitmap screenshot, Runnable next) {
        readCurrentHudChannel(screenshot, PARTY_OCR_ATTEMPTS, current -> {
            progress("跟随队长：当前第 " + current + " 分流");
            host.tap(PARTY_MANAGE_X, PARTY_MANAGE_Y,
                    () -> host.postDelayed(
                            () -> readLeaderChannel(PARTY_OCR_ATTEMPTS, current, next),
                            CHANNEL_DIALOG_MS));
        });
    }

    private void readCurrentHudChannel(Bitmap screenshot, int remainingAttempts,
            java.util.function.Consumer<Integer> next) {
        progress("跟随队长：读取当前分流");
        host.recognizeHudChannel(screenshot, channel -> {
            if (channel != null) {
                DiagnosticLog.info("BOSS", "leader follow current channel=" + channel);
                next.accept(channel);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> captureCurrentHudChannel(
                        remainingAttempts - 1, next), 1_000);
            } else {
                DiagnosticLog.warn("BOSS", "OCR miss: 右下角当前分流数字");
                fail("跟随队长：未识别到右下角当前分流");
            }
        });
    }

    private void captureCurrentHudChannel(int remainingAttempts,
            java.util.function.Consumer<Integer> next) {
        host.captureScreenshot(bitmap -> {
            if (bitmap != null) {
                readCurrentHudChannel(bitmap, remainingAttempts, next);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> captureCurrentHudChannel(
                        remainingAttempts - 1, next), 1_000);
            } else {
                fail("跟随队长：无法截图读取当前分流");
            }
        });
    }

    private void readLeaderChannel(int remainingAttempts, int current, Runnable next) {
        readLeaderChannel(remainingAttempts, current, next, false);
    }

    private void readLeaderChannel(int remainingAttempts, int current,
            Runnable next, boolean blockerChecked) {
        progress("跟随队长：读取队长分流");
        host.recognizeLeaderChannel(leader -> {
            if (leader != null) {
                DiagnosticLog.info("BOSS", "leader follow channels current=" + current
                        + " leader=" + leader + " same=" + (current == leader));
                closePartyDialog(() -> handleLeaderChannel(current, leader, next));
            } else {
                host.recognizeText(text -> retryLeaderChannelAfterMiss(
                        remainingAttempts, current, next, blockerChecked, text));
            }
        });
    }

    private void retryLeaderChannelAfterMiss(int remainingAttempts, int current,
            Runnable next, boolean blockerChecked, OcrText text) {
        boolean dialogOpen = hasPartyManageDialog(text);
        Integer fullScreenLeader = dialogOpen ? findLeaderChannel(text) : null;
        if (fullScreenLeader != null) {
            DiagnosticLog.info("BOSS", "leader channel crop missed; full screen="
                    + fullScreenLeader);
            closePartyDialog(() -> handleLeaderChannel(
                    current, fullScreenLeader, next));
            return;
        }
        if (remainingAttempts <= 1) {
            if (dialogOpen) {
                closePartyDialog(() -> failPartyOcr("队长分流", text));
            } else {
                failPartyOcr("队长分流", text);
            }
            return;
        }
        if (!blockerChecked) {
            progress("跟随队长：分流读取失败，检查遮挡窗口");
            host.ensureGameHudVisible(() -> {
                progress("跟随队长：重新打开队伍管理");
                host.tap(PARTY_MANAGE_X, PARTY_MANAGE_Y,
                        () -> host.postDelayed(
                                () -> readLeaderChannel(remainingAttempts - 1,
                                        current, next, true),
                                CHANNEL_DIALOG_MS));
            });
        } else if (dialogOpen) {
            host.postDelayed(() -> readLeaderChannel(
                    remainingAttempts - 1, current, next, true), 1_000);
        } else {
            DiagnosticLog.warn("BOSS", "party dialog is not open; tapping Manage again");
            progress("跟随队长：重新打开队伍管理");
            host.tap(PARTY_MANAGE_X, PARTY_MANAGE_Y,
                    () -> host.postDelayed(
                            () -> readLeaderChannel(remainingAttempts - 1,
                                    current, next, true),
                            CHANNEL_DIALOG_MS));
        }
    }

    private static boolean hasPartyManageDialog(OcrText text) {
        return isPartyManageDialogText(text.getText());
    }

    static boolean isPartyManageDialogText(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", "");
        return normalized.contains("点击此处增加队伍成员")
                || normalized.contains("所在地")
                || (normalized.contains("名称") && normalized.contains("职业"));
    }

    private void closePartyDialog(Runnable next) {
        progress("跟随队长：关闭队伍管理");
        DiagnosticLog.info("BOSS", "closing party dialog at "
                + PARTY_DIALOG_CLOSE_X + "," + PARTY_DIALOG_CLOSE_Y);
        host.tap(PARTY_DIALOG_CLOSE_X, PARTY_DIALOG_CLOSE_Y, () -> {
            DiagnosticLog.info("BOSS", "party dialog close tap completed");
            next.run();
        });
    }

    private void handleLeaderChannel(int current, int leader, Runnable next) {
        progress("跟随队长：队长在第 " + leader + " 分流");
        if (current == leader) {
            DiagnosticLog.info("BOSS", "leader follow decision=stay channel=" + current);
            nextLeaderCheckAt = System.currentTimeMillis() + LEADER_CHECK_MS;
            next.run();
            return;
        }
        DiagnosticLog.info("BOSS", "leader follow decision=switch from="
                + current + " to=" + leader);
        progress("跟随队长：第 " + current
                + " 分流 → 第 " + leader + " 分流");
        host.tap(1215, 705, () -> host.postDelayed(
                () -> channelSwitcher.switchOpenTo(leader,
                        () -> host.postDelayed(
                                () -> checkAfterChannelSwitch(() -> {
                                    nextLeaderCheckAt = System.currentTimeMillis()
                                            + LEADER_CHECK_MS;
                                    next.run();
                                }), CHANNEL_CHECK_MS)),
                CHANNEL_DIALOG_MS));
    }

    private void failPartyOcr(String target, OcrText text) {
        DiagnosticLog.warn("BOSS", "OCR miss: " + target + "; lines=" + ocrLines(text));
        fail("跟随队长：未识别到" + target);
    }

    private static String ocrLines(OcrText text) {
        List<String> values = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                values.add(line.getText() + "@" + bounds);
            }
        }
        return values.toString();
    }

    private static Integer findLeaderChannel(OcrText text) {
        Integer leader = null;
        int firstRow = Integer.MAX_VALUE;
        List<String> candidates = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds == null
                        || bounds.centerX() < 850 || bounds.centerX() > 950
                        || bounds.centerY() < 190 || bounds.centerY() > 270) {
                    continue;
                }
                Integer channel = parseLeaderChannel(line.getText());
                candidates.add(line.getText() + "@" + bounds + "=>" + channel);
                if (channel != null && bounds.top < firstRow) {
                    firstRow = bounds.top;
                    leader = channel;
                }
            }
        }
        DiagnosticLog.info("BOSS", "leader channel full-screen candidates="
                + candidates + " selected=" + leader);
        return leader;
    }

    static Integer parseLeaderChannel(String value) {
        Integer channel = parseChannel(value);
        if (channel != null) {
            return channel;
        }
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() != 1) {
            return null;
        }
        int digit = digits.charAt(0) - '0';
        return digit >= 1 && digit <= 8 ? digit : null;
    }

    static Integer parseMapX(String value) {
        int[] coordinate = parseMapCoordinate(value);
        return coordinate == null ? null : coordinate[0];
    }

    static int[] parseMapCoordinate(String value) {
        Matcher matcher = MAP_COORDINATE.matcher(value);
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            if (x <= AutomationHost.MAP_GAME_MAX_X && y <= AutomationHost.MAP_GAME_MAX_Y) {
                return new int[] {x, y};
            }
        }
        return null;
    }

    static Integer findCurrentChannel(OcrText text, Bitmap bitmap) {
        Integer current = null;
        int bestScore = 19;
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds != null
                        && bounds.centerX() >= 400 && bounds.centerX() <= 850
                        && bounds.centerY() >= 120 && bounds.centerY() <= 600) {
                    Integer channel = parseChannel(line.getText());
                    if (channel != null) {
                        int score = channelSelectionScore(bitmap, bounds);
                        if (score > bestScore) {
                            bestScore = score;
                            current = channel;
                        }
                    }
                }
            }
        }
        return current;
    }

    private static int channelSelectionScore(Bitmap bitmap, Rect textBounds) {
        int left = Math.max(0, textBounds.right + 5);
        int right = Math.min(bitmap.getWidth(), textBounds.right + 150);
        int top = Math.max(0, textBounds.top - 15);
        int bottom = Math.min(bitmap.getHeight(), textBounds.bottom + 15);
        int score = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int color = bitmap.getPixel(x, y);
                if (isChannelSelectionGold(
                        Color.red(color), Color.green(color), Color.blue(color))) {
                    score++;
                }
            }
        }
        return score;
    }

    static boolean isChannelSelectionGold(int red, int green, int blue) {
        return red >= 120 && green >= 90
                && red > blue * 1.2f && green > blue * 1.1f;
    }

    static Integer parseChannel(String value) {
        Matcher matcher = CHANNEL.matcher(value.replaceAll("\\s+", ""));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1) == null
                ? matcher.group(2) : matcher.group(1));
    }

    static long moveScanDelayMillis(long now, long deadline) {
        return Math.min(MOVE_SCAN_INTERVAL_MS, Math.max(0, deadline - now));
    }

    static List<Integer> buildRoute(int currentX) {
        return buildRoute(currentX, MAP_LEFT, MAP_RIGHT);
    }

    static List<Integer> buildRoute(int currentX, int routeLeft, int routeRight) {
        if (routeLeft < 0 || routeRight > AutomationHost.MAP_GAME_MAX_X
                || routeLeft >= routeRight) {
            throw new IllegalArgumentException("route bounds are invalid");
        }
        int current = Math.max(0, Math.min(currentX, AutomationHost.MAP_GAME_MAX_X));
        int direction = current < (routeLeft + routeRight) / 2 ? 1 : -1;
        int firstTarget = Math.max(routeLeft,
                Math.min(current + direction * 50, routeRight));
        int firstEdge = direction > 0 ? routeRight : routeLeft;
        int secondEdge = direction > 0 ? routeLeft : routeRight;
        List<Integer> values = new ArrayList<>();
        values.add(firstTarget);
        appendLeg(values, firstTarget, firstEdge, direction);
        appendLeg(values, firstEdge, secondEdge, -direction);
        return values;
    }

    private static void appendLeg(List<Integer> values, int start, int end, int direction) {
        for (int x = start + direction * 50;
                direction > 0 ? x < end : x > end;
                x += direction * 50) {
            values.add(x);
        }
        if (values.get(values.size() - 1) != end) {
            values.add(end);
        }
    }

    static BossTarget findRedBoss(
            OcrText text, Bitmap bitmap, int screenLeft, int ocrScale) {
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                for (OcrText.Element element : line.getElements()) {
                    Rect ocrBounds = element.getBoundingBox();
                    String name = element.getText().replaceAll("\\s+", "");
                    if (ocrBounds != null && containsChinese(name)) {
                        Rect bounds = toOriginalBounds(ocrBounds, ocrScale,
                                bitmap.getWidth(), bitmap.getHeight());
                        if (isEnemyListCandidate(bounds.centerY(), bitmap.getHeight())
                                && hasRedText(bitmap, bounds)) {
                            bounds.offset(screenLeft, 0);
                            return new BossTarget(name, bounds);
                        }
                    }
                }
            }
        }
        return null;
    }

    static boolean isEnemyListCandidate(int centerY, int bitmapHeight) {
        return centerY >= 180 * bitmapHeight / 720
                && centerY <= 435 * bitmapHeight / 720;
    }

    static Rect toOriginalBounds(
            Rect bounds, int scale, int bitmapWidth, int bitmapHeight) {
        return new Rect(
                Math.max(0, toOriginalStart(bounds.left, scale)),
                Math.max(0, toOriginalStart(bounds.top, scale)),
                Math.min(bitmapWidth, toOriginalEnd(bounds.right, scale)),
                Math.min(bitmapHeight, toOriginalEnd(bounds.bottom, scale)));
    }

    static int toOriginalStart(int coordinate, int scale) {
        return coordinate / scale;
    }

    static int toOriginalEnd(int coordinate, int scale) {
        return (coordinate + scale - 1) / scale;
    }

    private static boolean containsChinese(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= '\u4e00' && character <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRedText(Bitmap bitmap, Rect rawBounds) {
        Rect bounds = new Rect(
                Math.max(0, rawBounds.left), Math.max(0, rawBounds.top),
                Math.min(bitmap.getWidth(), rawBounds.right),
                Math.min(bitmap.getHeight(), rawBounds.bottom));
        if (bounds.isEmpty()) {
            return false;
        }
        int red = 0;
        int neutral = 0;
        boolean[] redColumns = new boolean[bounds.width()];
        boolean[] redRows = new boolean[bounds.height()];
        for (int y = bounds.top; y < bounds.bottom; y++) {
            for (int x = bounds.left; x < bounds.right; x++) {
                int color = bitmap.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (isBossRed(r, g, b)) {
                    red++;
                    redColumns[x - bounds.left] = true;
                    redRows[y - bounds.top] = true;
                } else if (isNeutralText(r, g, b)) {
                    neutral++;
                }
            }
        }
        return hasRedTextDistribution(red, neutral,
                countTrue(redColumns), countTrue(redRows),
                bounds.width(), bounds.height());
    }

    static boolean isBossRed(int red, int green, int blue) {
        return red >= 110 && red > green * 1.6f && red > blue * 1.35f;
    }

    private static boolean isNeutralText(int red, int green, int blue) {
        return red >= 130 && green >= 130 && blue >= 110
                && Math.max(red, Math.max(green, blue))
                - Math.min(red, Math.min(green, blue)) <= 55;
    }

    private static int countTrue(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    static boolean hasRedTextDistribution(int red, int neutral,
            int redColumns, int redRows, int width, int height) {
        return red >= Math.max(8, width * height / 50)
                && red * 2 >= neutral
                && redColumns >= Math.max(3, width / 5)
                && redRows >= Math.max(3, height / 4);
    }

    static final class BossTarget {
        final String name;
        final Rect bounds;

        private BossTarget(String name, Rect bounds) {
            this.name = name;
            this.bounds = bounds;
        }
    }
}
