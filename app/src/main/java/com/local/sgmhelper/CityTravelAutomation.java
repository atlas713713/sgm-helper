package com.local.sgmhelper;

import java.util.Random;

/**
 * 去某座城市。复刻武当的两段路：能直接传的走 {@code TeleportCityAction}（进客栈/商店 → 传送
 * 面板 → 选城 → 确定），不能直接传的（建安、九原等）先传到中转城，再走到城里的传送官那里
 * 对话跳过去，对应武当 {@code LegionTaskAction.npcTeleport}。
 *
 * <p>这是一段子流程，调用方必须已经在一个自动化任务里；到达后回调 {@code onArrived}，
 * 失败时自己调 {@link AutomationHost#failAutomation(String)}。
 */
final class CityTravelAutomation {
    private static final long ACTION_WAIT_MS = 1_000;
    private static final long MOVE_WAIT_MS = 2_000;
    private static final long PAN_GAP_MS = 200;
    private static final long PAN_SETTLE_MS = 2_000;
    private static final long TELEPORT_WAIT_MS = 5_000;
    private static final long SWIPE_DURATION_MS = 1_000;
    private static final long NPC_WAIT_MS = 2_000;
    private static final long NPC_TELEPORT_WAIT_MS = 4_000;
    private static final int APPROACH_MAX_STUCK = 10;
    private static final int PANEL_RETRIES = 3;
    private static final int PANEL_ROUNDS = 3;
    private static final int CONFIRM_RETRIES = 3;
    private static final int RESULT_POLLS = 30;
    private static final int RESULT_ROUNDS = 2;
    private static final int MAP_NAME_RETRIES = 3;
    private static final int OFFICER_WALK_ATTEMPTS = 45;
    private static final int OFFICER_MAP_POLLS = 20;
    private static final int OFFICER_DIALOG_ROUNDS = 5;
    /** NPC 快捷键，和副本流程用的是同一个按钮。 */
    private static final int NPC_SHORTCUT_X = 1208;
    private static final int NPC_SHORTCUT_Y = 410;
    /** 传送官对话框里“是”固定在第三行；框体沿用 NPCDialog 的 item3。 */
    private static final int NPC_ROW3_LEFT = 133;
    private static final int NPC_ROW3_TOP = 554;
    private static final int NPC_ROW3_RIGHT = 369;
    private static final int NPC_ROW3_BOTTOM = 594;
    private static final String NPC_YES = "是";

    private final AutomationHost host;
    private final Random random = new Random();

    /** 整趟行程的终点，和到达后的回调。 */
    private String finalCity;
    private Runnable onArrived;
    /** 当前这一段城市传送的目标，和这一段走完之后接着做什么。 */
    private String teleportCity;
    private Runnable afterTeleport;
    private String currentCity = "";
    private boolean useStore;
    private int entranceX;
    private int approachStuck;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int panelRetries;
    private int panelRounds;
    private int confirmRetries;
    private int resultRounds;

    CityTravelAutomation(AutomationHost host) {
        this.host = host;
    }

    /** 去 {@code city}；已经在那儿就直接回调。 */
    void travelTo(String city, Runnable onArrived) {
        if (!CityTeleportCatalog.canTravelTo(city)) {
            CityTeleportCatalog.OfficerRoute blocked = CityTeleportCatalog.officerRoute(city);
            host.failAutomation(blocked != null
                    ? "前往" + city + "：传送官只到" + blocked.arrivesAt + "，之后还要跑地图，暂不支持"
                    : "前往" + city + "：没有这座城市的传送数据");
            return;
        }
        this.finalCity = city;
        this.onArrived = onArrived;
        host.showProgress("前往" + city + "：确认当前城市");
        readCurrentCity(MAP_NAME_RETRIES, this::dispatchRoute);
    }

    private void dispatchRoute() {
        if (finalCity.equals(currentCity)) {
            arrive();
            return;
        }
        CityTeleportCatalog.OfficerRoute route = CityTeleportCatalog.officerRoute(finalCity);
        if (route == null) {
            startTeleport(finalCity, this::arrive);
            return;
        }
        if (route.stagingCities.contains(currentCity)) {
            walkToOfficer(route, OFFICER_WALK_ATTEMPTS);
        } else {
            String staging = route.stagingCities.get(0);
            startTeleport(staging, () -> walkToOfficer(route, OFFICER_WALK_ATTEMPTS));
        }
    }

