package com.local.sgmhelper;

import android.annotation.SuppressLint;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HelperAccessibilityService extends AccessibilityService {
    private static final String TAG = "SgmHelper";
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_STOPPED = 3;
    private static final int STATE_COMPLETED = 4;
    private static final int ACTION_DELAY_MS = 1000;
    private static final int CLICK_DELAY_MS = 2000;
    private static final int MILITARY_PROGRESS_CHECK_MS = 5 * 60 * 1000;
    private static final int TEXT_RETRY_COUNT = 5;
    private static final int SCREEN_WAIT_RETRY_COUNT = 20;
    static final String PREFS_NAME = "schedule";
    static final String PREF_HOUR = "hour";
    static final String PREF_MINUTE = "minute";
    static final String PREF_LEGION_HOUR = "legion_hour";
    static final String PREF_LEGION_MINUTE = "legion_minute";
    static final String PREF_MILITARY_HOUR = "military_hour";
    static final String PREF_MILITARY_MINUTE = "military_minute";
    static final String PREF_MILITARY_RETRY_AT = "military_retry_at";
    static final String PREF_SUPPLY_RETRY_AT = "supply_retry_at";
    static final String PREF_WILDERNESS_RETRY_AT = "wilderness_retry_at";
    private static final Pattern WILDERNESS_ZONE = Pattern.compile(
            "巡狩军团荒野[（(]([一二三四五六七八九十百0-9]+)[）)]");
    private static final Pattern MILITARY_COOLDOWN = Pattern.compile(
            "冷却时间[：:]?(?:(\\d+)小时)?(?:(\\d+)分(?:钟)?)?(?:(\\d+)秒)?");
    private static final Pattern REQUIRED_ITEM_PROGRESS = Pattern.compile(
            "(\\d+)[/／](\\d+)");
    private static final String[] MILITARY_QUEST_NAMES = {
            "补充军团物资", "巡狩军团荒野", "勇讨军团天将"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ponytail: one in-process service reference keeps the self-test small; clear it on every teardown.
    @SuppressLint("StaticFieldLeak")
    private static volatile HelperAccessibilityService instance;

    private WindowManager windowManager;
    private Button bubbleView;
    private View menuView;
    private TextView stateView;
    private TextView progressView;
    private TextView nextMilitaryView;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams menuParams;
    private int taskState = STATE_IDLE;
    private boolean automationRunning;
    private TextRecognizer textRecognizer;
    private String militaryZone;
    private boolean supplyQuestCooling;
    private boolean wildernessQuestCooling;
    private final Set<String> checkedMilitaryQuests = new HashSet<>();
    private final Set<String> retriedUnacceptedQuests = new HashSet<>();

    static HelperAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        showOverlay();
        showNextMilitaryTime(WorshipAlarmReceiver.scheduleAll(this));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The MVP does not inspect accessibility events or screen content.
    }

    @Override
    public void onInterrupt() {
        stopAutomation(STATE_PAUSED);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanUp();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanUp();
        super.onDestroy();
    }

    boolean tap(float x, float y, GestureResultCallback callback) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 60))
                .build();

        return dispatchGesture(gesture, callback, handler);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showOverlay() {
        if (bubbleView != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            Log.e(TAG, "WindowManager is unavailable");
            return;
        }

        Button bubble = new Button(this);
        bubble.setAllCaps(false);
        bubble.setText("辅");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(14);
        bubble.setContentDescription(getString(R.string.open_floating_menu));
        bubble.setMinWidth(0);
        bubble.setMinHeight(0);
        bubble.setPadding(0, 0, 0, 0);
        bubble.setElevation(dp(8));
        bubble.setAlpha(0.7f);
        bubble.setBackground(bubbleBackground(Color.rgb(38, 92, 191)));
        bubble.setOnClickListener(view -> toggleMenu());

        int size = dp(48);
        bubbleParams = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 0;
        bubbleParams.y = getResources().getDisplayMetrics().heightPixels - size;

        int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private int startX;
            private int startY;
            private boolean dragging;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                            dragging = true;
                        }
                        if (dragging) {
                            moveBubble(startX + Math.round(dx), startY + Math.round(dy));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            view.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        });

        try {
            windowManager.addView(bubble, bubbleParams);
            bubbleView = bubble;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to add accessibility overlay", error);
        }
    }

    private void moveBubble(int x, int y) {
        if (windowManager == null || bubbleView == null) {
            return;
        }

        int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - bubbleParams.width);
        int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - bubbleParams.height);
        bubbleParams.x = Math.max(0, Math.min(x, maxX));
        bubbleParams.y = Math.max(0, Math.min(y, maxY));
        windowManager.updateViewLayout(bubbleView, bubbleParams);
        updateMenuPosition();
    }

    private void toggleMenu() {
        if (menuView == null) {
            showMenu();
        } else {
            closeMenu();
        }
    }

    private void showMenu() {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(10), dp(10), dp(10), dp(10));
        menu.setElevation(dp(10));
        menu.setAlpha(0.7f);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(250, 250, 250));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(210, 214, 220));
        menu.setBackground(background);

        populateMainMenu(menu);

        menuParams = new WindowManager.LayoutParams(
                dp(230),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;
        positionMenu();

        try {
            windowManager.addView(menu, menuParams);
            menuView = menu;
            updateStateView();
        } catch (RuntimeException error) {
            stateView = null;
            Log.e(TAG, "Unable to add floating menu", error);
        }
    }

    private void populateMainMenu(LinearLayout menu) {
        menu.removeAllViews();
        stateView = addMenuText(menu, R.string.menu_status_idle);
        addMenuButton(menu, R.string.menu_start, view -> startGame());
        addMenuButton(menu, R.string.menu_pause, view -> stopAutomation(STATE_PAUSED));
        addMenuButton(menu, R.string.menu_stop, view -> stopAutomation(STATE_STOPPED));
        addMenuButton(menu, R.string.menu_settings, view -> showSettingsMenu());
        addMenuButton(menu, R.string.menu_exit, view -> exitService());
        addMenuText(menu, "版本 " + BuildConfig.VERSION_NAME);
        updateStateView();
    }

    private TextView addMenuText(LinearLayout menu, int textResource) {
        return addMenuText(menu, getString(textResource));
    }

    private TextView addMenuText(LinearLayout menu, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.rgb(31, 35, 40));
        text.setTextSize(14);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(6), dp(6), dp(6), dp(8));
        menu.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return text;
    }

    private Button addMenuButton(LinearLayout menu, int textResource, View.OnClickListener listener) {
        Button button = createMenuButton(textResource, listener);
        menu.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)));
        return button;
    }

    private Button createMenuButton(int textResource, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(textResource);
        button.setTextSize(15);
        button.setSingleLine(true);
        button.setMinWidth(0);
        button.setMinHeight(dp(42));
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setOnClickListener(listener);
        return button;
    }

    private void setTaskState(int state) {
        taskState = state;
        updateStateView();

        int color = Color.rgb(38, 92, 191);
        if (state == STATE_RUNNING) {
            color = Color.rgb(30, 142, 74);
        } else if (state == STATE_PAUSED) {
            color = Color.rgb(191, 124, 18);
        }
        if (bubbleView != null) {
            bubbleView.setBackground(bubbleBackground(color));
        }
    }

    private void startGame() {
        if (taskState == STATE_RUNNING) {
            closeMenu();
            return;
        }
        showProgress("初始化：读取下次军务时间");
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long supplyRetryAt = preferences.getLong(PREF_SUPPLY_RETRY_AT, 0);
        long wildernessRetryAt = preferences.getLong(PREF_WILDERNESS_RETRY_AT, 0);
        if (shouldCheckMilitaryNow(
                System.currentTimeMillis(), supplyRetryAt, wildernessRetryAt)) {
            startScheduledMilitary();
        } else {
            long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitary(this);
            startScheduledAutomation(
                    "初始化：下次军务 " + formatTime(nextMilitaryAt),
                    this::startTraining);
        }
    }

    static boolean shouldCheckMilitaryNow(
            long now, long supplyRetryAt, long wildernessRetryAt) {
        return supplyRetryAt <= now || wildernessRetryAt <= now;
    }

    private void updateStateView() {
        if (stateView == null) {
            return;
        }

        int textResource = R.string.menu_status_idle;
        if (taskState == STATE_RUNNING) {
            textResource = R.string.menu_status_running;
        } else if (taskState == STATE_PAUSED) {
            textResource = R.string.menu_status_paused;
        } else if (taskState == STATE_STOPPED) {
            textResource = R.string.menu_status_stopped;
        } else if (taskState == STATE_COMPLETED) {
            textResource = R.string.menu_status_completed;
        }
        stateView.setText(textResource);
    }

    private void showSettingsMenu() {
        if (!(menuView instanceof LinearLayout)) {
            return;
        }
        LinearLayout menu = (LinearLayout) menuView;
        menu.removeAllViews();
        stateView = null;
        addMenuText(menu, R.string.menu_settings);
        addMenuButton(menu, R.string.settings_training,
                view -> showSettingsPage(R.string.settings_training));
        addMenuButton(menu, R.string.settings_timer,
                view -> showSettingsPage(R.string.settings_timer));
        addMenuButton(menu, R.string.settings_boss,
                view -> showSettingsPage(R.string.settings_boss));
        addMenuButton(menu, R.string.settings_back, view -> populateMainMenu(menu));
    }

    private void showSettingsPage(int titleResource) {
        if (!(menuView instanceof LinearLayout)) {
            return;
        }
        LinearLayout menu = (LinearLayout) menuView;
        menu.removeAllViews();
        if (titleResource == R.string.settings_timer) {
            showTimerPage(menu);
            return;
        }
        addMenuText(menu, titleResource);
        addMenuText(menu, R.string.settings_placeholder);
        addMenuButton(menu, R.string.settings_back, view -> showSettingsMenu());
        updateMenuPosition();
    }

    private void showTimerPage(LinearLayout menu) {
        addMenuText(menu, R.string.settings_timer);
        addTimerRow(menu, R.string.timer_worship, PREF_HOUR, PREF_MINUTE, 10, 0,
                () -> WorshipAlarmReceiver.scheduleWorship(this), this::startScheduledWorship);
        addTimerRow(menu, R.string.timer_military,
                PREF_MILITARY_HOUR, PREF_MILITARY_MINUTE, 10, 30,
                () -> WorshipAlarmReceiver.scheduleMilitary(this),
                this::startScheduledMilitary);
        addTimerRow(menu, R.string.timer_reward, null, null, 0, 0, null, null);
        addTimerRow(menu, R.string.timer_legion_reward,
                PREF_LEGION_HOUR, PREF_LEGION_MINUTE, 10, 10,
                () -> WorshipAlarmReceiver.scheduleLegionReward(this),
                this::startScheduledLegionReward);
        addMenuButton(menu, R.string.settings_back, view -> showSettingsMenu());
        updateMenuPosition();
    }

    private void addTimerRow(LinearLayout menu, int titleResource,
            String hourKey, String minuteKey, int defaultHour, int defaultMinute,
            Runnable reschedule, Runnable runNow) {
        LinearLayout row = new LinearLayout(this);
        Button timer = createMenuButton(titleResource, null);
        timer.setTextSize(13);
        Button execute = createMenuButton(R.string.timer_run_now,
                runNow == null ? null : view -> runNow.run());
        execute.setTextSize(13);

        if (hourKey == null) {
            timer.setEnabled(false);
            execute.setEnabled(false);
        } else {
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            setTimerButtonText(timer, titleResource,
                    preferences.getInt(hourKey, defaultHour),
                    preferences.getInt(minuteKey, defaultMinute));
            timer.setOnClickListener(view -> showClock(timer, titleResource,
                    hourKey, minuteKey, defaultHour, defaultMinute, reschedule));
        }

        row.addView(timer, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(execute, new LinearLayout.LayoutParams(dp(72), dp(44)));
        menu.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
    }

    private void setTimerButtonText(TextView view, int titleResource, int hour, int minute) {
        view.setText(getString(R.string.timer_task_time,
                getString(titleResource), hour, minute));
    }

    private void showClock(TextView timeView, int titleResource,
            String hourKey, String minuteKey, int defaultHour, int defaultMinute,
            Runnable reschedule) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int hour = preferences.getInt(hourKey, defaultHour);
        int minute = preferences.getInt(minuteKey, defaultMinute);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            preferences.edit()
                    .putInt(hourKey, selectedHour)
                    .putInt(minuteKey, selectedMinute)
                    .apply();
            setTimerButtonText(timeView, titleResource, selectedHour, selectedMinute);
            reschedule.run();
        }, hour, minute, true);
        dialog.setTitle(titleResource);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
            dialog.getWindow().getDecorView().setAlpha(0.7f);
        }
        dialog.show();
    }

    void startScheduledWorship() {
        startScheduledAutomation("膜拜：打开游戏",
                () -> performTap(1215, 58, this::openRanking));
    }

    void startScheduledLegionReward() {
        startScheduledAutomation("军团奖励：打开游戏",
                () -> performTap(1215, 58, this::openLegionReward));
    }

    void startScheduledMilitary() {
        militaryZone = null;
        supplyQuestCooling = false;
        wildernessQuestCooling = false;
        checkedMilitaryQuests.clear();
        retriedUnacceptedQuests.clear();
        showNextMilitaryTime(WorshipAlarmReceiver.scheduleMilitary(this));
        startScheduledAutomation("自动军务：回城",
                () -> returnHome(() -> {
                    showProgress("自动军务：停止自动攻击");
                    ensureAutoAttackDisabled(
                            () -> handler.postDelayed(this::openMilitaryTask, 9_000));
                }));
    }

    private void startScheduledAutomation(String progress, Runnable firstAction) {
        if (!prepareWorshipAutomation()) {
            return;
        }
        showProgress(progress);

        Intent intent = getPackageManager().getLaunchIntentForPackage("hk.phx.khm.cs");
        if (intent == null) {
            failAutomation("Game package is unavailable");
            return;
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            handler.postDelayed(firstAction, 10_000);
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to open game for scheduled task", error);
            failAutomation("Unable to open game");
        }
    }

    private boolean prepareWorshipAutomation() {
        if (automationRunning) {
            return false;
        }

        automationRunning = true;
        setTaskState(STATE_RUNNING);
        closeMenu();
        return true;
    }

    private void openRanking() {
        showProgress("膜拜：查找排行榜");
        detectRanking(rankingVisible -> {
            if (!automationRunning) {
                return;
            }
            if (rankingVisible) {
                performTap(1190, 230, this::openGloryHall);
            } else {
                performSwipe(1120, 250, 1120, 350,
                        () -> performTap(1190, 330, this::openGloryHall));
            }
        });
    }

    private void openGloryHall() {
        showProgress("膜拜：进入荣耀厅");
        performTap(852, 358,
                () -> {
                    showProgress("膜拜：大丰收城");
                    performTap(165, 196,
                        () -> performTap(636, 623,
                                () -> {
                                    showProgress("膜拜：国战榜");
                                    performTap(167, 351,
                                        () -> performTap(636, 623,
                                                () -> performTap(1233, 55,
                                                        this::completeAutomation)));
                                }));
                });
    }

    private void openLegionReward() {
        showProgress("军团奖励：领取俸禄");
        performTap(994, 245,
                () -> performTap(409, 101,
                        () -> performTap(978, 178,
                                () -> performTap(531, 406,
                                        () -> performTap(950, 612,
                                                this::makeGeneralContributions)))));
    }

    private void makeGeneralContributions() {
        showProgress("军团奖励：进行贡献");
        performTap(380, 569,
                () -> performTap(380, 569,
                        () -> performTap(380, 569,
                                () -> performTap(380, 569,
                                        () -> performTap(380, 569,
                                                () -> performTap(380, 569,
                                                        () -> performTap(1106, 49,
                                                                () -> performTap(1106, 49,
                                                                        this::completeAutomation))))))));
    }

    private void openMilitaryTask() {
        showProgress("自动军务：检查自动寻路面板");
        closeAutoPathPanelIfNeeded(() -> {
            showProgress("自动军务：打开可承接任务");
            performTap(1255, 285,
                    () -> performTap(55, 369,
                            () -> performTap(316, 101, this::selectMilitaryQuest)));
        });
    }

    private void selectMilitaryQuest() {
        showProgress("自动军务：承接军团任务");
        findAvailableMilitaryQuest(TEXT_RETRY_COUNT);
    }

    private void findAvailableMilitaryQuest(int remainingAttempts) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    String questName = extractMilitaryQuestName(line.getText());
                    if (questName != null && bounds != null && bounds.centerX() < 640
                            && checkedMilitaryQuests.add(questName)) {
                        String zone = extractWildernessZone(line.getText());
                        if (zone != null) {
                            militaryZone = zone;
                        }
                        showProgress("自动军务：检查" + questName);
                        performTap(bounds.centerX(), bounds.centerY(),
                                () -> inspectAvailableMilitaryQuest(
                                        questName, TEXT_RETRY_COUNT));
                        return;
                    }
                }
            }
            if (checkedMilitaryQuests.size() < MILITARY_QUEST_NAMES.length
                    && remainingAttempts > 1) {
                handler.postDelayed(
                        () -> findAvailableMilitaryQuest(remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else if (allExecutableMilitaryQuestsCooling(
                    supplyQuestCooling, wildernessQuestCooling)) {
                showProgress("自动军务：无可执行任务，准备练级");
                performTap(982, 52, this::finishMilitaryAndStartTraining);
            } else {
                showProgress("自动军务：立即检查补充军团物资");
                performTap(180, 101,
                        () -> clickLeftScreenText("补充军团物资",
                                () -> inspectOngoingMilitaryQuest(
                                        "补充军团物资",
                                        this::continueToWilderness,
                                        this::startSupplyQuest),
                                TEXT_RETRY_COUNT,
                                () -> {
                                    if (supplyQuestCooling) {
                                        performTap(982, 52, this::continueToWilderness);
                                    } else {
                                        retryUnacceptedMilitaryQuest("补充军团物资");
                                    }
                                }));
            }
        });
    }

    private void inspectAvailableMilitaryQuest(String questName, int remainingAttempts) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            String status = findMilitaryAvailabilityText(text);
            if (status == null) {
                if (remainingAttempts > 1) {
                    handler.postDelayed(
                            () -> inspectAvailableMilitaryQuest(
                                    questName, remainingAttempts - 1),
                            ACTION_DELAY_MS);
                } else {
                    failAutomation("无法确认任务是否可承接：" + questName);
                }
                return;
            }
            if (isCooldownText(status)) {
                if ("补充军团物资".equals(questName)) {
                    supplyQuestCooling = true;
                } else if ("巡狩军团荒野".equals(questName)) {
                    wildernessQuestCooling = true;
                }
                Integer cooldownMinutes = extractCooldownMinutes(status);
                if (cooldownMinutes != null && expectedMilitaryNpc(questName) != null) {
                    long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitaryAfterCooldown(
                            this, questName, cooldownMinutes);
                    showNextMilitaryTime(WorshipAlarmReceiver.scheduleMilitary(this));
                    showProgress("自动军务：跳过" + questName
                            + "，下次 " + formatTime(nextMilitaryAt));
                } else {
                    Log.e(TAG, "Unable to parse cooldown: " + status);
                    showProgress("自动军务：跳过冷却中的" + questName);
                }
                findAvailableMilitaryQuest(TEXT_RETRY_COUNT);
            } else {
                acceptMilitaryQuest(questName);
            }
        });
    }

    private void startSupplyQuest() {
        showProgress("自动军务：开始补充军团物资");
        clickRightScreenText("补充军团物资",
                () -> scheduleMilitaryQuestCheck(
                        "补充军团物资", this::continueToWilderness),
                TEXT_RETRY_COUNT,
                this::continueToWilderness);
    }

    private void continueToWilderness() {
        if (wildernessQuestCooling) {
            finishMilitaryAndStartTraining();
            return;
        }
        showProgress("自动军务：立即检查巡狩军团荒野");
        Runnable inspect = () -> {
            if (militaryZone == null) {
                findOngoingWildernessQuest(TEXT_RETRY_COUNT);
            } else {
                clickLeftScreenText("巡狩军团荒野",
                        () -> inspectOngoingMilitaryQuest(
                                "巡狩军团荒野",
                                this::finishMilitaryAndStartTraining,
                                this::openWildernessTraining),
                        TEXT_RETRY_COUNT,
                        () -> retryUnacceptedMilitaryQuest("巡狩军团荒野"));
            }
        };
        Runnable selectOngoing = () -> performTap(180, 101, inspect);
        recognizeScreenText(text -> {
            if (isTaskWindowVisible(screenLines(text))) {
                selectOngoing.run();
            } else {
                performTap(1255, 285, selectOngoing);
            }
        });
    }

    private List<String> screenLines(Text text) {
        List<String> values = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                values.add(line.getText());
            }
        }
        return values;
    }

    static boolean isTaskWindowVisible(List<String> values) {
        boolean ongoing = false;
        boolean available = false;
        for (String value : values) {
            String normalized = normalizeText(value);
            ongoing |= normalized.contains("进行中");
            available |= normalized.contains("可承接");
        }
        return ongoing && available;
    }

    private void acceptMilitaryQuest(String questName) {
        showProgress("自动军务：承接" + questName);
        clickQuickArrival(
                () -> performTap(641, 473,
                        () -> {
                            String npc = expectedMilitaryNpc(questName);
                            if (npc == null) {
                                handler.postDelayed(
                                        () -> finishMilitaryConversation(questName), 19_000);
                            } else {
                                showProgress("自动军务：等待" + npc + "对话");
                                waitForScreenText(npc, SCREEN_WAIT_RETRY_COUNT,
                                        () -> finishMilitaryConversation(questName));
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

    private void findOngoingWildernessQuest(int remainingAttempts) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    String zone = extractWildernessZone(line.getText());
                    if (zone != null && bounds != null && bounds.centerX() < 640) {
                        militaryZone = zone;
                        showProgress("自动军务：立即检查荒野任务");
                        performTap(bounds.centerX(), bounds.centerY(),
                                () -> inspectOngoingMilitaryQuest(
                                        "巡狩军团荒野",
                                        this::finishMilitaryAndStartTraining,
                                        this::openWildernessTraining));
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                handler.postDelayed(
                        () -> findOngoingWildernessQuest(remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else {
                failAutomation("Military wilderness quest was not found");
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

    private String findMilitaryAvailabilityText(Text text) {
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds != null && bounds.centerX() > 640
                        && (isCooldownText(line.getText())
                                || isAvailableMilitaryQuest(line.getText()))) {
                    return line.getText();
                }
            }
        }
        return null;
    }

    static boolean isCooldownText(String value) {
        return normalizeText(value).contains("冷却时间");
    }

    static boolean isAvailableMilitaryQuest(String value) {
        return normalizeText(value).contains("承接等级");
    }

    static Integer extractCooldownMinutes(String value) {
        Matcher matcher = MILITARY_COOLDOWN.matcher(normalizeText(value));
        if (!matcher.find() || matcher.group(1) == null
                && matcher.group(2) == null && matcher.group(3) == null) {
            return null;
        }
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int seconds = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return hours * 60 + minutes + (seconds > 0 ? 1 : 0);
    }

    static boolean allExecutableMilitaryQuestsCooling(
            boolean supplyCooling, boolean wildernessCooling) {
        return supplyCooling && wildernessCooling;
    }

    private void finishMilitaryConversation(String questName) {
        showProgress("自动军务：OCR 查找并点击“想”");
        Runnable leave = () -> clickScreenText("离开", true,
                () -> {
                    if (questName != null) {
                        showNextMilitaryTime(WorshipAlarmReceiver
                                .markMilitaryQuestHandled(this, questName));
                    }
                    performTap(316, 101,
                            () -> findAvailableMilitaryQuest(TEXT_RETRY_COUNT));
                });
        clickScreenText("想", true, () -> {
            showProgress("自动军务：点击“离开”完成承接");
            leave.run();
        }, SCREEN_WAIT_RETRY_COUNT);
    }

    private void retryUnacceptedMilitaryQuest(String questName) {
        if (!retriedUnacceptedQuests.add(questName)) {
            failAutomation("任务未进入进行中：" + questName);
            return;
        }
        showProgress("自动军务：" + questName + "尚未承接，重新承接");
        checkedMilitaryQuests.remove(questName);
        performTap(316, 101, () -> findAvailableMilitaryQuest(TEXT_RETRY_COUNT));
    }

    private void openWildernessTraining() {
        showProgress("自动军务：进入荒野修炼");
        performTap(1215, 58,
                () -> performTap(994, 245,
                        () -> performTap(780, 613,
                                () -> performTap(640, 478,
                                        () -> handler.postDelayed(
                                                () -> waitForScreenText(
                                                        "荒野营地",
                                                        SCREEN_WAIT_RETRY_COUNT,
                                                        this::findWildernessTeleporter),
                                                4_000)))));
    }

    private void findWildernessTeleporter() {
        showProgress("自动军务：打开自动寻路");
        performTap(1130, 500, () -> {
            showProgress("自动军务：选择寻路页");
            performTap(1165, 165, () -> {
                showProgress("自动军务：寻找荒野传送官");
                clickScreenText("荒野传送官", true, () -> {
                    showProgress("自动军务：选择荒野修炼二区");
                    clickScreenText("二区", false,
                            () -> handler.postDelayed(
                                    () -> closeAutoPathPanelIfNeeded(this::clickMilitaryQuest),
                                    5_000),
                            SCREEN_WAIT_RETRY_COUNT);
                }, SCREEN_WAIT_RETRY_COUNT);
            });
        });
    }

    private void clickMilitaryQuest() {
        showProgress("自动军务：点击右侧任务");
        if (militaryZone == null) {
            failAutomation("未读取到巡狩军团荒野区号");
            return;
        }
        clickRightScreenText("巡狩军团荒野",
                () -> scheduleMilitaryQuestCheck(
                        "巡狩军团荒野",
                        this::finishMilitaryAndStartTraining),
                SCREEN_WAIT_RETRY_COUNT, null);
    }

    private void finishMilitaryAndStartTraining() {
        returnHome(() -> {
            showProgress("自动军务已完成，10秒后自动练级");
            handler.postDelayed(this::startTraining, 10_000);
        });
    }

    private void startTraining() {
        showProgress("自动练级：使用第一个标记卷");
        performTap(50, 625, () -> {
            showProgress("自动练级：等待地图加载");
            handler.postDelayed(() -> {
                showProgress("自动练级：等待自动攻击按钮");
                ensureAutoAttackEnabled(this::waitForNextMilitary);
            }, 5_000);
        });
    }

    private void waitForNextMilitary() {
        automationRunning = false;
        setTaskState(STATE_RUNNING);
        long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitary(this);
        showNextMilitaryTime(nextMilitaryAt);
        showProgress("自动练级中 · 下次军务 " + formatTime(nextMilitaryAt));
    }

    private String formatTime(long value) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(value));
    }

    private void scheduleMilitaryQuestCheck(String questName, Runnable completed) {
        long checkAt = System.currentTimeMillis() + MILITARY_PROGRESS_CHECK_MS;
        showProgress("自动军务：" + questName
                + "收集中，下次检查 " + formatTime(checkAt));
        handler.postDelayed(
                () -> checkOngoingMilitaryQuest(questName, completed),
                MILITARY_PROGRESS_CHECK_MS);
    }

    private void checkOngoingMilitaryQuest(String questName, Runnable completed) {
        showProgress("自动军务：检查" + questName);
        closeAutoPathPanelIfNeeded(
                () -> performTap(1255, 285,
                        () -> performTap(180, 101,
                                () -> clickLeftScreenText(questName,
                                        () -> inspectOngoingMilitaryQuest(
                                                questName, completed,
                                                () -> scheduleMilitaryQuestCheck(
                                                        questName, completed)),
                                        TEXT_RETRY_COUNT,
                                        () -> failAutomation(
                                                "任务未在进行中：" + questName)))));
    }

    private void inspectOngoingMilitaryQuest(
            String questName, Runnable completed, Runnable incomplete) {
        inspectOngoingMilitaryQuest(
                questName, completed, incomplete, TEXT_RETRY_COUNT);
    }

    private void inspectOngoingMilitaryQuest(
            String questName, Runnable completed, Runnable incomplete,
            int remainingAttempts) {
        showNextMilitaryTime(WorshipAlarmReceiver.markMilitaryQuestHandled(this, questName));
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            Boolean complete = requiredItemsComplete(text);
            if (isMilitaryQuestComplete(complete, false)) {
                finishOngoingMilitaryQuest(questName, completed);
                return;
            }
            if (Boolean.FALSE.equals(complete)) {
                showProgress("自动军务：" + questName + "材料未满");
                performTap(982, 52, incomplete);
                return;
            }
            detectCompletedMilitaryQuestMark(markFound -> {
                if (isMilitaryQuestComplete(complete, markFound)) {
                    finishOngoingMilitaryQuest(questName, completed);
                } else if (remainingAttempts > 1) {
                    handler.postDelayed(
                            () -> inspectOngoingMilitaryQuest(
                                    questName, completed, incomplete,
                                    remainingAttempts - 1),
                            ACTION_DELAY_MS);
                } else {
                    failAutomation("未识别到" + questName + "的材料进度或完成标记");
                }
            });
        });
    }

    static boolean isMilitaryQuestComplete(Boolean progressComplete, boolean markFound) {
        return Boolean.TRUE.equals(progressComplete)
                || progressComplete == null && markFound;
    }

    private void finishOngoingMilitaryQuest(String questName, Runnable completed) {
        showProgress("自动军务：" + questName + "材料已满，快速抵达");
        Runnable leave = () -> clickScreenText("离开", true,
                completed, SCREEN_WAIT_RETRY_COUNT);
        Runnable afterArrival = () -> {
            showProgress("自动军务：点击自动寻路");
            clickScreenText("自动寻路", false,
                    leave, SCREEN_WAIT_RETRY_COUNT);
        };
        handler.postDelayed(
                () -> clickQuickArrival(afterArrival),
                ACTION_DELAY_MS);
    }

    private Boolean requiredItemsComplete(Text text) {
        StringBuilder value = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                if (bounds != null && bounds.centerX() > 640
                        && bounds.centerY() > 150 && bounds.centerY() < 400) {
                    value.append(line.getText()).append(' ');
                }
            }
        }
        return requiredItemComplete(value.toString());
    }

    static Boolean requiredItemComplete(String value) {
        Matcher matcher = REQUIRED_ITEM_PROGRESS.matcher(normalizeText(value));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1)) >= Integer.parseInt(matcher.group(2));
    }

    private void detectCompletedMilitaryQuestMark(Consumer<Boolean> result) {
        captureScreenshot(bitmap -> {
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

    private void returnHome(Runnable next) {
        showProgress("自动军务：回城（1/2）");
        performTap(1210, 640, () -> {
            showProgress("自动军务：回城（2/2）");
            performTap(1210, 640, next);
        });
    }

    private void performTap(int x, int y, Runnable next) {
        if (!automationRunning) {
            return;
        }
        boolean accepted = tap(x, y, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                scheduleNext(next);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                failAutomation("Tap was cancelled");
            }
        });
        if (!accepted) {
            failAutomation("Tap was rejected");
        }
    }

    private void performSwipe(int startX, int startY, int endX, int endY, Runnable next) {
        if (!automationRunning) {
            return;
        }
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 300))
                .build();
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                scheduleNext(next);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                failAutomation("Swipe was cancelled");
            }
        }, handler);
        if (!accepted) {
            failAutomation("Swipe was rejected");
        }
    }

    private void scheduleNext(Runnable next) {
        if (automationRunning) {
            handler.postDelayed(next, CLICK_DELAY_MS);
        }
    }

    private void detectRanking(Consumer<Boolean> result) {
        captureScreenshot(bitmap -> {
            try {
                result.accept(bitmap != null && hasRankingIcon(bitmap));
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        });
    }

    private void ensureAutoAttackDisabled(Runnable next) {
        ensureAutoAttackState(false, next, SCREEN_WAIT_RETRY_COUNT);
    }

    private void ensureAutoAttackEnabled(Runnable next) {
        ensureAutoAttackState(true, next, SCREEN_WAIT_RETRY_COUNT);
    }

    private void ensureAutoAttackState(boolean targetEnabled, Runnable next) {
        ensureAutoAttackState(targetEnabled, next, 1);
    }

    private void ensureAutoAttackState(
            boolean targetEnabled, Runnable next, int remainingAttempts) {
        captureScreenshot(bitmap -> {
            try {
                if (!automationRunning) {
                    return;
                }
                Boolean enabled = bitmap == null ? null : isAutoAttackEnabled(bitmap);
                if (enabled == null) {
                    if (remainingAttempts > 1) {
                        handler.postDelayed(
                                () -> ensureAutoAttackState(
                                        targetEnabled, next, remainingAttempts - 1),
                                ACTION_DELAY_MS);
                    } else {
                        failAutomation("Unable to detect auto attack state");
                    }
                } else if (enabled == targetEnabled) {
                    next.run();
                } else {
                    performTap(1210, 480, next);
                }
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        });
    }

    private void closeAutoPathPanelIfNeeded(Runnable next) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            if (hasAutoPathPanel(text)) {
                performTap(1248, 147,
                        () -> waitForAutoPathPanelClosed(next, TEXT_RETRY_COUNT));
            } else {
                next.run();
            }
        });
    }

    private void waitForAutoPathPanelClosed(Runnable next, int remainingAttempts) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            if (!hasAutoPathPanel(text)) {
                next.run();
            } else if (remainingAttempts > 1) {
                handler.postDelayed(
                        () -> waitForAutoPathPanelClosed(next, remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else {
                failAutomation("Auto path panel did not close");
            }
        });
    }

    private boolean hasAutoPathPanel(Text text) {
        boolean enemy = false;
        boolean path = false;
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String value = normalizeText(line.getText());
                enemy |= value.equals("敌人");
                path |= value.equals("寻路");
            }
        }
        return enemy || path;
    }

    private Boolean isAutoAttackEnabled(Bitmap bitmap) {
        int left = 1200 * bitmap.getWidth() / 1280;
        int right = 1260 * bitmap.getWidth() / 1280;
        int top = 440 * bitmap.getHeight() / 720;
        int bottom = 510 * bitmap.getHeight() / 720;
        int red = 0;
        int blue = 0;

        for (int y = top; y < bottom; y += 2) {
            for (int x = left; x < right; x += 2) {
                int color = bitmap.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (r > 130 && r > g * 1.25f && r > b * 1.25f) {
                    red++;
                }
                if (b > 100 && b > r * 1.2f && b > g * 1.05f) {
                    blue++;
                }
            }
        }
        return red + blue < 10 ? null : red > blue;
    }

    private void recognizeScreenText(Consumer<Text> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                failAutomation("Unable to capture screen for text recognition");
                return;
            }
            if (textRecognizer == null) {
                textRecognizer = TextRecognition.getClient(
                        new ChineseTextRecognizerOptions.Builder().build());
            }
            textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(result::accept)
                    .addOnFailureListener(error -> {
                        Log.e(TAG, "Text recognition failed", error);
                        failAutomation("Text recognition failed");
                    })
                    .addOnCompleteListener(task -> bitmap.recycle());
        });
    }

    private void clickQuickArrival(Runnable next) {
        clickQuickArrival(next, SCREEN_WAIT_RETRY_COUNT);
    }

    private void clickQuickArrival(Runnable next, int remainingAttempts) {
        captureScreenshot(bitmap -> {
            if (bitmap == null || !automationRunning) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                return;
            }
            int left = 480 * bitmap.getWidth() / 1280;
            int top = 520 * bitmap.getHeight() / 720;
            int right = 980 * bitmap.getWidth() / 1280;
            int bottom = 710 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * 2, cropped.getHeight() * 2, true);
            cropped.recycle();

            if (textRecognizer == null) {
                textRecognizer = TextRecognition.getClient(
                        new ChineseTextRecognizerOptions.Builder().build());
            }
            textRecognizer.process(InputImage.fromBitmap(enlarged, 0))
                    .addOnSuccessListener(text -> {
                        List<Text.Line> fragments = new ArrayList<>();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                String value = normalizeText(line.getText());
                                if (line.getBoundingBox() != null
                                        && isQuickArrivalFragment(value)) {
                                    fragments.add(line);
                                }
                            }
                        }
                        fragments.sort((leftLine, rightLine) -> Integer.compare(
                                leftLine.getBoundingBox().centerX(),
                                rightLine.getBoundingBox().centerX()));
                        List<String> values = new ArrayList<>();
                        Rect bounds = null;
                        for (Text.Line fragment : fragments) {
                            values.add(fragment.getText());
                            if (bounds == null) {
                                bounds = new Rect(fragment.getBoundingBox());
                            } else {
                                bounds.union(fragment.getBoundingBox());
                            }
                        }
                        if (bounds != null && matchesQuickArrivalFragments(values)) {
                            performTap(left + bounds.centerX() / 2,
                                    top + bounds.centerY() / 2, next);
                        } else if (remainingAttempts > 1) {
                            handler.postDelayed(
                                    () -> clickQuickArrival(next, remainingAttempts - 1),
                                    ACTION_DELAY_MS);
                        } else {
                            failAutomation("Screen text was not found: 快速抵达");
                        }
                    })
                    .addOnFailureListener(error -> {
                        Log.e(TAG, "Quick arrival text recognition failed", error);
                        failAutomation("Text recognition failed: 快速抵达");
                    })
                    .addOnCompleteListener(task -> enlarged.recycle());
        });
    }

    private static boolean isQuickArrivalFragment(String value) {
        String normalized = normalizeText(value);
        return !normalized.isEmpty() && ("快速抵达".contains(normalized)
                || normalized.contains("快速") || normalized.contains("抵达"));
    }

    static boolean matchesQuickArrivalFragments(List<String> fragments) {
        StringBuilder combined = new StringBuilder();
        for (String fragment : fragments) {
            combined.append(normalizeText(fragment));
        }
        String value = combined.toString();
        return value.contains("快速抵达")
                || value.contains("快速") || value.contains("抵达");
    }

    private void clickScreenText(String expected, boolean exact, Runnable next) {
        clickScreenText(expected, exact, next, TEXT_RETRY_COUNT);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
                                 int remainingAttempts) {
        clickScreenText(expected, exact, next, remainingAttempts, null);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
                                 int remainingAttempts, Runnable ifMissing) {
        clickScreenText(expected, exact, next, remainingAttempts, ifMissing, 0);
    }

    private void clickRightScreenText(String expected, Runnable next,
            int remainingAttempts, Runnable ifMissing) {
        clickScreenText(expected, false, next, remainingAttempts, ifMissing,
                850, Integer.MAX_VALUE);
    }

    private void clickLeftScreenText(String expected, Runnable next,
            int remainingAttempts, Runnable ifMissing) {
        clickScreenText(expected, false, next, remainingAttempts, ifMissing, 0, 640);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing, int minX) {
        clickScreenText(expected, exact, next, remainingAttempts, ifMissing,
                minX, Integer.MAX_VALUE);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing, int minX, int maxX) {
        if (!automationRunning) {
            return;
        }
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            String target = normalizeText(expected);
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String value = normalizeText(line.getText());
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && isHorizontalMatch(
                            bounds.centerX(), minX, maxX)
                            && (exact ? value.equals(target) : value.contains(target))) {
                        performTap(bounds.centerX(), bounds.centerY(), next);
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                handler.postDelayed(
                        () -> clickScreenText(expected, exact, next,
                                remainingAttempts - 1, ifMissing, minX, maxX),
                        ACTION_DELAY_MS);
            } else if (ifMissing != null) {
                ifMissing.run();
            } else {
                failAutomation("Screen text was not found: " + expected);
            }
        });
    }

    static boolean isHorizontalMatch(int x, int minX, int maxX) {
        return x >= minX && x <= maxX;
    }

    private void waitForScreenText(String expected, int remainingAttempts, Runnable next) {
        if (!automationRunning) {
            return;
        }
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            String target = normalizeText(expected);
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    if (normalizeText(line.getText()).contains(target)) {
                        next.run();
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                handler.postDelayed(
                        () -> waitForScreenText(expected, remainingAttempts - 1, next),
                        ACTION_DELAY_MS);
            } else {
                failAutomation("Screen text was not found: " + expected);
            }
        });
    }

    private static String normalizeText(String value) {
        return value.replaceAll("\\s+", "")
                .replace('(', '（')
                .replace(')', '）');
    }

    private void captureScreenshot(Consumer<Bitmap> result) {
        captureScreenshot(result, TEXT_RETRY_COUNT);
    }

    private void captureScreenshot(Consumer<Bitmap> result, int remainingAttempts) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            result.accept(null);
            return;
        }

        takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
            @Override
            public void onSuccess(ScreenshotResult screenshot) {
                HardwareBuffer buffer = screenshot.getHardwareBuffer();
                Bitmap hardwareBitmap = null;
                Bitmap bitmap = null;
                try {
                    ColorSpace colorSpace = screenshot.getColorSpace();
                    hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace);
                    if (hardwareBitmap != null) {
                        bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
                    }
                    result.accept(bitmap);
                    bitmap = null;
                } catch (RuntimeException error) {
                    Log.e(TAG, "Unable to inspect screenshot", error);
                    result.accept(null);
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    if (hardwareBitmap != null) {
                        hardwareBitmap.recycle();
                    }
                    buffer.close();
                }
            }

            @Override
            public void onFailure(int errorCode) {
                if (shouldRetryScreenshot(errorCode, remainingAttempts)
                        && automationRunning) {
                    Log.w(TAG, "Screenshot busy; retrying: " + errorCode);
                    handler.postDelayed(
                            () -> captureScreenshot(result, remainingAttempts - 1),
                            ACTION_DELAY_MS);
                } else {
                    Log.e(TAG, "Screenshot failed: " + errorCode);
                    result.accept(null);
                }
            }
        });
    }

    static boolean shouldRetryScreenshot(int errorCode, int remainingAttempts) {
        return errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT
                && remainingAttempts > 1;
    }

    private boolean hasRankingIcon(Bitmap bitmap) {
        int left = 1162 * bitmap.getWidth() / 1280;
        int right = 1234 * bitmap.getWidth() / 1280;
        int top = 188 * bitmap.getHeight() / 720;
        int bottom = 266 * bitmap.getHeight() / 720;
        int bright = 0;
        int red = 0;
        int total = 0;

        for (int y = top; y < bottom; y += 2) {
            for (int x = left; x < right; x += 2) {
                int color = bitmap.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (r > 180 && g > 160 && b > 120) {
                    bright++;
                }
                if (r > 140 && r > g * 1.45f && r > b * 1.25f) {
                    red++;
                }
                total++;
            }
        }
        return bright * 100 >= total * 12 && red * 100 >= total * 7;
    }

    private void completeAutomation() {
        automationRunning = false;
        hideProgress();
        setTaskState(STATE_COMPLETED);
    }

    private void failAutomation(String message) {
        Log.e(TAG, message);
        automationRunning = false;
        handler.removeCallbacksAndMessages(null);
        setTaskState(STATE_STOPPED);
        showProgress("错误：" + message);
    }

    private void stopAutomation(int state) {
        automationRunning = false;
        handler.removeCallbacksAndMessages(null);
        hideProgress();
        setTaskState(state);
    }

    private void showProgress(String value) {
        if (progressView != null) {
            progressView.setText(value);
            return;
        }
        if (windowManager == null) {
            return;
        }

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(11);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(8), dp(3), dp(8), dp(3));
        text.setAlpha(0.7f);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.BLACK);
        background.setCornerRadius(dp(8));
        text.setBackground(background);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(4);

        try {
            windowManager.addView(text, params);
            progressView = text;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to show progress overlay", error);
        }
    }

    private void hideProgress() {
        if (windowManager != null && progressView != null) {
            try {
                windowManager.removeView(progressView);
            } catch (IllegalArgumentException ignored) {
                // The system already removed it.
            }
        }
        progressView = null;
    }

    private void showNextMilitaryTime(long value) {
        String label = "下次军务 " + formatTime(value);
        if (nextMilitaryView != null) {
            nextMilitaryView.setText(label);
            return;
        }
        if (windowManager == null) {
            return;
        }
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(11);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(8), dp(3), dp(8), dp(3));
        text.setAlpha(0.7f);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.BLACK);
        background.setCornerRadius(dp(8));
        text.setBackground(background);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = dp(4);
        try {
            windowManager.addView(text, params);
            nextMilitaryView = text;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to show next military time", error);
        }
    }

    private void hideNextMilitaryTime() {
        if (windowManager != null && nextMilitaryView != null) {
            try {
                windowManager.removeView(nextMilitaryView);
            } catch (IllegalArgumentException ignored) {
                // The system already removed it.
            }
        }
        nextMilitaryView = null;
    }

    private void exitService() {
        closeMenu();
        cleanUp();
        disableSelf();
    }

    private void updateMenuPosition() {
        if (windowManager == null || menuView == null) {
            return;
        }
        positionMenu();
        windowManager.updateViewLayout(menuView, menuParams);
    }

    private void positionMenu() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int gap = dp(8);
        int menuWidth = menuParams.width;
        int childCount = menuView instanceof LinearLayout
                ? ((LinearLayout) menuView).getChildCount()
                : 6;
        int estimatedMenuHeight = dp(20 + childCount * 44);

        menuParams.x = bubbleParams.x > screenWidth / 2
                ? bubbleParams.x - menuWidth - gap
                : bubbleParams.x + bubbleParams.width + gap;
        menuParams.x = Math.max(0, Math.min(menuParams.x, screenWidth - menuWidth));
        menuParams.y = Math.max(0, Math.min(bubbleParams.y, screenHeight - estimatedMenuHeight));
    }

    private void closeMenu() {
        if (windowManager != null && menuView != null) {
            try {
                windowManager.removeView(menuView);
            } catch (IllegalArgumentException ignored) {
                // The system already removed it.
            }
        }
        menuView = null;
        menuParams = null;
        stateView = null;
    }

    private GradientDrawable bubbleBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        return background;
    }

    private void cleanUp() {
        instance = null;
        automationRunning = false;
        handler.removeCallbacksAndMessages(null);
        hideProgress();
        hideNextMilitaryTime();
        closeMenu();
        if (windowManager != null && bubbleView != null) {
            try {
                windowManager.removeView(bubbleView);
            } catch (IllegalArgumentException ignored) {
                // The system already removed it.
            }
        }
        bubbleView = null;
        bubbleParams = null;
        windowManager = null;
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
