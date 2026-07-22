package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BossAutomation {
    private static final Pattern MAP_COORDINATE =
            Pattern.compile("(\\d{1,3})(?:\\s*[,，.。·:：/]\\s*|\\s+)(\\d{1,2})");
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
    private static final int PARTY_MANAGE_X = 190;
    private static final int PARTY_MANAGE_Y = 279;
    private static final int PARTY_DIALOG_CLOSE_X = 1260;
    private static final int PARTY_DIALOG_CLOSE_Y = 150;

    private final AutomationHost host;
    private final ChannelSwitcher channelSwitcher;
    private final AntiCheatVerification antiCheatVerification;
    private TeamFollowerVision teamFollowerVision;
    private List<Integer> route = new ArrayList<>();
    private int routeIndex;
    private boolean followLeader;
    private long nextLeaderCheckAt;

    BossAutomation(AutomationHost host) {
        this.host = host;
        channelSwitcher = new ChannelSwitcher(host);
        antiCheatVerification = new AntiCheatVerification(host);
    }

    void start() {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        followLeader = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(
                        HelperAccessibilityService.PREF_BOSS_FOLLOW_LEADER, false);
        nextLeaderCheckAt = 0;
        host.startPrimaryAutomation(
                AutomationHost.PrimaryTask.BOSS, "野王：打开游戏", this::prepareBoss);
    }

    private void prepareBoss() {
        host.checkInventoryBeforePrimary(this::prepareBossAfterInventoryCheck);
    }

    private void prepareBossAfterInventoryCheck() {
        host.ensureGameHudVisible(this::prepareModeOne);
    }

    private void prepareModeOne() {
        host.showProgress("野王：打开菜单");
        host.tap(1215, 58, () -> {
            host.showProgress("野王：打开自动设置");
            host.tap(1190, 360, () -> {
                host.showProgress("野王：选择模式 1");
                host.tap(1025, 75, () -> host.tap(1240, 45, this::useMarker));
            });
        });
    }

    private void useMarker() {
        host.showProgress("野王：停止自动攻击后前往标记点");
        host.useFirstMarker(() -> host.postDelayed(
                () -> host.waitForMapReady(20, this::openEnemyPanel), MAP_LOAD_MS));
    }

    private void openEnemyPanel() {
        host.showProgress("野王：打开自动寻敌");
        host.openAutoPathPanel(
                () -> host.tap(1015, 165, this::readCurrentPosition));
    }

    private void readCurrentPosition() {
        host.showProgress("野王：读取当前位置");
        host.recognizeMapCoordinate(value -> {
            Integer currentX = parseMapX(value);
            if (currentX == null) {
                host.showProgress("野王：未读取到坐标，重新检测");
                host.postDelayed(this::readCurrentPosition, 1_000);
                return;
            }
            startRoute(currentX);
        });
    }

    private void startRoute(int currentX) {
        route = buildRoute(currentX);
        routeIndex = 0;
        moveNext();
    }

    private void moveNext() {
        maybeFollowLeader(this::moveNextAfterLeaderCheck);
    }

    private void moveNextAfterLeaderCheck() {
        if (!host.isAutomationRunning()) {
            return;
        }
        if (routeIndex >= route.size()) {
            switchChannel();
            return;
        }
        int x = route.get(routeIndex);
        host.showProgress("野王：搜索坐标 " + x + "," + MAP_Y);
        long deadline = System.currentTimeMillis() + MOVE_DURATION_MS;
        host.tapMapCoordinateFast(x, MAP_Y, () -> scanDuringMove(deadline));
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
        host.showProgress("野王：攻击 " + target.name);
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
                    host.showProgress("野王：已击败 " + currentBoss);
                    moveNext();
                } else {
                    host.showProgress("野王：确认击败 " + currentBoss
                            + "（" + misses + "/" + BOSS_DEFEAT_CONFIRMATIONS + "）");
                    host.postDelayed(
                            () -> waitForBossDefeated(currentBoss, misses), BOSS_CHECK_MS);
                }
            } else if (!target.name.equals(currentBoss)) {
                attack(target);
            } else {
                host.showProgress("野王：战斗中 " + currentBoss);
                host.postDelayed(
                        () -> waitForBossDefeated(currentBoss, 0), BOSS_CHECK_MS);
            }
        });
    }

    static boolean isBossDefeatConfirmed(int consecutiveMisses) {
        return consecutiveMisses >= BOSS_DEFEAT_CONFIRMATIONS;
    }

    private void findRedBoss(java.util.function.Consumer<BossTarget> result) {
        host.recognizeRedBoss(result);
    }

    private void switchChannel() {
        host.showProgress("野王：打开分流信息");
        host.tap(1215, 705,
                () -> host.postDelayed(this::readCurrentChannel, CHANNEL_DIALOG_MS));
    }

    private void readCurrentChannel() {
        host.showProgress("野王：判断当前分流");
        host.recognizeText(text -> host.captureScreenshot(bitmap -> {
            Integer current = bitmap == null ? null : findCurrentChannel(text, bitmap);
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (current == null) {
                host.showProgress("野王：未读取到当前分流，重新检测");
                host.postDelayed(this::readCurrentChannel, 1_000);
                return;
            }
            int next = ChannelSwitcher.nextChannel(current);
            host.showProgress("野王：第 " + current + " 分流 → 第 " + next + " 分流");
            channelSwitcher.switchOpenTo(next,
                    () -> host.postDelayed(
                            () -> checkAfterChannelSwitch(this::openEnemyPanel),
                            CHANNEL_CHECK_MS));
        }));
    }

    private void checkAfterChannelSwitch(Runnable next) {
        antiCheatVerification.checkThen(() -> host.postDelayed(
                () -> antiCheatVerification.checkThen(next),
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
                host.showProgress("野王跟随队长：队长头像变灰，检查分流");
                followLeaderChannel(bitmap, this::openEnemyPanel);
            }
        });
    }

    private TeamFollowerVision teamFollowerVision() {
        if (teamFollowerVision == null) {
            teamFollowerVision = new TeamFollowerVision(host.context().getAssets());
        }
        return teamFollowerVision;
    }

    private void followLeaderChannel(Bitmap screenshot, Runnable next) {
        readCurrentHudChannel(screenshot, PARTY_OCR_ATTEMPTS, current -> {
            host.showProgress("野王跟随队长：当前第 " + current + " 分流");
            host.tap(PARTY_MANAGE_X, PARTY_MANAGE_Y,
                    () -> host.postDelayed(
                            () -> readLeaderChannel(PARTY_OCR_ATTEMPTS, current, next),
                            CHANNEL_DIALOG_MS));
        });
    }

    private void readCurrentHudChannel(Bitmap screenshot, int remainingAttempts,
            java.util.function.Consumer<Integer> next) {
        host.showProgress("野王跟随队长：读取当前分流");
        host.recognizeHudChannel(screenshot, channel -> {
            if (channel != null) {
                DiagnosticLog.info("BOSS", "leader follow current channel=" + channel);
                next.accept(channel);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> captureCurrentHudChannel(
                        remainingAttempts - 1, next), 1_000);
            } else {
                DiagnosticLog.warn("BOSS", "OCR miss: 右下角当前分流数字");
                host.failAutomation("野王跟随队长：未识别到右下角当前分流");
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
                host.failAutomation("野王跟随队长：无法截图读取当前分流");
            }
        });
    }

    private void readLeaderChannel(int remainingAttempts, int current, Runnable next) {
        host.showProgress("野王跟随队长：读取队长分流");
        host.recognizeText(text -> {
            Integer leader = findLeaderChannel(text);
            boolean dialogOpen = hasPartyManageDialog(text);
            if (leader != null) {
                DiagnosticLog.info("BOSS", "leader follow channels current=" + current
                        + " leader=" + leader + " same=" + (current == leader));
                closePartyDialog(() -> handleLeaderChannel(current, leader, next));
            } else if (remainingAttempts > 1) {
                if (dialogOpen) {
                    host.postDelayed(() -> readLeaderChannel(
                            remainingAttempts - 1, current, next), 1_000);
                } else {
                    DiagnosticLog.warn("BOSS", "party dialog is not open; tapping Manage again");
                    host.showProgress("野王跟随队长：重新打开队伍管理");
                    host.tap(PARTY_MANAGE_X, PARTY_MANAGE_Y,
                            () -> host.postDelayed(
                                    () -> readLeaderChannel(
                                            remainingAttempts - 1, current, next),
                                    CHANNEL_DIALOG_MS));
                }
            } else {
                if (dialogOpen) {
                    closePartyDialog(() -> failPartyOcr("队长分流", text));
                } else {
                    failPartyOcr("队长分流", text);
                }
            }
        });
    }

    private static boolean hasPartyManageDialog(Text text) {
        return isPartyManageDialogText(text.getText());
    }

    static boolean isPartyManageDialogText(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", "");
        return normalized.contains("点击此处增加队伍成员")
                || normalized.contains("所在地")
                || (normalized.contains("名称") && normalized.contains("职业"));
    }

    private void closePartyDialog(Runnable next) {
        host.showProgress("野王跟随队长：关闭队伍管理");
        DiagnosticLog.info("BOSS", "closing party dialog at "
                + PARTY_DIALOG_CLOSE_X + "," + PARTY_DIALOG_CLOSE_Y);
        host.tap(PARTY_DIALOG_CLOSE_X, PARTY_DIALOG_CLOSE_Y, () -> {
            DiagnosticLog.info("BOSS", "party dialog close tap completed");
            next.run();
        });
    }

    private void handleLeaderChannel(int current, int leader, Runnable next) {
        host.showProgress("野王跟随队长：队长在第 " + leader + " 分流");
        if (current == leader) {
            DiagnosticLog.info("BOSS", "leader follow decision=stay channel=" + current);
            nextLeaderCheckAt = System.currentTimeMillis() + LEADER_CHECK_MS;
            next.run();
            return;
        }
        DiagnosticLog.info("BOSS", "leader follow decision=switch from="
                + current + " to=" + leader);
        host.showProgress("野王跟随队长：第 " + current
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

    private void failPartyOcr(String target, Text text) {
        DiagnosticLog.warn("BOSS", "OCR miss: " + target + "; lines=" + ocrLines(text));
        host.failAutomation("野王跟随队长：未识别到" + target);
    }

    private static String ocrLines(Text text) {
        List<String> values = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                values.add(line.getText() + "@" + bounds);
            }
        }
        return values.toString();
    }

    private static Integer findLeaderChannel(Text text) {
        Integer leader = null;
        int firstRow = Integer.MAX_VALUE;
        List<String> candidates = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
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
        DiagnosticLog.info("BOSS", "leader channel OCR candidates="
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

    private static Integer findCurrentChannel(Text text, Bitmap bitmap) {
        Integer current = null;
        int bestScore = 19;
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
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
        int current = Math.max(0, Math.min(currentX, AutomationHost.MAP_GAME_MAX_X));
        int direction = current < 300 ? 1 : -1;
        int firstTarget = Math.max(MAP_LEFT,
                Math.min(current + direction * 50, MAP_RIGHT));
        int firstEdge = direction > 0 ? MAP_RIGHT : MAP_LEFT;
        int secondEdge = direction > 0 ? MAP_LEFT : MAP_RIGHT;
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
            Text text, Bitmap bitmap, int screenLeft, int ocrScale) {
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    Rect ocrBounds = element.getBoundingBox();
                    String name = element.getText().replaceAll("\\s+", "");
                    if (ocrBounds != null && containsChinese(name)) {
                        Rect bounds = toOriginalBounds(ocrBounds, ocrScale,
                                bitmap.getWidth(), bitmap.getHeight());
                        if (hasRedText(bitmap, bounds)) {
                            bounds.offset(screenLeft, 0);
                            return new BossTarget(name, bounds);
                        }
                    }
                }
            }
        }
        return null;
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