    private void arrive() {
        host.showProgress("前往" + finalCity + "：已到达");
        Runnable next = onArrived;
        onArrived = null;
        if (next != null) {
            next.run();
        }
    }

    // ---------------------------------------------------------------- 城市传送

    private void startTeleport(String city, Runnable next) {
        CityTeleportCatalog.Entry entry = CityTeleportCatalog.teleportEntry(city);
        if (entry == null) {
            host.failAutomation("前往" + city + "：这座城市不能直接传送");
            return;
        }
        // 武当把汉中当特例：必须先落脚成都再传汉中。
        if (CityTeleportCatalog.HANZHONG.equals(city)
                && !CityTeleportCatalog.HANZHONG_VIA.equals(currentCity)) {
            startTeleport(CityTeleportCatalog.HANZHONG_VIA,
                    () -> startTeleport(CityTeleportCatalog.HANZHONG, next));
            return;
        }
        teleportCity = city;
        afterTeleport = next;
        panelRetries = 0;
        panelRounds = 0;
        confirmRetries = 0;
        resultRounds = 0;
        beginTeleportLeg();
    }

    private void beginTeleportLeg() {
        if (teleportCity.equals(currentCity)) {
            finishTeleportLeg();
            return;
        }
        host.showProgress("传送" + teleportCity + "：找客栈或商店");
        host.closeAutoPathPanel(this::findEntrance);
    }

