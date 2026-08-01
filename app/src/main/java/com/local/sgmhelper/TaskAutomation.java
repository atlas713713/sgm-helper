package com.local.sgmhelper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TaskAutomation {
    private static final int ACTION_DELAY_MS = 1000;
    private static final int WILDERNESS_PROGRESS_CHECK_MS = 5 * 60 * 1000;
    private static final int SUPPLY_PROGRESS_CHECK_MS = 3 * 60 * 1000;
    private static final int TEXT_RETRY_COUNT = 5;
    private static final int SCREEN_WAIT_RETRY_COUNT = 20;
    private static final int TASK_START_CONFIRM_RETRY_COUNT = 4;
    private static final int SUPPLY_CLICK_RETRY_COUNT = 3;
    private static final int MILITARY_TEXT_LEFT = 80;
    private static final int MILITARY_TEXT_TOP = 70;
    private static final int MILITARY_TEXT_RIGHT = 970;
    private static final int MILITARY_TEXT_BOTTOM = 580;
    // Wudang's LegionTask/NPCDialog ItemViews, expressed in the app's
    // 1280x720 reference coordinate system.
    private static final int MILITARY_TASK_LIST_LEFT = 110;
    private static final int MILITARY_TASK_LIST_TOP = 125;
    private static final int MILITARY_TASK_LIST_RIGHT = 560;
    private static final int MILITARY_TASK_LIST_BOTTOM = 660;
    private static final int MILITARY_TASK_DETAIL_LEFT = 570;
    private static final int MILITARY_TASK_DETAIL_TOP = 120;
    private static final int MILITARY_TASK_DETAIL_RIGHT = 950;
    private static final int MILITARY_TASK_DETAIL_BOTTOM = 435;
    private static final int MILITARY_RIGHT_TASK_LEFT = 900;
    private static final int MILITARY_RIGHT_TASK_TOP = 120;
    private static final int MILITARY_RIGHT_TASK_RIGHT = 1245;
    private static final int MILITARY_RIGHT_TASK_BOTTOM = 470;
    private static final int NPC_NAME_LEFT = 120;
    private static final int NPC_NAME_TOP = 195;
    private static final int NPC_NAME_RIGHT = 380;
    private static final int NPC_NAME_BOTTOM = 245;
    private static final int NPC_DIALOG_LEFT = 105;
    private static final int NPC_DIALOG_TOP = 235;
    private static final int NPC_DIALOG_RIGHT = 415;
    private static final int NPC_DIALOG_BOTTOM = 680;
    private static final int TASK_CONFIRM_LEFT = 560;
    private static final int TASK_CONFIRM_TOP = 430;
    private static final int TASK_CONFIRM_RIGHT = 760;
    private static final int TASK_CONFIRM_BOTTOM = 535;
    private static final int AUTO_PATH_LEFT = 400;
    private static final int AUTO_PATH_TOP = 150;
    private static final int AUTO_PATH_RIGHT = 800;
    private static final int AUTO_PATH_BOTTOM = 600;
    private static final Pattern WILDERNESS_ZONE = Pattern.compile(
            "巡狩军团荒野[（(]([一二三四五六七八九十百0-9]+)[）)]");
    private static final Pattern MILITARY_COOLDOWN = Pattern.compile(
            "冷却(?:时间)?[：:]?(?:(\\d+)小时)?(?:(\\d+)分(?:钟)?)?(?:(\\d+)秒)?");
    private static final Pattern REQUIRED_ITEM_PROGRESS = Pattern.compile(
            "(\\d+)[/／](\\d+)");
    private static final String[] MILITARY_QUEST_NAMES = {
            "补充军团物资", "巡狩军团荒野", "勇讨军团天将"
    };

    private final AutomationHost host;
    private final WildernessNavigator wildernessNavigator;
    private String militaryZone;
    private boolean supplyQuestCooling;
    private boolean wildernessQuestCooling;
    private boolean supplyQuestEnabled;
    private boolean wildernessQuestEnabled;
    private boolean inWildernessTraining;
    private final Set<String> checkedMilitaryQuests = new HashSet<>();
    private final Set<String> retriedUnacceptedQuests = new HashSet<>();
    /** 已经为它跑过一趟军务城市的任务，避免来回传送。 */
    private final Set<String> traveledMilitaryQuests = new HashSet<>();
    private final CityTravelAutomation cityTravel;

    TaskAutomation(AutomationHost host) {
        this.host = host;
        wildernessNavigator = new WildernessNavigator(host, "自动军务");
        cityTravel = new CityTravelAutomation(host);
    }

    void start() {
        host.startAutomation("自动军务：原地检查收集进度", this::begin);
    }

    private void begin() {
        host.checkInventoryBeforePrimary(this::beginAfterInventoryCheck);
    }

    private void beginAfterInventoryCheck() {
        militaryZone = null;
        inWildernessTraining = false;
        resetMilitaryQuestChoices();
        WorshipAlarmReceiver.scheduleMilitary(host.context());
        host.showProgress("自动军务：停止自动攻击");
        host.ensureAutoAttackDisabled(
                () -> host.postDelayed(this::inspectOngoingBeforeAcceptance, 1_000));
    }

    private void inspectOngoingBeforeAcceptance() {
        host.showProgress("自动军务：检查进行中的收集任务");
        detectWildernessLocation(() ->
                host.closeAutoPathPanel(() -> withTaskWindowOpen(
                        () -> host.tapFast(180, 101,
                                () -> findOngoingCollectionTask(TEXT_RETRY_COUNT)))));
    }

    private void findOngoingCollectionTask(int remainingAttempts) {
        recognizeMilitaryTaskList(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            List<OcrText.Line> leftLines = new ArrayList<>();
            List<String> leftValues = new ArrayList<>();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && bounds.centerX() < 640) {
                        leftLines.add(line);
                        leftValues.add(line.getText());
                    }
                }
            }
            String questName = firstOngoingCollectionQuest(
                    leftValues, supplyQuestEnabled, wildernessQuestEnabled);
            if (questName != null) {
                for (OcrText.Line line : leftLines) {
                    if (questName.equals(extractMilitaryQuestName(line.getText()))) {
                        String zone = extractWildernessZone(line.getText());
                        if (zone != null) {
                            militaryZone = zone;
                        }
                        Runnable completed = this::recheckAvailableMilitaryQuests;
                        host.showProgress("自动军务：检查进行中的" + questName);
                        Rect bounds = line.getBoundingBox();
                        host.tap(bounds.centerX(), bounds.centerY(),
                                () -> inspectOngoingMilitaryQuest(
                                        questName, completed,
                                        () -> continueCollecting(
                                                questName, completed)));
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> findOngoingCollectionTask(remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else {
                returnHomeAndAcceptTasks();
            }
        });
    }

    private void returnHomeAndAcceptTasks() {
        host.showProgress("自动军务：没有进行中的收集任务，回城承接");
        host.tap(982, 52,
                () -> host.returnHome(
                        () -> host.postDelayed(this::openMilitaryTask, 9_000)));
    }

    static String firstOngoingCollectionQuest(List<String> values) {
        return firstOngoingCollectionQuest(values, true, true);
    }

    static String firstOngoingCollectionQuest(
            List<String> values, boolean supplyEnabled, boolean wildernessEnabled) {
        boolean supply = false;
        for (String value : values) {
            String questName = extractMilitaryQuestName(value);
            if (wildernessEnabled && "巡狩军团荒野".equals(questName)) {
                return questName;
            }
            supply |= supplyEnabled && "补充军团物资".equals(questName);
        }
        return supply ? "补充军团物资" : null;
    }

    private void openMilitaryTask() {
        host.showProgress("自动军务：检查自动寻路面板");
        host.closeAutoPathPanel(() -> {
            host.showProgress("自动军务：打开可承接任务");
            withTaskWindowOpen(
                    () -> host.tap(55, 369,
                            () -> host.tap(316, 101, this::selectMilitaryQuest)));
        });
    }

    private void selectMilitaryQuest() {
        host.showProgress("自动军务：承接军团任务");
        findAvailableMilitaryQuest(TEXT_RETRY_COUNT);
    }

    private void findAvailableMilitaryQuest(int remainingAttempts) {
        recognizeMilitaryTaskList(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    String questName = extractMilitaryQuestName(line.getText());
                    if (questName != null && bounds != null && bounds.centerX() < 640
                            && checkedMilitaryQuests.add(questName)) {
                        String zone = extractWildernessZone(line.getText());
                        if (zone != null) {
                            militaryZone = zone;
                        }
                        host.showProgress("自动军务：检查" + questName);
                        host.tap(bounds.centerX(), bounds.centerY(),
                                () -> inspectAvailableMilitaryQuest(questName));
                        return;
                    }
                }
            }
            if (checkedMilitaryQuests.size() < MILITARY_QUEST_NAMES.length
                    && remainingAttempts > 1) {
                host.postDelayed(
                        () -> findAvailableMilitaryQuest(remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else if (allExecutableMilitaryQuestsCooling(
                    supplyQuestCooling, wildernessQuestCooling)) {
                host.showProgress("自动军务：无可执行任务，准备练级");
                host.tap(982, 52, this::finishMilitaryAndStartTraining);
            } else {
                startWildernessPhase();
            }
        });
    }

    private void inspectAvailableMilitaryQuest(String questName) {
        recognizeMilitaryTaskDetail(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String rightDetails = rightMilitaryDetails(text);
            if (!isMilitaryQuestDetail(questName, rightDetails)) {
                DiagnosticLog.warn("OCR", "right military detail mismatch: expected="
                        + questName + "; text=" + rightDetails);
                host.failAutomation("右侧任务详情未切换到：" + questName);
                return;
            }
            String cooldown = isCooldownText(rightDetails) ? rightDetails : null;
            if (cooldown != null) {
                if ("补充军团物资".equals(questName)) {
                    supplyQuestCooling = true;
                } else if ("巡狩军团荒野".equals(questName)) {
                    wildernessQuestCooling = true;
                }
                Integer cooldownMinutes = extractCooldownMinutes(cooldown);
                if (cooldownMinutes != null && expectedMilitaryNpc(questName) != null) {
                    long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitaryAfterCooldown(
                            host.context(), questName, cooldownMinutes);
                    host.showProgress("自动军务：跳过" + questName
                            + "，下次 " + host.formatTime(nextMilitaryAt));
                } else {
                    DiagnosticLog.error("AUTOMATION", "Unable to parse cooldown: " + cooldown);
                    host.showProgress("自动军务：跳过冷却中的" + questName);
                }
                findAvailableMilitaryQuest(TEXT_RETRY_COUNT);
            } else {
                acceptMilitaryQuestInCity(questName, rightDetails);
            }
        });
    }

    /**
     * 武当先去军务城市再承接（{@code toTaskCity} → {@code checkTaskCity} → {@code getNewTask}）。
     * 任务详情里写了别的城市就先关掉任务窗、过去，再重新打开任务窗承接。
     */
    private void acceptMilitaryQuestInCity(String questName, String details) {
        String city = CityTeleportCatalog.findCity(details);
        if (city == null || !traveledMilitaryQuests.add(questName)) {
            acceptMilitaryQuest(questName);
            return;
        }
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (city.equals(CityTravelAutomation.normalize(mapName))) {
                acceptMilitaryQuest(questName);
                return;
            }
            if (!CityTeleportCatalog.canTravelTo(city)) {
                host.showProgress("自动军务：" + questName + "在" + city + "，暂时到不了，跳过");
                findAvailableMilitaryQuest(TEXT_RETRY_COUNT);
                return;
            }
            host.showProgress("自动军务：" + questName + "在" + city + "，先过去");
            // 过去之后要重新挑这个任务，所以把它从“已看过”里摘掉。
            checkedMilitaryQuests.remove(questName);
            host.tap(982, 52, () -> cityTravel.travelTo(city, this::openMilitaryTask));
        });
    }

    private void startSupplyQuest() {
        continueCollecting(
                "补充军团物资", this::recheckAvailableMilitaryQuests);
    }

    private void continueCollecting(String questName, Runnable completed) {
        continueCollecting(questName, completed, SUPPLY_CLICK_RETRY_COUNT);
    }

    private void continueCollecting(
            String questName, Runnable completed, int remainingClickAttempts) {
        if (shouldEnterWilderness(questName, inWildernessTraining)) {
            host.showProgress("自动军务：当前不在荒野，重新前往荒野");
            openWildernessTraining();
            return;
        }
        if (!shouldResumeSupplyCollection(questName)) {
            scheduleMilitaryQuestCheck(questName, completed);
            return;
        }
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (isWildernessLocation(List.of(mapName))) {
                host.showProgress("自动军务：物资任务必须在城内，先回城");
                host.returnHome(() -> host.postDelayed(
                        () -> continueCollecting(
                                questName, completed, remainingClickAttempts),
                        9_000));
                return;
            }
            clickSupplyQuest(questName, completed, remainingClickAttempts);
        });
    }

    private void clickSupplyQuest(
            String questName, Runnable completed, int remainingClickAttempts) {
        host.showProgress("自动军务：再次点击右侧补充军团物资");
        Runnable scheduleNextCheck = () -> scheduleMilitaryQuestCheck(
                questName, completed);
        clickMilitaryRightTaskText("团物资",
                scheduleNextCheck,
                TEXT_RETRY_COUNT,
                () -> {
                    if (remainingClickAttempts > 1) {
                        host.showProgress("自动军务：未点到右侧物资任务，短间隔重试");
                        host.postDelayed(
                                () -> continueCollecting(
                                        questName, completed, remainingClickAttempts - 1),
                                ACTION_DELAY_MS);
                    } else {
                        host.showProgress("自动军务：连续未点到右侧物资任务，保留复查");
                        logSupplyTaskOcrMiss(scheduleNextCheck);
                    }
                });
    }

    private void logSupplyTaskOcrMiss(Runnable next) {
        host.recognizeTextRegion(
                MILITARY_RIGHT_TASK_LEFT, MILITARY_RIGHT_TASK_TOP,
                MILITARY_RIGHT_TASK_RIGHT, MILITARY_RIGHT_TASK_BOTTOM, text -> {
            List<String> rightLines = new ArrayList<>();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && bounds.centerX() >= 850) {
                        rightLines.add(line.getText() + "@" + bounds.flattenToString());
                    }
                }
            }
            DiagnosticLog.warn("OCR", "right text not found: 补充军团物资; lines="
                    + rightLines);
            host.captureScreenshot(bitmap -> {
                try {
                    DiagnosticLog.saveScreenshot(bitmap, "supply-task-ocr-miss");
                    if (bitmap != null) {
                        int left = MILITARY_RIGHT_TASK_LEFT * bitmap.getWidth() / 1280;
                        int top = MILITARY_RIGHT_TASK_TOP * bitmap.getHeight() / 720;
                        int right = MILITARY_RIGHT_TASK_RIGHT * bitmap.getWidth() / 1280;
                        int bottom = MILITARY_RIGHT_TASK_BOTTOM * bitmap.getHeight() / 720;
                        Bitmap crop = Bitmap.createBitmap(
                                bitmap, left, top, right - left, bottom - top);
                        DiagnosticLog.saveScreenshot(crop, "supply-task-ocr-crop");
                        crop.recycle();
                        DiagnosticLog.info("OCR", "right task OCR crop=("
                                + MILITARY_RIGHT_TASK_LEFT + ","
                                + MILITARY_RIGHT_TASK_TOP + ")-("
                                + MILITARY_RIGHT_TASK_RIGHT + ","
                                + MILITARY_RIGHT_TASK_BOTTOM + ")");
                    }
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    next.run();
                }
            });
        });
    }

    static boolean shouldResumeSupplyCollection(String questName) {
        return "补充军团物资".equals(questName);
    }

    static boolean shouldEnterWilderness(String questName, boolean inWilderness) {
        return "巡狩军团荒野".equals(questName) && !inWilderness;
    }

    private void startWildernessPhase() {
        if (wildernessQuestCooling) {
            startSupplyPhase();
            return;
        }
        host.showProgress("自动军务：立即检查巡狩军团荒野");
        withTaskWindowOpen(() -> host.tap(180, 101,
                () -> findOngoingWildernessQuest(TEXT_RETRY_COUNT, 2)));
    }

    private void startSupplyPhase() {
        if (supplyQuestCooling) {
            finishMilitaryAndStartTraining();
            return;
        }
        host.showProgress("自动军务：立即检查补充军团物资");
        withTaskWindowOpen(() -> host.tap(180, 101,
                () -> clickMilitaryTaskListText("补充军团物资",
                        () -> inspectOngoingMilitaryQuest(
                                "补充军团物资",
                                this::recheckAvailableMilitaryQuests,
                                this::startSupplyQuest),
                        TEXT_RETRY_COUNT,
                        () -> retryUnacceptedMilitaryQuest("补充军团物资"))));
    }

    private void recheckAvailableMilitaryQuests() {
        resetMilitaryQuestChoices();
        host.showProgress("自动军务：任务完成，重新检查可承接");
        withTaskWindowOpen(
                () -> host.tap(55, 369,
                        () -> host.tap(316, 101, this::selectMilitaryQuest)));
    }

    private void resetMilitaryQuestChoices() {
        SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, 0);
        supplyQuestEnabled = preferences.getBoolean(
                HelperAccessibilityService.PREF_MILITARY_SUPPLY_ENABLED, true);
        wildernessQuestEnabled = preferences.getBoolean(
                HelperAccessibilityService.PREF_MILITARY_WILDERNESS_ENABLED, true);
        supplyQuestCooling = !supplyQuestEnabled;
        wildernessQuestCooling = !wildernessQuestEnabled;
        checkedMilitaryQuests.clear();
        checkedMilitaryQuests.add("勇讨军团天将");
        if (!supplyQuestEnabled) {
            checkedMilitaryQuests.add("补充军团物资");
        }
        if (!wildernessQuestEnabled) {
            checkedMilitaryQuests.add("巡狩军团荒野");
        }
        retriedUnacceptedQuests.clear();
        traveledMilitaryQuests.clear();
    }

    private void withTaskWindowOpen(Runnable next) {
        withTaskWindowOpen(next, 3);
    }

    private void withTaskWindowOpen(Runnable next, int remainingOpenAttempts) {
        recognizeMilitaryText(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (isTaskWindowVisible(screenLines(text))) {
                next.run();
            } else {
                host.showProgress("自动军务：打开任务窗口");
                host.tapFast(1255, 285,
                        () -> waitForTaskWindow(
                                next, TEXT_RETRY_COUNT, remainingOpenAttempts - 1));
            }
        });
    }

    private void waitForTaskWindow(
            Runnable next, int remainingTextAttempts, int remainingOpenAttempts) {
        recognizeMilitaryText(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (isTaskWindowVisible(screenLines(text))) {
                next.run();
            } else if (remainingTextAttempts > 1) {
                host.postDelayed(
                        () -> waitForTaskWindow(
                                next, remainingTextAttempts - 1, remainingOpenAttempts),
                        ACTION_DELAY_MS);
            } else if (remainingOpenAttempts > 0) {
                host.showProgress("自动军务：任务窗口未打开，先关闭自动寻路");
                host.closeAutoPathPanel(
                        () -> withTaskWindowOpen(next, remainingOpenAttempts));
            } else {
                host.failAutomation("连续重试后仍无法打开任务窗口");
            }
        });
    }

    private List<String> screenLines(OcrText text) {
        List<String> values = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                values.add(line.getText());
            }
        }
        return values;
    }

    private void recognizeMilitaryText(Consumer<OcrText> result) {
        host.recognizeTextRegion(
                MILITARY_TEXT_LEFT, MILITARY_TEXT_TOP,
                MILITARY_TEXT_RIGHT, MILITARY_TEXT_BOTTOM, result);
    }

    private void recognizeMilitaryTaskList(Consumer<OcrText> result) {
        host.recognizeTextRegion(
                MILITARY_TASK_LIST_LEFT, MILITARY_TASK_LIST_TOP,
                MILITARY_TASK_LIST_RIGHT, MILITARY_TASK_LIST_BOTTOM, result);
    }

    private void recognizeMilitaryTaskDetail(Consumer<OcrText> result) {
        host.recognizeTextRegion(
                MILITARY_TASK_DETAIL_LEFT, MILITARY_TASK_DETAIL_TOP,
                MILITARY_TASK_DETAIL_RIGHT, MILITARY_TASK_DETAIL_BOTTOM, result);
    }

    private void clickMilitaryTaskListText(String expected, Runnable next,
            int attempts, Runnable ifMissing) {
        host.clickTextRegion(expected, false,
                MILITARY_TASK_LIST_LEFT, MILITARY_TASK_LIST_TOP,
                MILITARY_TASK_LIST_RIGHT, MILITARY_TASK_LIST_BOTTOM,
                next, attempts, ifMissing);
    }

    private void clickMilitaryRightTaskText(String expected, Runnable next,
            int attempts, Runnable ifMissing) {
        host.clickTextRegion(expected, false,
                MILITARY_RIGHT_TASK_LEFT, MILITARY_RIGHT_TASK_TOP,
                MILITARY_RIGHT_TASK_RIGHT, MILITARY_RIGHT_TASK_BOTTOM,
                next, attempts, ifMissing);
    }

    static boolean isTaskWindowVisible(List<String> values) {
        boolean title = false;
        boolean ongoing = false;
        boolean available = false;
        boolean count = false;
        boolean target = false;
        boolean quickArrival = false;
        boolean abandon = false;
        for (String value : values) {
            String normalized = normalizeText(value);
            title |= normalized.equals("任务");
            ongoing |= normalized.contains("进行中");
            available |= normalized.contains("可承接");
            count |= normalized.contains("任务计数");
            target |= normalized.contains("任务目标");
            quickArrival |= normalized.contains("快速抵达");
            abandon |= normalized.contains("放弃任务");
        }
        int markers = (title ? 1 : 0) + (ongoing ? 1 : 0) + (available ? 1 : 0)
                + (count ? 1 : 0) + (target ? 1 : 0)
                + (quickArrival ? 1 : 0) + (abandon ? 1 : 0);
        return markers >= 2;
    }

    private void acceptMilitaryQuest(String questName) {
        host.showProgress("自动军务：承接" + questName);
        host.clickQuickArrival(
                () -> host.tap(641, 473,
                        () -> {
                            String npc = expectedMilitaryNpc(questName);
                            if (npc == null) {
                                host.postDelayed(
                                        () -> finishMilitaryConversation(questName), 19_000);
                            } else {
                                host.showProgress("自动军务：等待" + npc + "对话");
                                host.waitForTextRegion(npc,
                                        NPC_NAME_LEFT, NPC_NAME_TOP,
                                        NPC_NAME_RIGHT, NPC_NAME_BOTTOM,
                                        SCREEN_WAIT_RETRY_COUNT,
                                        () -> finishMilitaryConversation(questName),
                                        () -> host.failAutomation("NPC 窗口未找到：" + npc));
                            }
                        }));
    }

    static String expectedMilitaryNpc(String questName) {
        if ("补充军团物资".equals(questName)) {
            return "常务军士";
        }
        if ("巡狩军团荒野".equals(questName)) {
            return "荒野军士";
        }
        return null;
    }

    private void findOngoingWildernessQuest(
            int remainingTextAttempts, int remainingRecoveryAttempts) {
        recognizeMilitaryTaskList(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    String zone = extractWildernessZone(line.getText());
                    if ("巡狩军团荒野".equals(extractMilitaryQuestName(line.getText()))
                            && bounds != null && bounds.centerX() < 640
                            && (zone != null || militaryZone != null)) {
                        if (zone != null) {
                            militaryZone = zone;
                        }
                        host.showProgress("自动军务：立即检查荒野任务");
                        host.tap(bounds.centerX(), bounds.centerY(),
                                () -> inspectOngoingMilitaryQuest(
                                        "巡狩军团荒野",
                                        this::recheckAvailableMilitaryQuests,
                                        this::openWildernessTraining));
                        return;
                    }
                }
            }
            if (remainingTextAttempts > 1) {
                host.postDelayed(
                        () -> findOngoingWildernessQuest(
                                remainingTextAttempts - 1, remainingRecoveryAttempts),
                        ACTION_DELAY_MS);
            } else if (remainingRecoveryAttempts > 0) {
                host.showProgress("自动军务：未找到荒野任务，重新打开任务窗口");
                host.postDelayed(
                        () -> withTaskWindowOpen(() -> host.tap(180, 101,
                                () -> findOngoingWildernessQuest(
                                        TEXT_RETRY_COUNT,
                                        remainingRecoveryAttempts - 1))),
                        ACTION_DELAY_MS);
            } else {
                retryUnacceptedMilitaryQuest("巡狩军团荒野");
            }
        });
    }

    static String extractMilitaryQuestName(String value) {
        String normalized = normalizeText(value);
        for (String questName : MILITARY_QUEST_NAMES) {
            if (normalized.contains(questName)) {
                return questName;
            }
        }
        return null;
    }

    static String extractWildernessZone(String value) {
        Matcher matcher = WILDERNESS_ZONE.matcher(normalizeText(value));
        return matcher.find() ? matcher.group(1) : null;
    }

    private String rightMilitaryDetails(OcrText text) {
        StringBuilder rightDetails = new StringBuilder();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds != null && bounds.centerX() > 640) {
                    rightDetails.append(line.getText()).append(' ');
                }
            }
        }
        return rightDetails.toString();
    }

    static boolean isMilitaryQuestDetail(String questName, String value) {
        return normalizeText(value).contains(normalizeText(questName));
    }

    static boolean isCooldownText(String value) {
        String normalized = normalizeText(value);
        return normalized.contains("冷却")
                && (normalized.contains("时间")
                        || normalized.contains("小时")
                        || normalized.contains("分")
                        || normalized.contains("秒"));
    }

    static Integer extractCooldownMinutes(String value) {
        Matcher matcher = MILITARY_COOLDOWN.matcher(normalizeText(value));
        if (!matcher.find() || matcher.group(1) == null
                && matcher.group(2) == null && matcher.group(3) == null) {
            return null;
        }
        // 武当的 findLegionInfoInterval 只按一个单位换算：小时直接换算，
        // 分钟额外加 1 分钟防止 OCR/闹钟提前，只有秒数时不安排下一轮。
        if (matcher.group(1) != null) {
            return Integer.parseInt(matcher.group(1)) * 60;
        }
        if (matcher.group(2) != null) {
            return Integer.parseInt(matcher.group(2)) + 1;
        }
        return null;
    }

    static boolean allExecutableMilitaryQuestsCooling(
            boolean supplyCooling, boolean wildernessCooling) {
        return supplyCooling && wildernessCooling;
    }

    private void finishMilitaryConversation(String questName) {
        host.showProgress("自动军务：左侧 NPC 窗口查找并点击“想”");
        Runnable leave = () -> host.tap(250, 635,
                () -> host.tap(316, 101,
                        () -> findAvailableMilitaryQuest(TEXT_RETRY_COUNT)));
        host.clickTextRegion("想", false,
                NPC_DIALOG_LEFT, NPC_DIALOG_TOP,
                NPC_DIALOG_RIGHT, NPC_DIALOG_BOTTOM, () -> {
            host.showProgress("自动军务：点击“离开”完成承接");
            leave.run();
        }, SCREEN_WAIT_RETRY_COUNT,
                () -> host.failAutomation("左侧 NPC 窗口未找到：想"));
    }

    private void retryUnacceptedMilitaryQuest(String questName) {
        if (!retriedUnacceptedQuests.add(questName)) {
            host.failAutomation("任务未进入进行中：" + questName);
            return;
        }
        host.showProgress("自动军务：" + questName + "尚未承接，重新承接");
        checkedMilitaryQuests.remove(questName);
        host.tap(316, 101, () -> findAvailableMilitaryQuest(TEXT_RETRY_COUNT));
    }

    private void openWildernessTraining() {
        SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, 0);
        int preferredZone = preferences.getInt(
                HelperAccessibilityService.PREF_TRAINING_WILDERNESS_ZONE, 1);
        int targetZone = militaryWildernessZone(militaryZone, preferredZone);
        if (targetZone == 0) {
            host.failAutomation("无法识别巡狩军团荒野区号：" + militaryZone);
            return;
        }
        WildernessCatalog.Enemy enemy =
                WildernessCatalog.enemyForTier(militaryZone, targetZone);
        host.showProgress("自动军务：巡狩荒野（" + militaryZone
                + "）对应荒野修炼" + targetZone + "区"
                + (enemy == null ? "" : "·" + enemy.level + " " + enemy.name));
        // 武当除了挑区，还会算出该打哪一级怪并先走到它的刷新段。
        Runnable afterZone = enemy == null
                ? this::clickMilitaryQuest
                : () -> wildernessNavigator.navigateToMonster(
                        enemy.name, this::clickMilitaryQuest);
        wildernessNavigator.navigateToZone(targetZone, afterZone);
    }

    static int militaryWildernessZone(String taskLevel, int preferredZone) {
        return WildernessCatalog.zoneForTier(taskLevel, preferredZone);
    }

    static int[] militaryWildernessZoneRange(String taskLevel) {
        WildernessCatalog.Tier tier = WildernessCatalog.tier(taskLevel);
        return tier == null ? null : new int[] {tier.zoneLow, tier.zoneHigh};
    }

    private void clickMilitaryQuest() {
        clickMilitaryQuest(2);
    }

    private void clickMilitaryQuest(int remainingClickAttempts) {
        host.showProgress("自动军务：点击右侧任务");
        if (militaryZone == null) {
            host.failAutomation("未读取到巡狩军团荒野区号");
            return;
        }
        clickMilitaryRightTaskText("巡狩军团荒野",
                () -> confirmMilitaryQuestStarted(
                        remainingClickAttempts, TASK_START_CONFIRM_RETRY_COUNT),
                SCREEN_WAIT_RETRY_COUNT,
                () -> host.failAutomation("荒野加载完成后未找到右侧巡狩任务"));
    }

    private void confirmMilitaryQuestStarted(
            int remainingClickAttempts, int remainingConfirmAttempts) {
        host.recognizeTextRegion(
                TASK_CONFIRM_LEFT, TASK_CONFIRM_TOP,
                TASK_CONFIRM_RIGHT, TASK_CONFIRM_BOTTOM, text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (hasTaskExecutionSignal(screenLines(text))) {
                host.showProgress("自动军务：已确认巡狩任务开始");
                host.closeAutoPathPanel(() -> scheduleMilitaryQuestCheck(
                        "巡狩军团荒野", this::recheckAvailableMilitaryQuests));
            } else if (remainingConfirmAttempts > 1) {
                host.postDelayed(
                        () -> confirmMilitaryQuestStarted(
                                remainingClickAttempts, remainingConfirmAttempts - 1),
                        300);
            } else if (remainingClickAttempts > 1) {
                host.showProgress("自动军务：未确认任务开始，重新点击");
                clickMilitaryQuest(remainingClickAttempts - 1);
            } else {
                host.failAutomation("点击巡狩任务后未出现“执行任务”");
            }
        });
    }

    static boolean isWildernessTrainingMapName(String value) {
        return WildernessNavigator.isWildernessMapName(value);
    }

    static boolean hasTaskExecutionSignal(List<String> values) {
        for (String value : values) {
            if (normalizeText(value).contains("执行任务")) {
                return true;
            }
        }
        return false;
    }

    private void finishMilitaryAndStartTraining() {
        host.returnHome(() -> {
            host.showProgress("自动军务已完成，10秒后自动练级");
            host.postDelayed(host::resumePrimaryTask, 10_000);
        });
    }


    private void scheduleMilitaryQuestCheck(String questName, Runnable completed) {
        int delay = progressCheckDelayMillis(questName);
        long checkAt = WorshipAlarmReceiver.scheduleMilitaryQuestCheck(
                host.context(), questName, delay);
        host.showProgress("自动军务：" + questName
                + "收集中，下次检查 " + host.formatTime(checkAt));
        host.postDelayed(
                () -> checkOngoingMilitaryQuest(questName, completed),
                delay);
    }

    static int progressCheckDelayMillis(String questName) {
        return "补充军团物资".equals(questName)
                ? SUPPLY_PROGRESS_CHECK_MS : WILDERNESS_PROGRESS_CHECK_MS;
    }

    private void checkOngoingMilitaryQuest(String questName, Runnable completed) {
        checkOngoingMilitaryQuest(questName, completed, 3);
    }

    private void checkOngoingMilitaryQuest(
            String questName, Runnable completed, int remainingRecoveryAttempts) {
        host.showProgress("自动军务：检查" + questName);
        Runnable inspect = () -> host.closeAutoPathPanel(
                () -> withTaskWindowOpen(
                        () -> host.tap(180, 101,
                                () -> clickMilitaryTaskListText(questName,
                                        () -> inspectOngoingMilitaryQuest(
                                                questName, completed,
                                                () -> continueCollecting(
                                                        questName, completed)),
                                        TEXT_RETRY_COUNT,
                                        () -> {
                                            if (remainingRecoveryAttempts > 1) {
                                                host.showProgress("自动军务：未找到" + questName
                                                        + "，重新打开任务窗口");
                                                host.postDelayed(
                                                        () -> checkOngoingMilitaryQuest(
                                                                questName, completed,
                                                                remainingRecoveryAttempts - 1),
                                                        ACTION_DELAY_MS);
                                            } else {
                                                host.failAutomation(
                                                        "连续重试后任务仍未在进行中：" + questName);
                                            }
                                        }))));
        if ("巡狩军团荒野".equals(questName)) {
            detectWildernessLocation(inspect);
        } else {
            inspect.run();
        }
    }

    private void detectWildernessLocation(Runnable next) {
        host.showProgress("自动军务：确认是否在荒野修炼区");
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            inWildernessTraining = isWildernessTrainingMapName(mapName);
            next.run();
        });
    }

    static boolean isWildernessTrainingLocation(List<String> values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if ((normalized.contains("荒野修炼")
                    || normalized.contains("荒野修练"))
                    && normalized.contains("区")) {
                return true;
            }
        }
        return false;
    }

    static boolean isWildernessLocation(List<String> values) {
        if (isWildernessTrainingLocation(values)) {
            return true;
        }
        for (String value : values) {
            if (normalizeText(value).contains("荒野营地")) {
                return true;
            }
        }
        return false;
    }

    private void inspectOngoingMilitaryQuest(
            String questName, Runnable completed, Runnable incomplete) {
        inspectOngoingMilitaryQuest(
                questName, completed, incomplete, TEXT_RETRY_COUNT);
    }

    private void inspectOngoingMilitaryQuest(
            String questName, Runnable completed, Runnable incomplete,
            int remainingAttempts) {
        recognizeMilitaryTaskDetail(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            Boolean complete = requiredItemsComplete(text);
            if (isMilitaryQuestComplete(complete, false)) {
                finishOngoingMilitaryQuest(questName, completed);
                return;
            }
            if (Boolean.FALSE.equals(complete)) {
                WorshipAlarmReceiver.scheduleMilitaryQuestCheck(
                        host.context(), questName, progressCheckDelayMillis(questName));
                host.showProgress("自动军务：" + questName + "材料未满");
                host.tap(982, 52, incomplete);
                return;
            }
            detectCompletedMilitaryQuestMark(markFound -> {
                if (isMilitaryQuestComplete(complete, markFound)) {
                    finishOngoingMilitaryQuest(questName, completed);
                } else if (remainingAttempts > 1) {
                    host.postDelayed(
                            () -> inspectOngoingMilitaryQuest(
                                    questName, completed, incomplete,
                                    remainingAttempts - 1),
                            ACTION_DELAY_MS);
                } else {
                    host.failAutomation("未识别到" + questName + "的材料进度或完成标记");
                }
            });
        });
    }

    static boolean isMilitaryQuestComplete(Boolean progressComplete, boolean markFound) {
        return Boolean.TRUE.equals(progressComplete)
                || progressComplete == null && markFound;
    }

    private void finishOngoingMilitaryQuest(String questName, Runnable completed) {
        if (requiresReturnHomeBeforeTurnIn(questName)) {
            finishWildernessQuest(questName, completed);
            return;
        }
        host.showProgress("自动军务：" + questName + "材料已满，快速抵达");
        Runnable afterArrival = () -> {
            host.showProgress("自动军务：点击自动寻路");
            host.clickTemplateOrText(WudangTemplateMatcher.Template.AUTO_PATH,
                    "自动寻路", false,
                    AUTO_PATH_LEFT, AUTO_PATH_TOP,
                    AUTO_PATH_RIGHT, AUTO_PATH_BOTTOM,
                    leaveCompletedQuest(questName, completed),
                    2, SCREEN_WAIT_RETRY_COUNT, null);
        };
        host.postDelayed(
                () -> host.clickQuickArrival(afterArrival),
                ACTION_DELAY_MS);
    }

    static boolean requiresReturnHomeBeforeTurnIn(String questName) {
        return "巡狩军团荒野".equals(questName);
    }

    private void finishWildernessQuest(String questName, Runnable completed) {
        host.showProgress("自动军务：荒野材料已满，回城交付");
        host.tap(982, 52,
                () -> host.returnHome(
                        () -> host.postDelayed(
                                () -> reopenWildernessQuest(questName, completed),
                                5_000)));
    }

    private void reopenWildernessQuest(String questName, Runnable completed) {
        host.showProgress("自动军务：回城后重新打开荒野任务");
        Runnable arrive = () -> {
            host.showProgress("自动军务：点击快速抵达");
            host.postDelayed(
                    () -> host.clickQuickArrival(
                            () -> finishWildernessArrival(questName, completed)),
                    ACTION_DELAY_MS);
        };
        host.closeAutoPathPanel(() -> withTaskWindowOpen(
                () -> host.tap(180, 101,
                        () -> clickMilitaryTaskListText(questName,
                                arrive,
                                TEXT_RETRY_COUNT,
                                () -> useSelectedCompletedQuest(
                                        questName, arrive)))));
    }

    private void useSelectedCompletedQuest(String questName, Runnable next) {
        recognizeMilitaryTaskDetail(text -> {
            List<String> rightValues = new ArrayList<>();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && bounds.centerX() > 640) {
                        rightValues.add(line.getText());
                    }
                }
            }
            if (isSelectedQuestComplete(
                    questName, rightValues, requiredItemsComplete(text))) {
                host.showProgress("自动军务：左侧任务 OCR 失败，使用已选中的完成任务");
                next.run();
            } else {
                host.failAutomation("回城后未找到进行中的荒野任务");
            }
        });
    }

    static boolean isSelectedQuestComplete(
            String questName, List<String> rightValues, Boolean itemsComplete) {
        for (String value : rightValues) {
            if (questName.equals(extractMilitaryQuestName(value))) {
                return Boolean.TRUE.equals(itemsComplete);
            }
        }
        return false;
    }

    private void finishWildernessArrival(String questName, Runnable completed) {
        Runnable waitForDialogue = leaveCompletedQuest(questName, completed);
        host.showProgress("自动军务：检测是否出现自动寻路");
        host.clickTemplateOrText(WudangTemplateMatcher.Template.AUTO_PATH,
                "自动寻路", false,
                AUTO_PATH_LEFT, AUTO_PATH_TOP,
                AUTO_PATH_RIGHT, AUTO_PATH_BOTTOM,
                () -> {
                    host.showProgress("自动军务：已点击自动寻路，等待对话");
                    waitForDialogue.run();
                },
                2, TEXT_RETRY_COUNT,
                () -> {
                    host.showProgress("自动军务：未出现自动寻路，直接等待对话");
                    waitForDialogue.run();
                });
    }

    private Runnable leaveCompletedQuest(String questName, Runnable completed) {
        return () -> host.tap(250, 635,
                () -> host.postDelayed(
                        () -> refreshCompletedQuestCooldown(questName, completed),
                        ACTION_DELAY_MS));
    }

    private void refreshCompletedQuestCooldown(String questName, Runnable completed) {
        host.showProgress("自动军务：重新读取" + questName + "冷却");
        withTaskWindowOpen(() -> host.tap(55, 369,
                () -> host.tap(316, 101,
                        () -> clickMilitaryTaskListText(questName,
                                () -> inspectCompletedQuestCooldown(questName, completed),
                                TEXT_RETRY_COUNT,
                                () -> useCompletedQuestFallback(questName, completed)))));
    }

    private void inspectCompletedQuestCooldown(String questName, Runnable completed) {
        recognizeMilitaryTaskDetail(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            String rightDetails = rightMilitaryDetails(text);
            String cooldown = isMilitaryQuestDetail(questName, rightDetails)
                    && isCooldownText(rightDetails) ? rightDetails : null;
            Integer cooldownMinutes = cooldown == null
                    ? null : extractCooldownMinutes(cooldown);
            if (cooldownMinutes == null) {
                useCompletedQuestFallback(questName, completed);
                return;
            }
            long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitaryAfterCooldown(
                    host.context(), questName, cooldownMinutes);
            host.showProgress("自动军务：" + questName
                    + "下次 " + host.formatTime(nextMilitaryAt));
            completed.run();
        });
    }

    private void useCompletedQuestFallback(String questName, Runnable completed) {
        long nextMilitaryAt = WorshipAlarmReceiver.markMilitaryQuestHandled(
                host.context(), questName);
        host.showProgress("自动军务：未读取到" + questName
                + "冷却，2小时后复查 " + host.formatTime(nextMilitaryAt));
        completed.run();
    }

    private Boolean requiredItemsComplete(OcrText text) {
        StringBuilder value = new StringBuilder();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds != null && isRequiredItemDetailPosition(
                        bounds.centerX(), bounds.centerY())) {
                    value.append(line.getText()).append(' ');
                }
            }
        }
        return requiredItemComplete(value.toString());
    }

    static boolean isRequiredItemDetailPosition(int x, int y) {
        return x > 640 && x < 960 && y > 150 && y < 400;
    }

    static Boolean requiredItemComplete(String value) {
        Matcher matcher = REQUIRED_ITEM_PROGRESS.matcher(normalizeText(value));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1)) >= Integer.parseInt(matcher.group(2));
    }

    private void detectCompletedMilitaryQuestMark(Consumer<Boolean> result) {
        host.captureScreenshot(bitmap -> {
            try {
                result.accept(bitmap != null && hasCompletedMilitaryQuestMark(bitmap));
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        });
    }

    private boolean hasCompletedMilitaryQuestMark(Bitmap bitmap) {
        int green = 0;
        int left = 450 * bitmap.getWidth() / 1280;
        int right = 520 * bitmap.getWidth() / 1280;
        int top = 130 * bitmap.getHeight() / 720;
        int bottom = 450 * bitmap.getHeight() / 720;
        for (int y = top; y < bottom; y += 2) {
            for (int x = left; x < right; x += 2) {
                int color = bitmap.getPixel(x, y);
                if (isCompletionGreen(
                        Color.red(color), Color.green(color), Color.blue(color))) {
                    green++;
                }
            }
        }
        return green >= 40;
    }

    static boolean isCompletionGreen(int red, int green, int blue) {
        return green > 140 && green > red * 1.4f && green > blue * 1.2f;
    }

    private static String normalizeText(String value) {
        return value.replaceAll("\\s+", "")
                .replace('卻', '却')
                .replace('時', '时')
                .replace('間', '间')
                .replace('(', '（')
                .replace(')', '）');
    }

}