    private void findEntrance() {
        host.recognizeMapCoordinate(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            int[] coordinate = BossAutomation.parseMapCoordinate(text);
            if (coordinate == null) {
                host.failAutomation("传送" + teleportCity + "：读不到城内坐标");
                return;
            }
            useStore = CityTeleportCatalog.useStore(coordinate[0]);
            entranceX = CityTeleportCatalog.entranceX(currentCity, useStore);
            approachStuck = 0;
            lastX = Integer.MIN_VALUE;
            lastY = Integer.MIN_VALUE;
            host.showProgress("传送" + teleportCity + "：走向"
                    + (useStore ? "商店" : "客栈"));
            approachEntrance();
        });
    }

    /** 武当先按同一个 x、随机 y 靠过去，x 对上了再走到门口 (x, 3)。 */
    private void approachEntrance() {
        int strollY = 10 + random.nextInt(10);
        host.tapMapCoordinate(entranceX, strollY, CityTeleportCatalog.CITY_MAP_MAX_X,
                () -> host.postDelayed(this::checkApproach, ACTION_WAIT_MS));
    }

    private void checkApproach() {
        host.recognizeMapCoordinate(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            int[] coordinate = BossAutomation.parseMapCoordinate(text);
            if (coordinate == null) {
                retryApproach();
                return;
            }
            if (Math.abs(coordinate[0] - entranceX) <= CityTeleportCatalog.ARRIVE_TOLERANCE) {
                host.tapMapCoordinate(entranceX, CityTeleportCatalog.ENTRANCE_Y,
                        CityTeleportCatalog.CITY_MAP_MAX_X,
                        () -> host.postDelayed(this::checkPanel, MOVE_WAIT_MS));
                return;
            }
            if (coordinate[0] == lastX && coordinate[1] == lastY) {
                approachStuck++;
            }
            lastX = coordinate[0];
            lastY = coordinate[1];
            retryApproach();
        });
    }

    private void retryApproach() {
        if (approachStuck >= APPROACH_MAX_STUCK) {
            host.failAutomation("传送" + teleportCity + "：走不到"
                    + (useStore ? "商店" : "客栈"));
            return;
        }
        approachEntrance();
    }

    private void checkPanel() {
        if (useStore) {
            host.waitTemplateOrText(WudangTemplateMatcher.Template.SHOP,
                    "商店", true,
                    CityTeleportCatalog.PANEL_TITLE_LEFT,
                    CityTeleportCatalog.PANEL_TITLE_TOP,
                    CityTeleportCatalog.PANEL_TITLE_RIGHT,
                    CityTeleportCatalog.PANEL_TITLE_BOTTOM,
                    2, 1, this::openTeleportTab, this::handlePanelMissing);
            return;
        }
        host.recognizeTextRegion(CityTeleportCatalog.PANEL_TITLE_LEFT,
                CityTeleportCatalog.PANEL_TITLE_TOP, CityTeleportCatalog.PANEL_TITLE_RIGHT,
                CityTeleportCatalog.PANEL_TITLE_BOTTOM, text -> {
                    if (!host.isAutomationRunning()) {
                        return;
                    }
                    String title = text == null ? "" : text.getText();
                    if (matchesPanelTitle(title, useStore)) {
                        panelRetries = 0;
                        openTeleportTab();
                        return;
                    }
                    DiagnosticLog.info("TRAVEL", "panel title=" + title);
                    handlePanelMissing();
                });
    }

    private void handlePanelMissing() {
        if (panelRetries < PANEL_RETRIES) {
            panelRetries++;
            host.postDelayed(this::checkPanel, ACTION_WAIT_MS);
            return;
        }
        panelRetries = 0;
        panelRounds++;
        if (panelRounds > PANEL_ROUNDS) {
            host.failAutomation("传送" + teleportCity + "：没能打开"
                    + (useStore ? "商店" : "客栈"));
            return;
        }
        // 退开一步再重新走进去，和武当一样。
        approachStuck = 0;
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        host.tapMapCoordinate(entranceX, CityTeleportCatalog.RETRY_Y,
                CityTeleportCatalog.CITY_MAP_MAX_X,
                () -> host.postDelayed(this::approachEntrance, MOVE_WAIT_MS));
    }

    private void openTeleportTab() {
        host.showProgress("传送" + teleportCity + "：打开传送面板");
        if (!useStore) {
            host.postDelayed(this::panTeleportMap, ACTION_WAIT_MS);
            return;
        }
        host.tapUi(CityTeleportCatalog.STORE_TELEPORT_TAB_X,
                CityTeleportCatalog.STORE_TELEPORT_TAB_Y,
                () -> host.postDelayed(this::panTeleportMap, ACTION_WAIT_MS));
    }

    private void panTeleportMap() {
        int[] pan = CityTeleportCatalog.panGesture(
                CityTeleportCatalog.teleportEntry(teleportCity).page);
        if (pan == null) {
            pickCity();
            return;
        }
        host.showProgress("传送" + teleportCity + "：翻到目标区域");
        panOnce(pan, () -> host.postDelayed(
                () -> panOnce(pan, () -> host.postDelayed(this::pickCity, PAN_SETTLE_MS)),
                PAN_GAP_MS));
    }

    private void panOnce(int[] pan, Runnable next) {
        host.swipe(pan[0], pan[1], pan[2], pan[3], SWIPE_DURATION_MS, next);
    }

    private void pickCity() {
        CityTeleportCatalog.Entry entry = CityTeleportCatalog.teleportEntry(teleportCity);
        host.showProgress("传送" + teleportCity + "：选择城市");
        // 武当点两下城市再点“传送”，第一下常常只是把地图对焦过去。
        host.tapUi(entry.x, entry.y, () -> host.postDelayed(
                () -> host.tapUi(entry.x, entry.y, () -> host.postDelayed(
                        () -> host.tapUi(CityTeleportCatalog.TELEPORT_BUTTON_X,
                                CityTeleportCatalog.TELEPORT_BUTTON_Y,
                                () -> host.postDelayed(this::confirmTeleport, ACTION_WAIT_MS)),
                        ACTION_WAIT_MS)),
                ACTION_WAIT_MS));
    }

    private void confirmTeleport() {
        host.recognizeTextRegion(CityTeleportCatalog.CONFIRM_LEFT,
                CityTeleportCatalog.CONFIRM_TOP, CityTeleportCatalog.CONFIRM_RIGHT,
                CityTeleportCatalog.CONFIRM_BOTTOM, text -> {
                    if (!host.isAutomationRunning()) {
                        return;
                    }
                    String button = text == null ? "" : text.getText().replaceAll("\\s", "");
                    if (button.contains("确定")) {
                        confirmRetries = 0;
                        host.showProgress("传送" + teleportCity + "：确认传送");
                        host.tapUi(CityTeleportCatalog.CONFIRM_X, CityTeleportCatalog.CONFIRM_Y,
                                () -> host.postDelayed(
                                        () -> waitForArrival(RESULT_POLLS), TELEPORT_WAIT_MS));
                        return;
                    }
                    if (confirmRetries >= CONFIRM_RETRIES) {
                        host.failAutomation("传送" + teleportCity + "：没有出现确认框");
                        return;
                    }
                    confirmRetries++;
                    host.tapUi(CityTeleportCatalog.TELEPORT_BUTTON_X,
                            CityTeleportCatalog.TELEPORT_BUTTON_Y,
                            () -> host.postDelayed(this::confirmTeleport, ACTION_WAIT_MS));
                });
    }

    private void waitForArrival(int remainingPolls) {
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String name = normalize(mapName);
            if (teleportCity.equals(name)) {
                currentCity = name;
                closeTipDialog(this::finishTeleportLeg);
                return;
            }
            if (remainingPolls > 1) {
                host.postDelayed(() -> waitForArrival(remainingPolls - 1), ACTION_WAIT_MS);
                return;
            }
            resultRounds++;
            if (resultRounds > RESULT_ROUNDS) {
                host.failAutomation("传送" + teleportCity + "：传送后没有到达");
                return;
            }
            host.showProgress("传送" + teleportCity + "：没到，重新来一次");
            readCurrentCity(MAP_NAME_RETRIES, this::beginTeleportLeg);
        });
    }

    private void finishTeleportLeg() {
        Runnable next = afterTeleport;
        afterTeleport = null;
        if (next != null) {
            next.run();
        }
    }

    /** 传送落地后偶尔弹提示框，武当就是 OCR 到“关闭”才点。 */
    private void closeTipDialog(Runnable next) {
        host.recognizeTextRegion(CityTeleportCatalog.TIP_CLOSE_LEFT,
                CityTeleportCatalog.TIP_CLOSE_TOP, CityTeleportCatalog.TIP_CLOSE_RIGHT,
                CityTeleportCatalog.TIP_CLOSE_BOTTOM, text -> {
                    if (!host.isAutomationRunning()) {
                        return;
                    }
                    String button = text == null ? "" : text.getText().replaceAll("\\s", "");
                    if (button.contains("关闭")) {
                        host.tapUi(CityTeleportCatalog.TIP_CLOSE_X,
                                CityTeleportCatalog.TIP_CLOSE_Y,
                                () -> host.postDelayed(next, ACTION_WAIT_MS));
                    } else {
                        next.run();
                    }
                });
    }

    // ---------------------------------------------------------------- 传送官

    private void walkToOfficer(CityTeleportCatalog.OfficerRoute route, int remainingAttempts) {
        host.showProgress("前往" + finalCity + "：走向" + currentCity + "的传送官");
        host.tapMapCoordinate(route.officerX, route.officerY,
                CityTeleportCatalog.CITY_MAP_MAX_X,
                () -> host.postDelayed(
                        () -> checkOfficerArrival(route, remainingAttempts), MOVE_WAIT_MS));
    }

    private void checkOfficerArrival(
            CityTeleportCatalog.OfficerRoute route, int remainingAttempts) {
        host.recognizeMapCoordinate(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (DungeonBattleAutomation.isAtCoordinate(text, route.officerX, route.officerY,
                    CityTeleportCatalog.ARRIVE_TOLERANCE)) {
                host.postDelayed(() -> talkToOfficer(route, remainingAttempts), MOVE_WAIT_MS);
                return;
            }
            if (remainingAttempts <= 1) {
                host.failAutomation("前往" + finalCity + "：走不到传送官");
                return;
            }
            host.tapMapCoordinate(route.officerX, route.officerY,
                    CityTeleportCatalog.CITY_MAP_MAX_X,
                    () -> host.postDelayed(
                            () -> checkOfficerArrival(route, remainingAttempts - 1),
                            ACTION_WAIT_MS));
        });
    }

    private void talkToOfficer(CityTeleportCatalog.OfficerRoute route, int remainingAttempts) {
        host.showProgress("前往" + finalCity + "：和传送官对话");
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> host.postDelayed(
                () -> confirmOfficerDialog(route, remainingAttempts), NPC_WAIT_MS));
    }

    private void confirmOfficerDialog(
            CityTeleportCatalog.OfficerRoute route, int remainingAttempts) {
        host.recognizeTextRegion(NPC_ROW3_LEFT, NPC_ROW3_TOP, NPC_ROW3_RIGHT, NPC_ROW3_BOTTOM,
                text -> {
                    if (!host.isAutomationRunning()) {
                        return;
                    }
                    String option = text == null ? "" : text.getText().replaceAll("\\s", "");
                    if (option.contains(NPC_YES)) {
                        host.tap(DungeonBattleAutomation.npcOptionX(3),
                                DungeonBattleAutomation.npcOptionY(3),
                                () -> host.postDelayed(
                                        () -> waitForOfficerTeleport(route, OFFICER_MAP_POLLS,
                                                OFFICER_DIALOG_ROUNDS),
                                        NPC_TELEPORT_WAIT_MS));
                        return;
                    }
                    DiagnosticLog.info("TRAVEL", "officer dialog row3=" + option);
                    if (remainingAttempts <= 1) {
                        host.failAutomation("前往" + finalCity + "：传送官没有出现“是”");
                        return;
                    }
                    host.postDelayed(
                            () -> checkOfficerArrival(route, remainingAttempts - 1),
                            ACTION_WAIT_MS);
                });
    }

    private void waitForOfficerTeleport(CityTeleportCatalog.OfficerRoute route,
            int remainingPolls, int remainingRounds) {
        String expected = route.landsIn(finalCity);
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String name = normalize(mapName);
            if (expected.equals(name)) {
                currentCity = name;
                arrive();
                return;
            }
            if (remainingPolls > 1) {
                host.postDelayed(
                        () -> waitForOfficerTeleport(route, remainingPolls - 1, remainingRounds),
                        ACTION_WAIT_MS);
                return;
            }
            if (remainingRounds <= 1) {
                host.failAutomation("前往" + finalCity + "：传送官没有把人送过去");
                return;
            }
            // 再点一次 NPC 和“是”，武当也是这么补救的。
            host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> host.postDelayed(
                    () -> host.tap(DungeonBattleAutomation.npcOptionX(3),
                            DungeonBattleAutomation.npcOptionY(3),
                            () -> host.postDelayed(
                                    () -> waitForOfficerTeleport(route, OFFICER_MAP_POLLS,
                                            remainingRounds - 1),
                                    NPC_WAIT_MS)),
                    NPC_WAIT_MS));
        });
    }

    // ---------------------------------------------------------------- 公共

    private void readCurrentCity(int remainingAttempts, Runnable next) {
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String name = normalize(mapName);
            if (CityTeleportCatalog.isKnownCity(name)) {
                currentCity = name;
                next.run();
                return;
            }
            if (remainingAttempts > 1) {
                host.postDelayed(() -> readCurrentCity(remainingAttempts - 1, next),
                        ACTION_WAIT_MS);
                return;
            }
            // 不在城里（荒野、副本）就先回城，回城后再读一次。
            DiagnosticLog.info("TRAVEL", "not in a city, map=" + name);
            host.showProgress("前往" + finalCity + "：先回城");
            host.returnHome(() -> host.postDelayed(
                    () -> readCurrentCityAfterHome(MAP_NAME_RETRIES, next), MOVE_WAIT_MS));
        });
    }

    private void readCurrentCityAfterHome(int remainingAttempts, Runnable next) {
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String name = normalize(mapName);
            if (CityTeleportCatalog.isKnownCity(name)) {
                currentCity = name;
                next.run();
                return;
            }
            if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> readCurrentCityAfterHome(remainingAttempts - 1, next),
                        ACTION_WAIT_MS);
                return;
            }
            host.failAutomation("前往" + finalCity + "：回城后仍未识别到城市");
        });
    }

    static String normalize(String mapName) {
        return mapName == null ? "" : mapName.replaceAll("\\s", "");
    }

    /** 客栈和商店共用一个标题框，武当靠模板图区分，我们改成 OCR 标题文字。 */
    static boolean matchesPanelTitle(String title, boolean store) {
        String value = title == null ? "" : title.replaceAll("\\s", "");
        return value.contains(store ? "商店" : "客栈");
    }
}
