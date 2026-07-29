package com.local.sgmhelper;

import android.annotation.SuppressLint;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class HelperAccessibilityService extends AccessibilityService
        implements AutomationHost {
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_STOPPED = 3;
    private static final int STATE_COMPLETED = 4;
    private static final int ACTION_DELAY_MS = 1000;
    private static final int CLICK_DELAY_MS = 2000;
    private static final int TEXT_RETRY_COUNT = 5;
    private static final int SCREEN_WAIT_RETRY_COUNT = 20;
    private static final int MAX_AUTOMATION_RECOVERY_ATTEMPTS = 3;
    private static final int RECOVERY_START_DELAY_MS = 5_000;
    private static final int RECOVERY_LONG_DELAY_MS = 60_000;
    private static final long INVENTORY_CHECK_INTERVAL_MS = 60_000;
    private static final int COLOR_SETTINGS_BACKGROUND = 0xFF4A4A4A;
    private static final int COLOR_SETTINGS_TEXT = 0xFFFFC107;
    static final String PREFS_NAME = "schedule";
    static final String PREF_HOUR = "hour";
    static final String PREF_MINUTE = "minute";
    static final String PREF_LEGION_HOUR = "legion_hour";
    static final String PREF_LEGION_MINUTE = "legion_minute";
    static final String PREF_MILITARY_HOUR = "military_hour";
    static final String PREF_MILITARY_MINUTE = "military_minute";
    static final String PREF_WELFARE_HOUR = "welfare_hour";
    static final String PREF_WELFARE_MINUTE = "welfare_minute";
    static final String PREF_HEAVENFALL_HOUR = "heavenfall_hour";
    static final String PREF_HEAVENFALL_MINUTE = "heavenfall_minute";
    static final String PREF_DUNGEON_HOUR = "dungeon_hour";
    static final String PREF_DUNGEON_MINUTE = "dungeon_minute";
    static final String PREF_HEAVENFALL_DURATION_MINUTES = "heavenfall_duration_minutes";
    static final String PREF_HEAVENFALL_ZONE = "heavenfall_zone";
    static final String PREF_WORSHIP_ENABLED = "worship_enabled";
    static final String PREF_MILITARY_ENABLED = "military_enabled";
    static final String PREF_MILITARY_SUPPLY_ENABLED = "military_supply_enabled";
    static final String PREF_MILITARY_WILDERNESS_ENABLED = "military_wilderness_enabled";
    static final String PREF_WELFARE_ENABLED = "welfare_enabled";
    static final String PREF_LEGION_REWARD_ENABLED = "legion_reward_enabled";
    static final String PREF_HEAVENFALL_ENABLED = "heavenfall_enabled";
    static final String PREF_DUNGEON_ENABLED = "dungeon_enabled";
    static final String PREF_START_TRAINING_ON_LAUNCH = "start_training";
    static final String PREF_AUTO_SELL_ENABLED = "auto_sell_enabled";
    static final String PREF_AUTO_SELL_MIN_FREE_SLOTS = "auto_sell_min_free_slots";
    static final String PREF_PRIMARY_TASK = "primary_task";
    static final String PREF_MANUALLY_STOPPED = "manually_stopped";
    private static final String PREF_DUNGEON_BATTLE_MODE = "dungeon_battle_mode";
    private static final String PREF_DUNGEON_ELITE_MODE = "dungeon_elite_mode";
    static final String PREF_SOLDIER_REVIVAL_ENABLED = "soldier_revival_before_training";
    static final String PREF_BOSS_FOLLOW_LEADER = "boss_follow_leader";
    static final String PREF_WORLD_BOSS_MAP = "world_boss_map";
    static final String PREF_WORLD_BOSS_TARGET = "world_boss_target";
    static final String PREF_MILITARY_RETRY_AT = "military_retry_at";
    static final String PREF_SUPPLY_RETRY_AT = "supply_retry_at";
    static final String PREF_WILDERNESS_RETRY_AT = "wilderness_retry_at";
    private static final String PREF_DUNGEON_SWEEP_LEVELS = "dungeon_sweep_levels";
    private static final String PREF_DUNGEON_BATTLE_LEVELS = "dungeon_battle_levels";
    private static final String PREF_DUNGEON_ELITE_LEVELS = "dungeon_elite_levels";
    static final String PREF_TRAINING_LOCATION = "training_location";
    static final String PREF_TRAINING_WILDERNESS_ZONE = "training_wilderness_zone";
    static final String PREF_TRAINING_MONSTER = "training_monster";
    static final String PREF_TRAINING_PULL_FOR_OTHERS = "training_pull_for_others";
    static final String PREF_TRAINING_PULL_ROUTE = "training_pull_route";
    static final String TRAINING_LOCATION_WILDERNESS = "荒野";
    static final String TRAINING_LOCATION_WILD = "野境";
    static final String TRAINING_LOCATION_MARKER = "标记点";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler inventoryHandler = new Handler(Looper.getMainLooper());
    private final AutomationTaskManager taskManager = new AutomationTaskManager(
            new AutomationTaskManager.Listener() {
                @Override
                public void onStart(AutomationTaskManager.Run run) {
                    beginManagedRun(run);
                }

                @Override
                public void onCancel(AutomationTaskManager.Run run) {
                    cancelManagedRun(run);
                }

                @Override
                public void onResumePrimary() {
                    automationRunning = false;
                    clearAutomationRecovery();
                    restartPrimaryTask(primaryTask);
                }
            });
    private final TrainingAutomation trainingAutomation = new TrainingAutomation(this);
    private final AutoSellAutomation autoSellAutomation = new AutoSellAutomation(this);
    private final TaskAutomation taskAutomation = new TaskAutomation(this);
    private final RewardAutomation rewardAutomation = new RewardAutomation(this);
    private final WelfareAutomation welfareAutomation = new WelfareAutomation(this);
    private final LoginAutomation loginAutomation = new LoginAutomation(this);
    private final AntiCheatVerification hudAntiCheatVerification =
            new AntiCheatVerification(this);
    private final DungeonSweepAutomation dungeonSweepAutomation =
            new DungeonSweepAutomation(this);
    private final DungeonBattleAutomation dungeonBattleAutomation =
            new DungeonBattleAutomation(this);
    private final BossAutomation bossAutomation = new BossAutomation(this);
    private final ChannelSwitchTest channelSwitchTest = new ChannelSwitchTest(this);
    private final HeavenfallAutomation heavenfallAutomation = new HeavenfallAutomation(this);
    private final SoldierRevivalAutomation soldierRevivalAutomation =
            new SoldierRevivalAutomation(this);

    // ponytail: one in-process service reference keeps the self-test small; clear it on every teardown.
    @SuppressLint("StaticFieldLeak")
    private static volatile HelperAccessibilityService instance;

    private WindowManager windowManager;
    private Button bubbleView;
    private View menuView;
    private TextView stateView;
    private TextView progressView;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams menuParams;
    private int taskState = STATE_IDLE;
    private boolean automationRunning;
    private int automationRecoveryAttempts;
    private long failingRunId;
    private Runnable currentAutomationAction;
    private PrimaryTask primaryTask = PrimaryTask.TRAINING;
    private Runnable primaryTaskAction = trainingAutomation::start;
    private boolean inventoryCheckRunning;
    private boolean inventorySelling;
    private PaddleDungeonTextRecognizer paddleDungeonTextRecognizer;

    static HelperAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        DiagnosticLog.info("SERVICE", "accessibility service connected");
        restorePrimaryTask();
        showOverlay();
        WorshipAlarmReceiver.scheduleAll(this);
        boolean startedPending = WorshipAlarmReceiver.startPendingAutomation(this);
        if (!startedPending && primaryTask != PrimaryTask.TRAINING) {
            handler.postDelayed(this::resumePersistedPrimaryTask, 1_000);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The MVP does not inspect accessibility events or screen content.
    }

    @Override
    public void onInterrupt() {
        DiagnosticLog.warn("SERVICE", "accessibility service interrupted");
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
            DiagnosticLog.error("OVERLAY", "WindowManager is unavailable");
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
            DiagnosticLog.error("OVERLAY", "Unable to add accessibility overlay", error);
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
        menu.setElevation(dp(10));

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
            DiagnosticLog.error("OVERLAY", "Unable to add floating menu", error);
        }
    }

    private void populateMainMenu(LinearLayout menu) {
        menu.removeAllViews();
        styleMainMenu(menu);
        setMenuSize(dp(230), WindowManager.LayoutParams.WRAP_CONTENT);
        stateView = addMenuText(menu, R.string.menu_status_idle);
        addMenuButton(menu, R.string.menu_training, view -> startTrainingMain());
        addMenuButton(menu, R.string.menu_boss, this::startBossFromMenu);
        addMenuButton(menu, R.string.menu_dungeon_sweep,
                view -> startDungeonBattle());
        addMenuButton(menu, R.string.menu_channel_test, view -> channelSwitchTest.start());
        addMenuButton(menu, R.string.menu_stop, view -> stopAutomation(STATE_STOPPED));
        addMenuButton(menu, R.string.menu_settings, view -> showTrainingSettings());
        addMenuButton(menu, R.string.menu_update, view -> openUpdater());
        addMenuButton(menu, R.string.menu_exit, view -> exitService());
        addMenuText(menu, "版本 " + BuildConfig.VERSION_NAME);
        updateStateView();
        updateMenuPosition();
    }

    private void openUpdater() {
        closeMenu();
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_SHOW_CONTROLS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void styleMainMenu(LinearLayout menu) {
        menu.setPadding(dp(12), dp(10), dp(12), dp(10));
        menu.setAlpha(0.92f);
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_SETTINGS_BACKGROUND);
        background.setCornerRadius(dp(4));
        menu.setBackground(background);
    }

    private TextView addMenuText(LinearLayout menu, int textResource) {
        return addMenuText(menu, getString(textResource));
    }

    private TextView addMenuText(LinearLayout menu, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
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

    void startTrainingMain() {
        if (isAutomationRunning()) {
            showProgress("已有任务正在运行，请先终止");
            closeMenu();
            return;
        }
        setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
        stopInventoryMonitoring();
        startTrainingAfterPreparation();
    }

    private void startBossFromMenu(View button) {
        startBossFromMenu(button, bossAutomation::start);
    }

    private void startWildernessBossFromMenu(View button) {
        startBossFromMenu(button, bossAutomation::startWilderness);
    }

    private void startWorldBossFromMenu(View button) {
        startBossFromMenu(button, bossAutomation::startWorld);
    }

    private void startBossFromMenu(View button, Runnable start) {
        if (isAutomationRunning()) {
            start.run();
            return;
        }
        button.setEnabled(false);
        handler.postDelayed(start, 300);
    }

    private void startTrainingAfterPreparation() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (WorshipAlarmReceiver.shouldScheduleMilitary(
                preferences.getBoolean(PREF_MILITARY_ENABLED, true),
                preferences.getBoolean(PREF_MILITARY_SUPPLY_ENABLED, true),
                preferences.getBoolean(PREF_MILITARY_WILDERNESS_ENABLED, true))) {
            showProgress("初始化：重新检测军务任务");
            startScheduledMilitary();
        } else {
            startTrainingPrimaryTask(this::startTrainingWithOptionalRevival);
        }
    }

    private void startTrainingWithOptionalRevival() {
        if (getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREF_SOLDIER_REVIVAL_ENABLED, false)) {
            soldierRevivalAutomation.run(trainingAutomation::start);
        } else {
            trainingAutomation.start();
        }
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

    private LinearLayout prepareSettingsPage(int selectedTab) {
        if (!(menuView instanceof LinearLayout)) {
            return null;
        }
        LinearLayout menu = (LinearLayout) menuView;
        menu.removeAllViews();
        stateView = null;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        setMenuSize(Math.min(dp(1000), screenWidth - dp(32)),
                Math.min(dp(620), screenHeight - dp(32)));
        menu.setPadding(dp(18), dp(14), dp(18), dp(14));
        menu.setAlpha(0.95f);
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_SETTINGS_BACKGROUND);
        background.setCornerRadius(dp(8));
        menu.setBackground(background);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        addSettingsTab(tabs, R.string.settings_training, selectedTab,
                view -> showTrainingSettings());
        addSettingsTab(tabs, R.string.settings_boss, selectedTab,
                view -> showBossMenu());
        addSettingsTab(tabs, R.string.menu_dungeon_sweep, selectedTab,
                view -> showDungeonSweepMenu());
        addSettingsTab(tabs, R.string.settings_timed_tasks, selectedTab,
                view -> showTimerPage());
        addSettingsTab(tabs, R.string.settings_auxiliary, selectedTab,
                view -> showAuxiliarySettings());
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        tabsParams.bottomMargin = dp(12);
        menu.addView(tabs, tabsParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        menu.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return content;
    }

    private void addSettingsTab(LinearLayout tabs, int textResource, int selectedTab,
            View.OnClickListener listener) {
        TextView tab = new TextView(this);
        tab.setText(textResource);
        boolean selected = textResource == selectedTab;
        tab.setTextSize(16);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(18), 0, dp(18), 0);
        tab.setTypeface(tab.getTypeface(), selected
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        if (selected) {
            tab.setTextColor(0xFF2B2B2B);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(COLOR_SETTINGS_TEXT);
            pill.setCornerRadius(dp(18));
            tab.setBackground(pill);
        } else {
            tab.setTextColor(0xFFE0E0E0);
        }
        tab.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        params.rightMargin = dp(10);
        tabs.addView(tab, params);
    }

    private TextView addSettingsText(LinearLayout menu, int textResource) {
        TextView text = addMenuText(menu, textResource);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.START);
        text.setPadding(dp(4), dp(4), dp(4), dp(6));
        return text;
    }

    private void tintCheckable(CompoundButton box) {
        box.setButtonTintList(ColorStateList.valueOf(COLOR_SETTINGS_TEXT));
    }

    private Button createSettingsButton(int textResource, boolean accent,
            View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(textResource);
        button.setTextSize(14);
        button.setSingleLine(true);
        button.setTextColor(accent ? 0xFF2B2B2B : Color.WHITE);
        button.setMinWidth(0);
        button.setMinimumWidth(dp(104));
        button.setMinHeight(0);
        button.setMinimumHeight(dp(38));
        button.setPadding(dp(20), 0, dp(20), 0);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(accent ? COLOR_SETTINGS_TEXT : 0xFF6A6A6A);
        pill.setCornerRadius(dp(19));
        button.setBackground(pill);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout addSettingsButtonRow(LinearLayout menu, Button... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (Button button : buttons) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            params.rightMargin = dp(12);
            row.addView(button, params);
        }
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        menu.addView(row, rowParams);
        return row;
    }

    private void showMainMenu() {
        if (menuView instanceof LinearLayout) {
            populateMainMenu((LinearLayout) menuView);
        }
    }

    private void showTrainingSettings() {
        LinearLayout menu = prepareSettingsPage(R.string.settings_training);
        if (menu == null) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText(R.string.training_location);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER_VERTICAL);
        locationRow.addView(label, new LinearLayout.LayoutParams(dp(120), dp(40)));

        RadioGroup locations = new RadioGroup(this);
        locations.setOrientation(LinearLayout.HORIZONTAL);
        addLocationOption(locations, TRAINING_LOCATION_WILDERNESS);
        addLocationOption(locations, TRAINING_LOCATION_WILD);
        addLocationOption(locations, TRAINING_LOCATION_MARKER);
        locationRow.addView(locations, new LinearLayout.LayoutParams(0, dp(40), 1));
        menu.addView(locationRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        LinearLayout wildernessFields = new LinearLayout(this);
        wildernessFields.setOrientation(LinearLayout.VERTICAL);
        int savedZone = preferences.getInt(PREF_TRAINING_WILDERNESS_ZONE, 1);
        Spinner zone = addSpinnerRow(wildernessFields, R.string.training_wilderness_zone,
                15, savedZone,
                value -> preferences.edit().putInt(PREF_TRAINING_WILDERNESS_ZONE, value).apply());
        Spinner monster = addTextSpinnerRow(wildernessFields, R.string.training_monster,
                TrainingAutomation.monstersForZone(savedZone),
                preferences.getString(PREF_TRAINING_MONSTER,
                        TrainingAutomation.defaultMonsterForZone(savedZone)),
                value -> preferences.edit().putString(PREF_TRAINING_MONSTER, value).apply());
        zone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view,
                    int position, long id) {
                int value = position + 1;
                preferences.edit().putInt(PREF_TRAINING_WILDERNESS_ZONE, value).apply();
                setSpinnerValues(monster, TrainingAutomation.monstersForZone(value),
                        preferences.getString(PREF_TRAINING_MONSTER, ""));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        menu.addView(wildernessFields, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        String selectedLocation = preferences.getString(
                PREF_TRAINING_LOCATION, TRAINING_LOCATION_MARKER);
        for (int index = 0; index < locations.getChildCount(); index++) {
            RadioButton option = (RadioButton) locations.getChildAt(index);
            if (selectedLocation.equals(option.getTag())) {
                option.setChecked(true);
                break;
            }
        }
        wildernessFields.setVisibility(shouldShowWildernessOptions(selectedLocation)
                ? View.VISIBLE : View.GONE);
        locations.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected == null) {
                return;
            }
            String location = selected.getTag().toString();
            preferences.edit().putString(PREF_TRAINING_LOCATION, location).apply();
            wildernessFields.setVisibility(shouldShowWildernessOptions(location)
                    ? View.VISIBLE : View.GONE);
        });

        CheckBox pullForOthers = addSettingsCheckBox(
                menu, R.string.training_pull_for_others,
                preferences.getBoolean(PREF_TRAINING_PULL_FOR_OTHERS, false));
        pullForOthers.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(PREF_TRAINING_PULL_FOR_OTHERS, checked).apply();
            if (checked && preferences.getString(
                    PREF_TRAINING_PULL_ROUTE, "").trim().isEmpty()) {
                showTrainingPullRouteDialog();
            }
        });
        addSettingsText(menu, R.string.training_pull_route_hint);
        addSettingsButtonRow(menu,
                createSettingsButton(R.string.training_pull_route, false,
                        view -> showTrainingPullRouteDialog()));
        addSettingsButtonRow(menu,
                createSettingsButton(R.string.settings_back, false, view -> showMainMenu()));
        updateMenuPosition();
    }

    private void showTrainingPullRouteDialog() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        EditText route = new EditText(this);
        route.setMinLines(5);
        route.setGravity(Gravity.TOP);
        route.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        route.setHint(R.string.training_pull_route_example);
        route.setText(preferences.getString(PREF_TRAINING_PULL_ROUTE, ""));
        int padding = dp(20);
        route.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.training_pull_route)
                .setView(route)
                .setNegativeButton(R.string.login_cancel, null)
                .setPositiveButton(R.string.login_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = route.getText().toString().trim();
                    try {
                        TrainingAutomation.parsePullRoute(value);
                        preferences.edit().putString(PREF_TRAINING_PULL_ROUTE, value).apply();
                        dialog.dismiss();
                    } catch (IllegalArgumentException error) {
                        route.setError(error.getMessage());
                    }
                }));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        }
        dialog.show();
    }

    private void addLocationOption(RadioGroup group, String value) {
        RadioButton option = new RadioButton(this);
        option.setId(View.generateViewId());
        option.setTag(value);
        option.setText(value);
        option.setTextColor(Color.WHITE);
        option.setTextSize(14);
        option.setPadding(dp(4), 0, dp(16), 0);
        tintCheckable(option);
        group.addView(option, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
    }

    private Spinner addSpinnerRow(LinearLayout parent, int labelResource, int optionCount,
            int selectedValue, Consumer<Integer> onSelected) {
        List<String> values = new ArrayList<>();
        for (int value = 1; value <= optionCount; value++) {
            values.add(String.valueOf(value));
        }
        Spinner spinner = addTextSpinnerRow(parent, labelResource, values,
                String.valueOf(selectedValue), value -> onSelected.accept(Integer.parseInt(value)));
        spinner.setSelection(Math.max(0, Math.min(selectedValue - 1, optionCount - 1)));
        return spinner;
    }

    private Spinner addTextSpinnerRow(LinearLayout parent, int labelResource,
            List<String> values, String selectedValue, Consumer<String> onSelected) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText(labelResource);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(dp(120), dp(40)));

        Spinner spinner = new Spinner(this);
        setSpinnerValues(spinner, values, selectedValue);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view,
                    int position, long id) {
                onSelected.accept(parentView.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        row.addView(spinner, new LinearLayout.LayoutParams(dp(160), dp(40)));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        return spinner;
    }

    private void setSpinnerValues(Spinner spinner, List<String> values, String selectedValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView text = (TextView) view;
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(14);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int selectedIndex = values.indexOf(selectedValue);
        spinner.setSelection(selectedIndex < 0 ? 0 : selectedIndex);
    }

    static boolean shouldShowWildernessOptions(String location) {
        return TRAINING_LOCATION_WILDERNESS.equals(location);
    }

    private void showLoginSettings() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), 0, dp(20), 0);

        EditText account = new EditText(this);
        account.setHint(R.string.login_account_hint);
        account.setSingleLine(true);
        account.setText(preferences.getString(LoginAutomation.PREF_ACCOUNT, ""));
        fields.addView(account, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText password = new EditText(this);
        password.setHint(R.string.login_password_hint);
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setText(preferences.getString(LoginAutomation.PREF_PASSWORD, ""));
        fields.addView(password, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.settings_login)
                .setView(fields)
                .setNegativeButton(R.string.login_cancel, null)
                .setPositiveButton(R.string.login_save, (ignored, which) -> preferences.edit()
                        .putString(LoginAutomation.PREF_ACCOUNT,
                                account.getText().toString().trim())
                        .putString(LoginAutomation.PREF_PASSWORD,
                                password.getText().toString())
                        .apply())
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        }
        dialog.show();
    }

    private void showBossMenu() {
        LinearLayout menu = prepareSettingsPage(R.string.settings_boss);
        if (menu == null) {
            return;
        }
        showBossPage(menu);
    }

    private void showDungeonSweepMenu() {
        LinearLayout menu = prepareSettingsPage(R.string.menu_dungeon_sweep);
        if (menu == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        addDungeonSection(menu, preferences, R.string.dungeon_battle_section,
                DungeonBattleAutomation.LEVELS, PREF_DUNGEON_BATTLE_LEVELS,
                R.string.dungeon_start_battle, view -> startDungeonBattle());
        addDungeonSection(menu, preferences, R.string.dungeon_elite_section,
                DungeonBattleAutomation.ELITE_LEVELS, PREF_DUNGEON_ELITE_LEVELS,
                R.string.dungeon_start_elite, view -> startDungeonElite());
        addDungeonSection(menu, preferences, R.string.dungeon_sweep_section,
                DungeonSweepAutomation.LEVELS, PREF_DUNGEON_SWEEP_LEVELS,
                R.string.dungeon_start_sweep, view -> startDungeonSweep(false));

        addSettingsButtonRow(menu,
                createSettingsButton(R.string.settings_back, false, view -> showMainMenu()));
        updateMenuPosition();
    }

    /** One dungeon mode: its own level checkboxes, select-all/clear, and start button. */
    private void addDungeonSection(LinearLayout menu, SharedPreferences preferences,
            int titleResource, int[] levels, String preferenceKey,
            int startResource, View.OnClickListener onStart) {
        addSettingsText(menu, titleResource).setTextColor(COLOR_SETTINGS_TEXT);

        Set<String> selected = new HashSet<>(preferences.getStringSet(
                preferenceKey, Collections.emptySet()));
        List<CheckBox> boxes = new ArrayList<>();
        int columns = 6;
        for (int start = 0; start < levels.length; start += columns) {
            LinearLayout row = new LinearLayout(this);
            for (int index = start; index < start + columns; index++) {
                if (index >= levels.length) {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(38), 1));
                    continue;
                }
                int level = levels[index];
                CheckBox box = new CheckBox(this);
                box.setText(String.valueOf(level));
                box.setTextColor(Color.WHITE);
                box.setTextSize(14);
                tintCheckable(box);
                box.setChecked(selected.contains(String.valueOf(level)));
                box.setOnCheckedChangeListener((button, checked) -> {
                    Set<String> updated = new HashSet<>(preferences.getStringSet(
                            preferenceKey, Collections.emptySet()));
                    if (checked) {
                        updated.add(String.valueOf(level));
                    } else {
                        updated.remove(String.valueOf(level));
                    }
                    preferences.edit().putStringSet(preferenceKey, updated).apply();
                });
                boxes.add(box);
                row.addView(box, new LinearLayout.LayoutParams(0, dp(38), 1));
            }
            menu.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        }

        addSettingsButtonRow(menu,
                createSettingsButton(R.string.dungeon_select_all, false,
                        view -> boxes.forEach(box -> box.setChecked(true))),
                createSettingsButton(R.string.dungeon_clear, false,
                        view -> boxes.forEach(box -> box.setChecked(false))),
                createSettingsButton(startResource, true, onStart));
    }

    private static List<Integer> selectedDungeonLevels(SharedPreferences preferences) {
        return selectedDungeonLevels(preferences, PREF_DUNGEON_SWEEP_LEVELS,
                DungeonSweepAutomation.LEVELS);
    }

    private static List<Integer> selectedDungeonLevels(SharedPreferences preferences,
            String preferenceKey, int[] available) {
        Set<String> selected = preferences.getStringSet(
                preferenceKey, Collections.emptySet());
        List<Integer> levels = new ArrayList<>();
        for (int level : available) {
            if (selected.contains(String.valueOf(level))) {
                levels.add(level);
            }
        }
        return levels;
    }

    private void showBossPage(LinearLayout menu) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        CheckBox followLeader = addSettingsCheckBox(menu, R.string.boss_follow_leader,
                preferences.getBoolean(PREF_BOSS_FOLLOW_LEADER, false));
        followLeader.setOnCheckedChangeListener((button, checked) -> preferences.edit()
                .putBoolean(PREF_BOSS_FOLLOW_LEADER, checked)
                .apply());

        String choose = getString(R.string.boss_world_select_required);
        List<String> mapValues = new ArrayList<>();
        mapValues.add(choose);
        mapValues.addAll(WorldBossCatalog.mapNames());
        WorldBossCatalog.MapEntry savedMap = WorldBossCatalog.findMapByName(
                preferences.getString(PREF_WORLD_BOSS_MAP, ""));
        String savedMapDisplay = savedMap == null
                ? choose : savedMap.name + " " + savedMap.bossInfo();
        Spinner[] targetSpinner = new Spinner[1];
        addTextSpinnerRow(menu, R.string.boss_world_map,
                mapValues, savedMapDisplay, value -> {
                    WorldBossCatalog.MapEntry selected =
                            WorldBossCatalog.findMapByDisplayName(value);
                    if (selected == null) {
                        preferences.edit().remove(PREF_WORLD_BOSS_MAP)
                                .remove(PREF_WORLD_BOSS_TARGET).apply();
                        if (targetSpinner[0] != null) {
                            setSpinnerValues(targetSpinner[0],
                                    Collections.singletonList(choose), choose);
                        }
                        return;
                    }
                    WorldBossCatalog.BossEntry selectedBoss = selected.findBoss(
                            preferences.getString(PREF_WORLD_BOSS_TARGET, ""));
                    if (selectedBoss == null) {
                        selectedBoss = selected.bosses.get(0);
                    }
                    preferences.edit().putString(PREF_WORLD_BOSS_MAP, selected.name)
                            .putString(PREF_WORLD_BOSS_TARGET, selectedBoss.name).apply();
                    if (targetSpinner[0] != null) {
                        setSpinnerValues(targetSpinner[0], selected.bossNames(),
                                selectedBoss.displayName());
                    }
                });
        List<String> targetValues = savedMap == null
                ? Collections.singletonList(choose) : savedMap.bossNames();
        WorldBossCatalog.BossEntry savedBoss = savedMap == null ? null
                : savedMap.findBoss(preferences.getString(PREF_WORLD_BOSS_TARGET, ""));
        targetSpinner[0] = addTextSpinnerRow(menu, R.string.boss_world_target,
                targetValues, savedBoss == null ? choose : savedBoss.displayName(), value -> {
                    WorldBossCatalog.MapEntry selectedMap = WorldBossCatalog.findMapByName(
                            preferences.getString(PREF_WORLD_BOSS_MAP, ""));
                    WorldBossCatalog.BossEntry selectedBoss =
                            selectedMap == null ? null : selectedMap.findBoss(value);
                    if (selectedBoss != null) {
                        preferences.edit().putString(
                                PREF_WORLD_BOSS_TARGET, selectedBoss.name).apply();
                    }
                });

        addSettingsButtonRow(menu,
                createSettingsButton(R.string.boss_wilderness, true,
                        this::startWildernessBossFromMenu),
                createSettingsButton(R.string.boss_world, false,
                        this::startWorldBossFromMenu));
        addSettingsButtonRow(menu,
                createSettingsButton(R.string.settings_back, false, view -> showMainMenu()));
        updateMenuPosition();
    }

    private void showTimerPage() {
        LinearLayout menu = prepareSettingsPage(R.string.settings_timed_tasks);
        if (menu == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        addTimerRow(menu, R.string.timer_worship, PREF_HOUR, PREF_MINUTE,
                PREF_WORSHIP_ENABLED, true, 10, 30,
                () -> WorshipAlarmReceiver.scheduleWorship(this), this::startScheduledWorship);
        addTimerRow(menu, R.string.timer_military,
                PREF_MILITARY_HOUR, PREF_MILITARY_MINUTE,
                PREF_MILITARY_ENABLED, true, -1, -1,
                () -> WorshipAlarmReceiver.scheduleMilitary(this),
                this::startScheduledMilitary);
        CheckBox supply = addSettingsCheckBox(menu, R.string.timer_military_supply,
                preferences.getBoolean(PREF_MILITARY_SUPPLY_ENABLED, true));
        supply.setPadding(dp(40), 0, 0, 0);
        supply.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(PREF_MILITARY_SUPPLY_ENABLED, checked).apply();
            WorshipAlarmReceiver.scheduleMilitary(this);
        });
        CheckBox wilderness = addSettingsCheckBox(menu, R.string.timer_military_wilderness,
                preferences.getBoolean(PREF_MILITARY_WILDERNESS_ENABLED, true));
        wilderness.setPadding(dp(40), 0, 0, 0);
        wilderness.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(PREF_MILITARY_WILDERNESS_ENABLED, checked).apply();
            WorshipAlarmReceiver.scheduleMilitary(this);
        });
        addTimerRow(menu, R.string.timer_reward,
                PREF_WELFARE_HOUR, PREF_WELFARE_MINUTE,
                PREF_WELFARE_ENABLED, true, 12, 5,
                () -> WorshipAlarmReceiver.scheduleWelfare(this),
                this::startScheduledWelfare);
        addTimerRow(menu, R.string.timer_legion_reward,
                PREF_LEGION_HOUR, PREF_LEGION_MINUTE,
                PREF_LEGION_REWARD_ENABLED, true, 10, 10,
                () -> WorshipAlarmReceiver.scheduleLegionReward(this),
                this::startScheduledLegionReward);
        addTimerRow(menu, R.string.timer_dungeon,
                PREF_DUNGEON_HOUR, PREF_DUNGEON_MINUTE,
                PREF_DUNGEON_ENABLED, true, 14, 0,
                () -> WorshipAlarmReceiver.scheduleDungeon(this),
                this::startScheduledDungeon);
        addTimerRow(menu, R.string.timer_heavenfall,
                PREF_HEAVENFALL_HOUR, PREF_HEAVENFALL_MINUTE,
                PREF_HEAVENFALL_ENABLED, true, 22, 0,
                () -> WorshipAlarmReceiver.scheduleHeavenfall(this),
                this::startScheduledHeavenfall);
        addSpinnerRow(menu, R.string.heavenfall_duration, 60,
                preferences.getInt(PREF_HEAVENFALL_DURATION_MINUTES, 5),
                value -> preferences.edit()
                        .putInt(PREF_HEAVENFALL_DURATION_MINUTES, value).apply());
        addSpinnerRow(menu, R.string.heavenfall_zone, 15,
                preferences.getInt(PREF_HEAVENFALL_ZONE, 1),
                value -> preferences.edit().putInt(PREF_HEAVENFALL_ZONE, value).apply());
        addSettingsButtonRow(menu,
                createSettingsButton(R.string.settings_back, false, view -> showMainMenu()));
        updateMenuPosition();
    }

    private void showAuxiliarySettings() {
        LinearLayout menu = prepareSettingsPage(R.string.settings_auxiliary);
        if (menu == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        CheckBox autoStart = addSettingsCheckBox(menu, R.string.training_start_after_restart,
                preferences.getBoolean(PREF_START_TRAINING_ON_LAUNCH, true));
        autoStart.setOnCheckedChangeListener((button, checked) -> preferences.edit()
                .putBoolean(PREF_START_TRAINING_ON_LAUNCH, checked)
                .apply());

        boolean sellEnabled = preferences.getBoolean(PREF_AUTO_SELL_ENABLED, false);
        CheckBox autoSell = addSettingsCheckBox(menu, R.string.training_auto_sell, sellEnabled);

        LinearLayout slotsWrap = new LinearLayout(this);
        slotsWrap.setOrientation(LinearLayout.VERTICAL);
        slotsWrap.setPadding(dp(40), 0, 0, 0);
        Spinner slots = addSpinnerRow(slotsWrap, R.string.training_auto_sell_min_free_slots, 20,
                preferences.getInt(PREF_AUTO_SELL_MIN_FREE_SLOTS, 5),
                value -> preferences.edit()
                        .putInt(PREF_AUTO_SELL_MIN_FREE_SLOTS, value).apply());
        menu.addView(slotsWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        slotsWrap.setAlpha(sellEnabled ? 1f : 0.4f);
        slots.setEnabled(sellEnabled);

        autoSell.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(PREF_AUTO_SELL_ENABLED, checked).apply();
            slotsWrap.setAlpha(checked ? 1f : 0.4f);
            slots.setEnabled(checked);
            if (checked) {
                startInventoryMonitoring();
            } else {
                stopInventoryMonitoring();
            }
        });

        CheckBox autoRevival = addSettingsCheckBox(menu, R.string.soldier_revival_auto,
                preferences.getBoolean(PREF_SOLDIER_REVIVAL_ENABLED, false));
        autoRevival.setOnCheckedChangeListener((button, checked) -> preferences.edit()
                .putBoolean(PREF_SOLDIER_REVIVAL_ENABLED, checked)
                .apply());

        addSettingsButtonRow(menu,
                createSettingsButton(R.string.menu_soldier_revival, false,
                        view -> soldierRevivalAutomation.start()),
                createSettingsButton(R.string.settings_login, false,
                        view -> showLoginSettings()));
        addSettingsButtonRow(menu,
                createSettingsButton(R.string.settings_back, false, view -> showMainMenu()));
        updateMenuPosition();
    }

    private CheckBox addSettingsCheckBox(LinearLayout menu, int textResource, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(textResource);
        box.setTextColor(Color.WHITE);
        box.setTextSize(14);
        tintCheckable(box);
        box.setChecked(checked);
        menu.addView(box, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        return box;
    }

    private void addTimerRow(LinearLayout menu, int titleResource,
            String hourKey, String minuteKey, String enabledKey, boolean defaultEnabled,
            int defaultHour, int defaultMinute,
            Runnable reschedule, Runnable runNow) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox enabled = new CheckBox(this);
        tintCheckable(enabled);
        Button timer = createSettingsButton(titleResource, false, null);
        timer.setTextSize(13);
        Button execute = createSettingsButton(R.string.timer_run_now, false,
                runNow == null ? null : view -> runNow.run());
        execute.setTextSize(13);

        if (hourKey == null) {
            enabled.setEnabled(false);
            timer.setEnabled(false);
            execute.setEnabled(false);
            timer.setAlpha(0.4f);
            execute.setAlpha(0.4f);
        } else {
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            enabled.setChecked(preferences.getBoolean(enabledKey, defaultEnabled));
            enabled.setOnCheckedChangeListener((button, checked) -> {
                preferences.edit().putBoolean(enabledKey, checked).apply();
                reschedule.run();
            });
            int hour = preferences.getInt(hourKey, defaultHour);
            int minute = preferences.getInt(minuteKey, defaultMinute);
            if (hour < 0 || minute < 0) {
                timer.setText(getString(R.string.timer_task_unset,
                        getString(titleResource)));
            } else {
                setTimerButtonText(timer, titleResource, hour, minute);
            }
            timer.setOnClickListener(view -> showClock(timer, titleResource,
                    hourKey, minuteKey, defaultHour, defaultMinute, reschedule));
        }

        row.addView(enabled, new LinearLayout.LayoutParams(dp(40), dp(40)));
        row.addView(timer, new LinearLayout.LayoutParams(dp(300), dp(38)));
        LinearLayout.LayoutParams executeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        executeParams.leftMargin = dp(10);
        row.addView(execute, executeParams);
        menu.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
    }

    private void setTimerButtonText(TextView view, int titleResource, int hour, int minute) {
        view.setText(getString(R.string.timer_task_time,
                getString(titleResource), hour, minute));
    }

    private void showClock(TextView timeView, int titleResource,
            String hourKey, String minuteKey, int defaultHour, int defaultMinute,
            Runnable reschedule) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        int hour = preferences.getInt(hourKey,
                defaultHour >= 0 ? defaultHour : now.get(Calendar.HOUR_OF_DAY));
        int minute = preferences.getInt(minuteKey,
                defaultMinute >= 0 ? defaultMinute : now.get(Calendar.MINUTE));
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
        rewardAutomation.startWorship();
    }

    void startScheduledLegionReward() {
        rewardAutomation.startLegionReward();
    }

    void startScheduledWelfare() {
        welfareAutomation.start();
    }

    void startScheduledHeavenfall() {
        heavenfallAutomation.start();
    }

    void startScheduledDungeon() {
        startDungeonSweep(true);
    }

    void startScheduledMilitary() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!WorshipAlarmReceiver.shouldScheduleMilitary(
                preferences.getBoolean(PREF_MILITARY_ENABLED, true),
                preferences.getBoolean(PREF_MILITARY_SUPPLY_ENABLED, true),
                preferences.getBoolean(PREF_MILITARY_WILDERNESS_ENABLED, true))) {
            DiagnosticLog.info("MILITARY", "skipped; disabled or no quest selected");
            return;
        }
        if (!shouldRunMilitary(primaryTask)) {
            DiagnosticLog.info("MILITARY",
                    "skipped; primary task=" + primaryTaskLabel(primaryTask));
            return;
        }
        taskAutomation.start();
    }

    private void startScheduledAutomation(String progress, Runnable firstAction) {
        submitGameTask(AutomationTaskManager.Kind.INTERRUPT,
                taskKey(progress), progress, firstAction, firstAction);
    }

    private void startTrainingPrimaryTask(Runnable firstAction) {
        submitManagedTask(AutomationTaskManager.Kind.PRIMARY,
                "primary:" + PrimaryTask.TRAINING.name(), "自动练级：打开游戏", () -> {
                    setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
                    beginGameAutomation(
                            "自动练级：打开游戏", firstAction, firstAction);
                });
    }

    void startLoginOnly() {
        submitGameTask(AutomationTaskManager.Kind.IDLE_ONLY,
                "login", "登录：打开游戏", this::completeAutomation, null);
    }

    private void beginGameAutomation(
            String progress, Runnable afterLogin, Runnable recoveryAction) {
        showProgress(progress);

        Intent intent = getPackageManager().getLaunchIntentForPackage("hk.phx.khm.cs");
        if (intent == null) {
            failAutomation("Game package is unavailable");
            return;
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            currentAutomationAction = recoveryAction;
            postDelayed(() -> loginAutomation.start(afterLogin), 10_000);
        } catch (RuntimeException error) {
            DiagnosticLog.error("AUTOMATION", "Unable to open game for scheduled task", error);
            failAutomation("Unable to open game");
        }
    }

    @Override
    public Context context() {
        return this;
    }

    @Override
    public boolean isAutomationRunning() {
        return taskManager.hasCurrent();
    }

    @Override
    public long currentRunId() {
        AutomationTaskManager.Run run = taskManager.current();
        return run == null ? 0 : run.id;
    }

    @Override
    public boolean isRunCurrent(long runId) {
        return taskManager.isCurrent(runId);
    }

    @Override
    public void startAutomation(String progress, Runnable firstAction) {
        startScheduledAutomation(progress, firstAction);
    }

    @Override
    public void startInGameAutomation(String progress, Runnable firstAction) {
        submitManagedTask(AutomationTaskManager.Kind.INTERRUPT,
                taskKey(progress), progress, () -> {
                    showProgress(progress);
                    currentAutomationAction = firstAction;
                    firstAction.run();
                });
    }

    @Override
    public void startIdleAutomation(String progress, Runnable firstAction) {
        submitGameTask(AutomationTaskManager.Kind.IDLE_ONLY,
                taskKey(progress), progress, firstAction, firstAction);
    }

    @Override
    public void startPrimaryAutomation(
            PrimaryTask task, String progress, Runnable firstAction) {
        submitManagedTask(AutomationTaskManager.Kind.PRIMARY,
                "primary:" + task.name(), progress, () -> {
                    setPrimaryTask(task, firstAction);
                    beginGameAutomation(progress, firstAction, firstAction);
                    if (task == PrimaryTask.BOSS) {
                        startInventoryMonitoring();
                    }
                });
    }

    @Override
    public void postDelayed(Runnable action, long delayMillis) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            return;
        }
        handler.postAtTime(() -> {
            if (taskManager.isCurrent(run.id)) {
                action.run();
            }
        }, run, SystemClock.uptimeMillis() + delayMillis);
    }

    private void submitGameTask(AutomationTaskManager.Kind kind,
            String key, String progress, Runnable afterLogin, Runnable recoveryAction) {
        submitManagedTask(kind, key, progress,
                () -> beginGameAutomation(progress, afterLogin, recoveryAction));
    }

    private void submitManagedTask(AutomationTaskManager.Kind kind,
            String key, String progress, Runnable start) {
        boolean accepted = taskManager.submit(
                new AutomationTaskManager.Request(key, progress, kind, start));
        if (accepted) {
            return;
        }
        if (kind == AutomationTaskManager.Kind.INTERRUPT) {
            DiagnosticLog.info("TASK", "coalesced duplicate task=" + key);
        } else {
            showProgress("已有任务正在运行，请先终止");
        }
    }

    private void beginManagedRun(AutomationTaskManager.Run run) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(PREF_MANUALLY_STOPPED, false)
                .apply();
        automationRunning = true;
        failingRunId = 0;
        if (!run.request.preserveRecovery) {
            automationRecoveryAttempts = 0;
        }
        setTaskState(STATE_RUNNING);
        closeMenu();
        DiagnosticLog.info("TASK", "start run=" + run.id + " key=" + run.request.key
                + " kind=" + run.request.kind);
        try {
            run.request.start.run();
        } catch (RuntimeException error) {
            DiagnosticLog.error("TASK", "Unable to start " + run.request.key, error);
            failAutomation("Unable to start " + run.request.key);
        }
    }

    private void cancelManagedRun(AutomationTaskManager.Run run) {
        DiagnosticLog.info("TASK", "cancel run=" + run.id + " key=" + run.request.key);
        retireManagedRun(run);
        automationRunning = false;
        stopInventoryMonitoring();
        currentAutomationAction = null;
        failingRunId = 0;
    }

    private void retireManagedRun(AutomationTaskManager.Run run) {
        handler.removeCallbacksAndMessages(run);
        if (paddleDungeonTextRecognizer != null) {
            paddleDungeonTextRecognizer.cancelPending();
        }
    }

    private static String taskKey(String progress) {
        int separator = progress.indexOf('：');
        return separator < 0 ? progress : progress.substring(0, separator);
    }

    @Override
    public void enterTraining(long nextMilitaryAt) {
        clearAutomationRecovery();
        automationRunning = taskManager.hasCurrent();
        setTaskState(STATE_RUNNING);
        showProgress(nextMilitaryAt > 0
                ? "自动练级中 · 下次军务 " + formatTime(nextMilitaryAt)
                : "自动练级中");
        startInventoryMonitoring();
    }

    @Override
    public void enterActiveTraining(long nextMilitaryAt) {
        automationRunning = true;
        automationRecoveryAttempts = 0;
        setTaskState(STATE_RUNNING);
        showProgress(nextMilitaryAt > 0
                ? "为他人拉怪中 · 下次军务 " + formatTime(nextMilitaryAt)
                : "为他人拉怪中");
        startInventoryMonitoring();
    }

    @Override
    public void checkInventoryBeforePrimary(Runnable next) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = preferences.getBoolean(PREF_AUTO_SELL_ENABLED, false);
        if (!shouldCheckInventoryBeforePrimary(enabled, primaryTask)) {
            next.run();
            return;
        }
        showProgress("启动前检查背包");
        int minimumFreeSlots = preferences.getInt(PREF_AUTO_SELL_MIN_FREE_SLOTS, 5);
        ensureGameHudVisible(() -> autoSellAutomation.checkNearlyFullBeforeStart(
                minimumFreeSlots, nearlyFull -> {
                    if (taskState != STATE_RUNNING) {
                        return;
                    }
                    if (Boolean.TRUE.equals(nearlyFull)) {
                        interruptForAutoSell();
                    } else {
                        if (nearlyFull == null) {
                            showProgress("启动前未识别到背包容量，跳过本次检查");
                        }
                        next.run();
                    }
                }));
    }

    static boolean shouldCheckInventoryBeforePrimary(boolean enabled, PrimaryTask task) {
        return enabled && (task == PrimaryTask.TRAINING || task == PrimaryTask.BOSS);
    }

    @Override
    public String formatTime(long value) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(value));
    }

    @Override
    public void returnHome(Runnable next) {
        showProgress("自动军务：回城前停止自动攻击");
        ensureAutoAttackDisabled(() -> performReturnHome(next));
    }

    private void performReturnHome(Runnable next) {
        showProgress("自动军务：回城（1/2）");
        performTap(1210, 640, () -> {
            showProgress("自动军务：回城（2/2）");
            performTap(1210, 640, next);
        });
    }

    private void performTap(int x, int y, Runnable next) {
        performTap(x, y, next, CLICK_DELAY_MS);
    }

    private void performTap(int x, int y, Runnable next, int nextDelayMillis) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            return;
        }
        boolean accepted = tap(x, y, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                if (taskManager.isCurrent(run.id)) {
                    scheduleNext(next, nextDelayMillis);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                if (taskManager.isCurrent(run.id)) {
                    failAutomation("Tap was cancelled");
                }
            }
        });
        if (!accepted && taskManager.isCurrent(run.id)) {
            failAutomation("Tap was rejected");
        }
    }

    private void performSwipe(int startX, int startY, int endX, int endY, Runnable next) {
        performSwipe(startX, startY, endX, endY, 300, next);
    }

    private void performSwipe(int startX, int startY, int endX, int endY,
            long durationMillis, Runnable next) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            return;
        }
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMillis))
                .build();
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                if (taskManager.isCurrent(run.id)) {
                    scheduleNext(next);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                if (taskManager.isCurrent(run.id)) {
                    failAutomation("Swipe was cancelled");
                }
            }
        }, handler);
        if (!accepted && taskManager.isCurrent(run.id)) {
            failAutomation("Swipe was rejected");
        }
    }

    private void scheduleNext(Runnable next) {
        scheduleNext(next, CLICK_DELAY_MS);
    }

    private void scheduleNext(Runnable next, int delayMillis) {
        postDelayed(next, delayMillis);
    }

    @Override
    public void tap(int x, int y, Runnable next) {
        performTap(x, y, next);
    }

    @Override
    public void tapFast(int x, int y, Runnable next) {
        performTap(x, y, next, 200);
    }

    @Override
    public void setTextAt(int x, int y, String value, Runnable next) {
        performTap(x, y, () -> setFocusedText(value, next), 500);
    }

    private void setFocusedText(String value, Runnable next) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo field = root == null
                ? null
                : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) {
            failAutomation("登录：未找到可编辑的输入框");
            return;
        }

        Bundle arguments = new Bundle();
        arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            failAutomation("登录：无法写入输入框");
            return;
        }
        performTap(1200, 668, next, 500);
    }

    @Override
    public void swipe(int startX, int startY, int endX, int endY, Runnable next) {
        performSwipe(startX, startY, endX, endY, next);
    }

    @Override
    public void swipe(int startX, int startY, int endX, int endY,
            long durationMillis, Runnable next) {
        performSwipe(startX, startY, endX, endY, durationMillis, next);
    }

    @Override
    public void pressBack(Runnable next) {
        if (!automationRunning) {
            return;
        }
        if (!performGlobalAction(GLOBAL_ACTION_BACK)) {
            failAutomation("返回键执行失败");
            return;
        }
        scheduleNext(next, 300);
    }

    @Override
    public void ensureAutoAttackDisabled(Runnable next) {
        ensureGameHudVisible(
                () -> ensureAutoAttackState(false, next, SCREEN_WAIT_RETRY_COUNT));
    }

    @Override
    public void ensureAutoAttackEnabled(Runnable next) {
        ensureAutoAttackState(true, next, SCREEN_WAIT_RETRY_COUNT);
    }

    @Override
    public void ensureGameHudVisible(Runnable next) {
        ensureGameHudVisible(next, TEXT_RETRY_COUNT);
    }

    @Override
    public void claimWelfare(Runnable next) {
        welfareAutomation.startInGame(next);
    }

    private void ensureGameHudVisible(Runnable next, int remainingAttempts) {
        if (!automationRunning) {
            return;
        }
        int attempt = TEXT_RETRY_COUNT - remainingAttempts + 1;
        if (menuView != null) {
            DiagnosticLog.info("SCREEN_GUARD", "state=ASSISTANT_MENU action=close attempt="
                    + attempt);
            showProgress("准备游戏画面：关闭助手菜单");
            closeMenu();
            retryGameHudGuard(next, remainingAttempts, "助手菜单未关闭");
            return;
        }
        showProgress("准备游戏画面：检查遮挡窗口");
        closeBlockingWindowIfNeeded(next, remainingAttempts, attempt);
    }

    private void closeBlockingWindowIfNeeded(
            Runnable next, int remainingAttempts, int attempt) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            List<String> values = new ArrayList<>();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    values.add(line.getText());
                }
            }
            ScreenGuard.Blocker blocker = ScreenGuard.blockerFor(values);
            DiagnosticLog.info("SCREEN_GUARD", "state=" + blocker
                    + " attempt=" + attempt);
            if (blocker == ScreenGuard.Blocker.DUPLICATE_LOGIN) {
                failGameHudGuard("相同账号已在其他设备上登录");
                return;
            }
            if (blocker == ScreenGuard.Blocker.GAME_WINDOW) {
                DiagnosticLog.info("SCREEN_GUARD", "state=GAME_WINDOW action=back_3");
                showProgress("准备游戏画面：连续返回三次");
                pressBackThreeTimesThen(next, 3);
                return;
            }
            if (remainingAttempts <= 1 && blocker != ScreenGuard.Blocker.NONE) {
                failGameHudGuard("连续尝试后仍无法关闭遮挡窗口：" + blocker);
                return;
            }
            if (blocker == ScreenGuard.Blocker.DISCONNECTED) {
                DiagnosticLog.info("SCREEN_GUARD", "state=DISCONNECTED action=reconnect");
                showProgress("准备游戏画面：重连地图服务器");
                clickScreenText("确定", true,
                        () -> postDelayed(
                                () -> ensureGameHudVisible(
                                        next, remainingAttempts - 1),
                                8_000),
                        3, () -> failGameHudGuard("地图掉线弹窗无法确认"));
                return;
            }
            if (blocker == ScreenGuard.Blocker.DEFEATED) {
                DiagnosticLog.info("SCREEN_GUARD", "state=DEFEATED action=confirm");
                showProgress("Preparing game screen: closing defeated dialog");
                clickScreenText("确定", true,
                        () -> retryGameHudGuard(next, remainingAttempts,
                                "角色被击倒提示未关闭"),
                        3, () -> retryGameHudGuard(next, remainingAttempts,
                                "角色被击倒提示无法确认"));
                return;
            }
            if (blocker == ScreenGuard.Blocker.ANTI_CHEAT) {
                DiagnosticLog.info("SCREEN_GUARD", "state=ANTI_CHEAT action=solve");
                hudAntiCheatVerification.checkThen(
                        () -> retryGameHudGuard(next, remainingAttempts,
                                "反外挂验证未清除"));
                return;
            }
            LoginAutomation.Screen screen = LoginAutomation.screenFor(values);
            if (screen == LoginAutomation.Screen.REWARD_RECOVERY) {
                if (remainingAttempts > 1) {
                    DiagnosticLog.info("SCREEN_GUARD",
                            "state=REWARD_RECOVERY action=back");
                    showProgress("准备游戏画面：关闭奖励找回");
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    postDelayed(() -> ensureGameHudVisible(
                            next, remainingAttempts - 1), ACTION_DELAY_MS);
                } else {
                    failGameHudGuard("奖励找回窗口未关闭");
                }
            } else if (screen == LoginAutomation.Screen.WELFARE) {
                if (remainingAttempts > 1) {
                    DiagnosticLog.info("SCREEN_GUARD", "state=" + screen
                            + " action=close");
                    closeWelfareWindow(() -> ensureGameHudVisible(
                            next, remainingAttempts - 1));
                } else {
                    failGameHudGuard("福利窗口未关闭");
                }
            } else if (screen == LoginAutomation.Screen.UNCLAIMED_REWARDS) {
                if (remainingAttempts > 1) {
                    DiagnosticLog.info("SCREEN_GUARD",
                            "state=UNCLAIMED_REWARDS action=close_then_claim");
                    closeWelfareWindow(
                            () -> ensureGameHudVisible(next, remainingAttempts - 1));
                } else {
                    failGameHudGuard("未领取奖励窗口未关闭");
                }
            } else if (screen == LoginAutomation.Screen.LOGGED_IN) {
                DiagnosticLog.info("SCREEN_GUARD", "state=CLEAN_HUD action=continue");
                next.run();
            } else if (screen == LoginAutomation.Screen.UNKNOWN) {
                DiagnosticLog.info("SCREEN_GUARD", "state=UNKNOWN action=back_3");
                showProgress("准备游戏画面：连续返回三次");
                pressBackThreeTimesThen(next, 3);
            } else {
                DiagnosticLog.info("SCREEN_GUARD", "state=" + screen
                        + " action=login");
                loginAutomation.start(next);
            }
        });
    }

    private void pressBackThreeTimesThen(Runnable next, int remaining) {
        if (!automationRunning) {
            return;
        }
        if (remaining == 0) {
            DiagnosticLog.info("SCREEN_GUARD", "state=BACK_3_COMPLETE action=continue");
            next.run();
            return;
        }
        boolean sent = performGlobalAction(GLOBAL_ACTION_BACK);
        DiagnosticLog.info("SCREEN_GUARD", "state=BACK action="
                + (sent ? "sent" : "failed") + " remaining=" + (remaining - 1));
        postDelayed(
                () -> pressBackThreeTimesThen(next, remaining - 1),
                ACTION_DELAY_MS);
    }

    private void retryGameHudGuard(
            Runnable next, int remainingAttempts, String failureMessage) {
        if (remainingAttempts > 1) {
            postDelayed(
                    () -> ensureGameHudVisible(next, remainingAttempts - 1),
                    ACTION_DELAY_MS);
        } else {
            failGameHudGuard(failureMessage);
        }
    }

    private void failGameHudGuard(String message) {
        currentAutomationAction = null;
        failAutomation("游戏画面恢复失败：" + message);
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
                        postDelayed(
                                () -> ensureAutoAttackState(
                                        targetEnabled, next, remainingAttempts - 1),
                                ACTION_DELAY_MS);
                    } else {
                        failAutomation("Unable to detect auto attack state");
                    }
                } else if (enabled == targetEnabled) {
                    next.run();
                } else {
                    if (remainingAttempts > 1) {
                        showProgress(targetEnabled ? "开启自动攻击" : "停止自动攻击");
                        performTap(1210, 480,
                                () -> ensureAutoAttackState(
                                        targetEnabled, next, remainingAttempts - 1));
                    } else {
                        failAutomation("Unable to change auto attack state");
                    }
                }
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        });
    }

    private void closeAutoPathPanelIfNeeded(Runnable next) {
        closeAutoPathPanelIfNeeded(next, TEXT_RETRY_COUNT);
    }

    private void closeAutoPathPanelIfNeeded(Runnable next, int remainingAttempts) {
        recognizeTextRegion(930, 0, 1280, 600, text -> {
            if (!automationRunning) {
                return;
            }
            if (hasAutoPathPanel(text)) {
                if (remainingAttempts > 0) {
                    showProgress("关闭自动寻路栏");
                    performTap(1248, 147,
                            () -> closeAutoPathPanelIfNeeded(
                                    next, remainingAttempts - 1));
                } else {
                    failAutomation("Auto path panel did not close after retries");
                }
            } else {
                next.run();
            }
        });
    }

    @Override
    public void closeAutoPathPanel(Runnable next) {
        closeAutoPathPanelIfNeeded(next);
    }

    @Override
    public void openAutoPathPanel(Runnable next) {
        showProgress("打开自动寻路栏");
        performTap(1150, 500,
                () -> waitForAutoPathPanel(next, TEXT_RETRY_COUNT));
    }

    private void waitForAutoPathPanel(Runnable next, int remainingAttempts) {
        recognizeTextRegion(930, 0, 1280, 600, text -> {
            if (!automationRunning) {
                return;
            }
            if (hasAutoPathPanel(text)) {
                next.run();
            } else if (remainingAttempts > 1) {
                postDelayed(
                        () -> waitForAutoPathPanel(next, remainingAttempts - 1),
                        ACTION_DELAY_MS);
            } else {
                failAutomation("自动寻路栏未打开");
            }
        });
    }

    private boolean hasAutoPathPanel(OcrText text) {
        List<String> values = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                values.add(line.getText());
            }
        }
        return hasAutoPathPanelLabels(values);
    }

    static boolean hasAutoPathPanelLabels(List<String> values) {
        return ScreenGuard.hasAutoPathPanel(values);
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

    private void recognizeScreenText(Consumer<OcrText> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                failAutomation("Unable to capture screen for text recognition");
                return;
            }
            recognizeText(bitmap, text -> {
                bitmap.recycle();
                result.accept(text);
            }, error -> {
                bitmap.recycle();
                DiagnosticLog.error("OCR", "Paddle text recognition failed", error);
                failAutomation("Paddle text recognition failed");
            });
        });
    }

    @Override
    public void recognizeText(Consumer<OcrText> result) {
        recognizeScreenText(result);
    }

    @Override
    public void recognizeText(Bitmap bitmap, Consumer<OcrText> result,
            Consumer<Throwable> failure) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            recycleIfNeeded(bitmap);
            return;
        }
        if (paddleDungeonTextRecognizer == null) {
            paddleDungeonTextRecognizer = new PaddleDungeonTextRecognizer(this);
        }
        paddleDungeonTextRecognizer.recognize(bitmap,
                lines -> handler.post(() -> {
                    if (taskManager.isCurrent(run.id)) {
                        result.accept(new OcrText(lines));
                    } else {
                        recycleIfNeeded(bitmap);
                    }
                }),
                error -> handler.post(() -> {
                    if (taskManager.isCurrent(run.id)) {
                        failure.accept(error);
                    } else {
                        recycleIfNeeded(bitmap);
                    }
                }));
    }

    private static void recycleIfNeeded(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @Override
    public void recognizeDungeonText(Consumer<List<OcrLine>> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                failAutomation("Unable to capture screen for Paddle OCR");
                return;
            }
            recognizeText(bitmap, text -> {
                bitmap.recycle();
                List<OcrLine> lines = new ArrayList<>();
                for (OcrText.TextBlock block : text.getTextBlocks()) {
                    for (OcrText.Line line : block.getLines()) {
                        lines.add(new OcrLine(
                                line.getText(), line.getBoundingBox(), 0f));
                    }
                }
                result.accept(lines);
            }, error -> {
                bitmap.recycle();
                DiagnosticLog.error("OCR", "Paddle OCR failed", error);
                failAutomation("Paddle OCR failed");
            });
        });
    }

    @Override
    public void recognizeRedBoss(Consumer<BossAutomation.BossTarget> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept(null);
                return;
            }
            int left = 920 * bitmap.getWidth() / 1280;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, 0, bitmap.getWidth() - left, bitmap.getHeight());
            bitmap.recycle();
            int scale = 3;
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * scale,
                    cropped.getHeight() * scale, true);

            recognizeText(enlarged, text -> {
                result.accept(BossAutomation.findRedBoss(
                        text, cropped, left, scale));
                enlarged.recycle();
                cropped.recycle();
            }, error -> {
                enlarged.recycle();
                cropped.recycle();
                DiagnosticLog.error("OCR", "Red boss recognition failed", error);
                failAutomation("红名 BOSS 识别失败");
            });
        });
    }

    @Override
    public void recognizeHudChannel(Bitmap screenshot, Consumer<Integer> result) {
        if (screenshot == null) {
            result.accept(null);
            return;
        }
        int left = 1190 * screenshot.getWidth() / 1280;
        int top = 690 * screenshot.getHeight() / 720;
        int right = 1250 * screenshot.getWidth() / 1280;
        int bottom = 716 * screenshot.getHeight() / 720;
        Bitmap context = Bitmap.createBitmap(
                screenshot, left, top, right - left, bottom - top);
        screenshot.recycle();
        Bitmap enlarged = Bitmap.createScaledBitmap(
                context, context.getWidth() * 5, context.getHeight() * 5, true);
        context.recycle();
        recognizeText(enlarged, text -> {
            enlarged.recycle();
            Integer channel = parseHudChannelContext(text.getText());
            DiagnosticLog.info("BOSS", "HUD channel context Paddle OCR='"
                    + text.getText().replace('\n', ' ') + "' parsed=" + channel);
            result.accept(channel);
        }, error -> {
            enlarged.recycle();
            DiagnosticLog.warn("BOSS", "HUD channel context Paddle OCR failed: "
                    + error.getMessage());
            result.accept(null);
        });
    }

    @Override
    public void recognizeLeaderChannel(Consumer<Integer> result) {
        captureScreenshot(screenshot -> {
            if (screenshot == null) {
                result.accept(null);
                return;
            }
            int left = 850 * screenshot.getWidth() / 1280;
            int top = 195 * screenshot.getHeight() / 720;
            int right = 950 * screenshot.getWidth() / 1280;
            int bottom = 260 * screenshot.getHeight() / 720;
            Bitmap context = Bitmap.createBitmap(
                    screenshot, left, top, right - left, bottom - top);
            screenshot.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    context, context.getWidth() * 6, context.getHeight() * 6, true);
            context.recycle();
            recognizeText(enlarged, text -> {
                enlarged.recycle();
                Integer channel = BossAutomation.parseLeaderChannel(text.getText());
                DiagnosticLog.info("BOSS", "leader channel crop Paddle OCR='"
                        + text.getText().replace('\n', ' ')
                        + "' parsed=" + channel);
                result.accept(channel);
            }, error -> {
                enlarged.recycle();
                DiagnosticLog.warn("BOSS", "leader channel crop Paddle OCR failed: "
                        + error.getMessage());
                result.accept(null);
            });
        });
    }

    static Integer parseHudChannelContext(String value) {
        Integer channel = null;
        if (value != null) {
            for (int index = 0; index < value.length(); index++) {
                int digit = value.charAt(index) - '0';
                if (digit >= 1 && digit <= 8) {
                    channel = digit;
                }
            }
        }
        return channel;
    }

    @Override
    public void recognizeMapCoordinate(Consumer<String> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept("");
                return;
            }
            int left = 440 * bitmap.getWidth() / 1280;
            int top = 655 * bitmap.getHeight() / 720;
            int right = 850 * bitmap.getWidth() / 1280;
            int bottom = 700 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * 8, cropped.getHeight() * 8, true);
            cropped.recycle();

            recognizeText(enlarged, text -> {
                enlarged.recycle();
                String value = text.getText();
                DiagnosticLog.info("OCR",
                        "Map coordinate Paddle OCR='" + value.replace('\n', ' ') + "'");
                result.accept(value);
            }, error -> {
                enlarged.recycle();
                DiagnosticLog.warn("OCR",
                        "Map coordinate Paddle OCR failed: " + error.getMessage());
                result.accept("");
            });
        });
    }

    @Override
    public void recognizeMapName(Consumer<String> result) {
        recognizePaddleTextCrop(430, 550, 760, 615, 4, "Map name", result);
    }

    @Override
    public void recognizeWorldBossMap(Consumer<String> result) {
        recognizePaddleTextCrop(25, 165, 335, 315, 6, "World boss rows", result);
    }

    private void recognizePaddleTextCrop(int baseLeft, int baseTop, int baseRight,
            int baseBottom, int scale, String label, Consumer<String> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept("");
                return;
            }
            int left = baseLeft * bitmap.getWidth() / 1280;
            int top = baseTop * bitmap.getHeight() / 720;
            int right = baseRight * bitmap.getWidth() / 1280;
            int bottom = baseBottom * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * scale,
                    cropped.getHeight() * scale, true);
            cropped.recycle();
            recognizeText(enlarged, text -> {
                enlarged.recycle();
                StringBuilder value = new StringBuilder();
                for (OcrText.TextBlock block : text.getTextBlocks()) {
                    for (OcrText.Line line : block.getLines()) {
                        if (value.length() > 0) {
                            value.append(' ');
                        }
                        value.append(line.getText());
                    }
                }
                DiagnosticLog.info("BOSS", label + " Paddle OCR='" + value + "'");
                result.accept(value.toString());
            }, error -> {
                enlarged.recycle();
                DiagnosticLog.error("OCR", label + " Paddle OCR failed", error);
                result.accept("");
            });
        });
    }

    @Override
    public void recognizeBackpackCapacity(Consumer<String> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept("");
                return;
            }
            int left = 1078 * bitmap.getWidth() / 1280;
            int top = 76 * bitmap.getHeight() / 720;
            int right = 1181 * bitmap.getWidth() / 1280;
            int bottom = 97 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            recognizeBackpackCapacityPaddle(cropped, result);
        });
    }

    private void recognizeBackpackCapacityPaddle(
            Bitmap cropped, Consumer<String> result) {
        Bitmap enlarged = Bitmap.createScaledBitmap(
                cropped, cropped.getWidth() * 4, cropped.getHeight() * 4, true);
        cropped.recycle();
        recognizeText(enlarged, text -> {
            enlarged.recycle();
            StringBuilder raw = new StringBuilder();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    raw.append(line.getText());
                }
            }
            String value = raw.toString().replaceAll("[^0-9/]", "");
            DiagnosticLog.info("AUTO_SELL",
                    "Backpack Paddle OCR='" + value + "'");
            result.accept(value);
        }, error -> {
            enlarged.recycle();
            DiagnosticLog.warn("AUTO_SELL",
                    "Backpack Paddle OCR failed: " + error.getMessage());
            result.accept("");
        });
    }

    @Override
    public void recognizeYuanbaoQuickSell(Consumer<Boolean> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept(false);
                return;
            }
            int left = 395 * bitmap.getWidth() / 1280;
            int top = 595 * bitmap.getHeight() / 720;
            int right = 600 * bitmap.getWidth() / 1280;
            int bottom = 645 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * 4, cropped.getHeight() * 4, true);
            cropped.recycle();

            recognizeText(enlarged, text -> {
                enlarged.recycle();
                String value = text.getText();
                boolean matched = matchesYuanbaoQuickSell(value);
                DiagnosticLog.info("AUTO_SELL",
                        "quick sale ROI Paddle OCR='" + normalizeText(value)
                                + "' matched=" + matched);
                result.accept(matched);
            }, error -> {
                enlarged.recycle();
                DiagnosticLog.warn("AUTO_SELL",
                        "Quick sale ROI Paddle OCR failed: " + error.getMessage());
                result.accept(false);
            });
        });
    }

    static boolean matchesYuanbaoQuickSell(String value) {
        if (value == null) {
            return false;
        }
        String normalized = normalizeText(value);
        return normalized.contains("快速贩卖装备")
                || normalized.contains("快速") && normalized.contains("贩卖");
    }

    @Override
    public void clickQuickArrival(Runnable next) {
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

            recognizeText(enlarged, text -> {
                        enlarged.recycle();
                        List<OcrText.Line> fragments = new ArrayList<>();
                        for (OcrText.TextBlock block : text.getTextBlocks()) {
                            for (OcrText.Line line : block.getLines()) {
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
                        for (OcrText.Line fragment : fragments) {
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
                            postDelayed(
                                    () -> clickQuickArrival(next, remainingAttempts - 1),
                                    ACTION_DELAY_MS);
                        } else {
                            failAutomation("Screen text was not found: 快速抵达");
                        }
                    }, error -> {
                        enlarged.recycle();
                        DiagnosticLog.error(
                                "OCR", "Quick arrival Paddle OCR failed", error);
                        failAutomation("Paddle OCR failed: 快速抵达");
                    });
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

    @Override
    public void clickText(String expected, boolean exact, Runnable next, int attempts) {
        clickScreenText(expected, exact, next, attempts);
    }

    @Override
    public void clickText(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing) {
        clickScreenText(expected, exact, next, attempts, ifMissing);
    }

    @Override
    public void clickDungeonText(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing) {
        clickDungeonScreenText(expected, exact, next, attempts, ifMissing);
    }

    private void clickDungeonScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing) {
        if (!automationRunning) {
            return;
        }
        recognizeDungeonText(lines -> {
            if (!automationRunning) {
                return;
            }
            Rect match = findOcrTextBounds(lines, expected, exact);
            if (match != null) {
                performTap(match.centerX(), match.centerY(), next, CLICK_DELAY_MS);
            } else if (remainingAttempts > 1) {
                postDelayed(
                        () -> clickDungeonScreenText(expected, exact, next,
                                remainingAttempts - 1, ifMissing),
                        ACTION_DELAY_MS);
            } else if (ifMissing != null) {
                ifMissing.run();
            } else {
                failAutomation("Dungeon text was not found: " + expected);
            }
        });
    }

    @Override
    public void clickLeftText(String expected, Runnable next,
            int attempts, Runnable ifMissing) {
        clickLeftScreenText(expected, next, attempts, ifMissing);
    }

    @Override
    public void clickRightText(String expected, Runnable next,
            int attempts, Runnable ifMissing) {
        clickRightScreenText(expected, next, attempts, ifMissing);
    }

    @Override
    public void clickRightTextFast(String expected, Runnable next,
            int attempts, Runnable ifMissing) {
        clickScreenText(expected, false, next, attempts, ifMissing,
                850, Integer.MAX_VALUE, 500);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing, int minX) {
        clickScreenText(expected, exact, next, remainingAttempts, ifMissing,
                minX, Integer.MAX_VALUE);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing, int minX, int maxX) {
        clickScreenText(expected, exact, next, remainingAttempts, ifMissing,
                minX, maxX, CLICK_DELAY_MS);
    }

    private void clickScreenText(String expected, boolean exact, Runnable next,
            int remainingAttempts, Runnable ifMissing, int minX, int maxX,
            int nextDelayMillis) {
        if (!automationRunning) {
            return;
        }
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            String target = normalizeText(expected);
            List<OcrText.Line> lines = new ArrayList<>();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && isHorizontalMatch(
                            bounds.centerX(), minX, maxX)) {
                        lines.add(line);
                    }
                }
            }
            Rect match = findTextBounds(lines, target, exact);
            if (match != null) {
                performTap(match.centerX(), match.centerY(), next, nextDelayMillis);
                return;
            }
            if (remainingAttempts > 1) {
                postDelayed(
                        () -> clickScreenText(expected, exact, next,
                                remainingAttempts - 1, ifMissing, minX, maxX,
                                nextDelayMillis),
                        ACTION_DELAY_MS);
            } else if (ifMissing != null) {
                ifMissing.run();
            } else {
                failAutomation("Screen text was not found: " + expected);
            }
        });
    }

    private static Rect findTextBounds(List<OcrText.Line> lines, String target, boolean exact) {
        for (OcrText.Line line : lines) {
            if (matchesTextFragments(
                    Collections.singletonList(line.getText()), target, true)) {
                return line.getBoundingBox();
            }
        }
        if (!exact) {
            for (OcrText.Line line : lines) {
                if (matchesTextFragments(
                        Collections.singletonList(line.getText()), target, false)) {
                    return line.getBoundingBox();
                }
            }
        }
        lines.sort((left, right) -> Integer.compare(
                left.getBoundingBox().left, right.getBoundingBox().left));
        for (int start = 0; start < lines.size(); start++) {
            Rect anchor = lines.get(start).getBoundingBox();
            Rect mergedBounds = new Rect(anchor);
            List<String> fragments = new ArrayList<>();
            fragments.add(lines.get(start).getText());
            for (int index = start + 1; index < lines.size(); index++) {
                OcrText.Line line = lines.get(index);
                Rect bounds = line.getBoundingBox();
                if (Math.max(anchor.top, bounds.top) > Math.min(anchor.bottom, bounds.bottom)) {
                    continue;
                }
                if (bounds.left - mergedBounds.right > 80) {
                    break;
                }
                fragments.add(line.getText());
                mergedBounds.union(bounds);
                if (matchesTextFragments(fragments, target, exact)) {
                    return mergedBounds;
                }
            }
        }
        return null;
    }

    static Rect findOcrTextBounds(List<OcrLine> lines, String target, boolean exact) {
        for (OcrLine line : lines) {
            if (matchesTextFragments(
                    Collections.singletonList(line.text), target, exact)) {
                return line.bounds;
            }
        }
        lines.sort((left, right) -> Integer.compare(left.bounds.left, right.bounds.left));
        for (int start = 0; start < lines.size(); start++) {
            Rect anchor = lines.get(start).bounds;
            Rect mergedBounds = new Rect(anchor);
            List<String> fragments = new ArrayList<>();
            fragments.add(lines.get(start).text);
            for (int index = start + 1; index < lines.size(); index++) {
                OcrLine line = lines.get(index);
                if (Math.max(anchor.top, line.bounds.top)
                        > Math.min(anchor.bottom, line.bounds.bottom)) {
                    continue;
                }
                if (line.bounds.left - mergedBounds.right > 80) {
                    break;
                }
                fragments.add(line.text);
                mergedBounds.union(line.bounds);
                if (matchesTextFragments(fragments, target, exact)) {
                    return mergedBounds;
                }
            }
        }
        return null;
    }

    static boolean matchesTextFragments(
            List<String> fragments, String target, boolean exact) {
        StringBuilder combined = new StringBuilder();
        for (String fragment : fragments) {
            combined.append(normalizeText(fragment));
        }
        String value = combined.toString();
        String normalizedTarget = normalizeText(target);
        return exact ? value.equals(normalizedTarget) : value.contains(normalizedTarget);
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
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                for (OcrText.Line line : block.getLines()) {
                    if (normalizeText(line.getText()).contains(target)) {
                        next.run();
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                postDelayed(
                        () -> waitForScreenText(expected, remainingAttempts - 1, next),
                        ACTION_DELAY_MS);
            } else {
                failAutomation("Screen text was not found: " + expected);
            }
        });
    }

    @Override
    public void waitForText(String expected, int attempts, Runnable next) {
        waitForScreenText(expected, attempts, next);
    }

    @Override
    public void waitForMapReady(int attempts, Runnable next) {
        captureScreenshot(bitmap -> {
            if (bitmap == null || !automationRunning) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                return;
            }
            int left = 740 * bitmap.getWidth() / 1280;
            int top = 555 * bitmap.getHeight() / 720;
            int right = 860 * bitmap.getWidth() / 1280;
            int bottom = 620 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * 6, cropped.getHeight() * 6, true);
            cropped.recycle();
            recognizeText(enlarged, text -> {
                        enlarged.recycle();
                        String value = text.getText();
                        boolean ready = isMapButtonText(value);
                        DiagnosticLog.info("BOSS", "map button Paddle OCR='"
                                + normalizeText(value) + "' ready=" + ready);
                        if (ready) {
                            next.run();
                        } else {
                            retryMapReady(attempts, next);
                        }
                    }, error -> {
                        enlarged.recycle();
                        DiagnosticLog.warn("BOSS", "map button Paddle OCR failed: "
                                + error.getMessage());
                        retryMapReady(attempts, next);
                    });
        });
    }

    private void retryMapReady(int attempts, Runnable next) {
        if (attempts > 1) {
            postDelayed(() -> waitForMapReady(attempts - 1, next), ACTION_DELAY_MS);
        } else {
            failAutomation("传送后未检测到地图加载完成");
        }
    }

    static boolean isMapButtonText(String value) {
        return normalizeText(value == null ? "" : value).contains("地图");
    }

    private static String normalizeText(String value) {
        return value.replaceAll("\\s+", "")
                .replace('(', '（')
                .replace(')', '）');
    }

    @Override
    public void captureScreenshot(Consumer<Bitmap> result) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            result.accept(null);
            return;
        }
        captureScreenshot(run, result, TEXT_RETRY_COUNT);
    }

    private void captureScreenshot(AutomationTaskManager.Run run,
            Consumer<Bitmap> result, int remainingAttempts) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (taskManager.isCurrent(run.id)) {
                result.accept(null);
            }
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
                    if (taskManager.isCurrent(run.id)) {
                        result.accept(bitmap);
                        bitmap = null;
                    }
                } catch (RuntimeException error) {
                    DiagnosticLog.error("SCREENSHOT", "Unable to inspect screenshot", error);
                    if (taskManager.isCurrent(run.id)) {
                        result.accept(null);
                    }
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
                if (!taskManager.isCurrent(run.id)) {
                    return;
                }
                if (shouldRetryScreenshot(errorCode, remainingAttempts)
                        && automationRunning) {
                    DiagnosticLog.warn("SCREENSHOT", "busy; retrying, code=" + errorCode
                            + " remainingAttempts=" + (remainingAttempts - 1));
                    postDelayed(() -> captureScreenshot(
                            run, result, remainingAttempts - 1), ACTION_DELAY_MS);
                } else {
                    DiagnosticLog.error("SCREENSHOT", "failed, code=" + errorCode);
                    result.accept(null);
                }
            }
        });
    }

    static boolean shouldRetryScreenshot(int errorCode, int remainingAttempts) {
        return errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT
                && remainingAttempts > 1;
    }

    static boolean shouldRetryAutomation(int failedAttempts) {
        return failedAttempts <= MAX_AUTOMATION_RECOVERY_ATTEMPTS;
    }

    static boolean shouldRunMilitary(PrimaryTask task) {
        return task == PrimaryTask.TRAINING;
    }

    @Override
    public void completeAutomation() {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            return;
        }
        DiagnosticLog.info("AUTOMATION", "completed");
        boolean completedPrimaryDungeon = run.request.kind == AutomationTaskManager.Kind.PRIMARY
                && primaryTask == PrimaryTask.DUNGEON;
        stopInventoryMonitoring();
        clearAutomationRecovery();
        if (completedPrimaryDungeon) {
            resetPrimaryTaskToTraining();
        }
        retireManagedRun(run);
        taskManager.finish(run.id);
        automationRunning = taskManager.hasCurrent();
        if (!automationRunning) {
            hideProgress();
            setTaskState(STATE_COMPLETED);
        }
    }

    @Override
    public void resumePrimaryTask() {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null) {
            return;
        }
        inventorySelling = false;
        clearAutomationRecovery();
        retireManagedRun(run);
        taskManager.finish(run.id);
        automationRunning = taskManager.hasCurrent();
    }

    @Override
    public void failAutomation(String message) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null || failingRunId == run.id) {
            return;
        }
        failingRunId = run.id;
        DiagnosticLog.error("AUTOMATION", message);
        handler.removeCallbacksAndMessages(run);
        if (paddleDungeonTextRecognizer != null) {
            paddleDungeonTextRecognizer.cancelPending();
        }
        captureScreenshot(bitmap -> {
            try {
                DiagnosticLog.saveScreenshot(bitmap, "automation-error");
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                finishAutomationFailure(run.id, message);
            }
        });
    }

    private void finishAutomationFailure(long runId, String message) {
        AutomationTaskManager.Run run = taskManager.current();
        if (run == null || run.id != runId) {
            return;
        }
        if (run.request.kind != AutomationTaskManager.Kind.PRIMARY
                || currentAutomationAction == null) {
            inventorySelling = false;
            taskManager.abort(runId);
            automationRunning = taskManager.hasCurrent();
            if (automationRunning) {
                return;
            }
            automationRunning = false;
            setTaskState(STATE_STOPPED);
            showProgress("错误：" + message);
            return;
        }

        recoverPrimaryTask(run, message);
    }

    private void recoverPrimaryTask(AutomationTaskManager.Run failedRun, String message) {
        if (inventorySelling) {
            inventorySelling = false;
        }
        automationRecoveryAttempts++;
        boolean finalAttempt = !shouldRetryAutomation(automationRecoveryAttempts);
        Runnable recovery = primaryTaskAction;
        long delay = finalAttempt ? RECOVERY_LONG_DELAY_MS : RECOVERY_START_DELAY_MS;
        String taskLabel = primaryTaskLabel(primaryTask);
        AutomationTaskManager.Request retry = new AutomationTaskManager.Request(
                failedRun.request.key,
                failedRun.request.progress,
                AutomationTaskManager.Kind.PRIMARY,
                () -> {
                    currentAutomationAction = finalAttempt ? null : recovery;
                    setTaskState(STATE_RUNNING);
                    startInventoryMonitoring();
                    showProgress(finalAttempt
                            ? "错误：" + message + " · 连续失败，60秒后最后恢复"
                                    + taskLabel + "，不再自动重试"
                            : "错误：" + message + " · 5秒后恢复主要任务：" + taskLabel);
                    postDelayed(() -> {
                        if (recovery == null) {
                            failAutomation("没有可恢复的主要任务");
                            return;
                        }
                        showProgress("错误恢复：恢复主要任务 · " + taskLabel);
                        recovery.run();
                    }, delay);
                },
                true);
        taskManager.restart(failedRun.id, retry);
    }

    private void stopAutomation(int state) {
        DiagnosticLog.info("AUTOMATION", "stopped state=" + state);
        if (state == STATE_STOPPED) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(PREF_MANUALLY_STOPPED, true)
                    .apply();
        }
        automationRunning = false;
        inventorySelling = false;
        stopInventoryMonitoring();
        clearAutomationRecovery();
        resetPrimaryTaskToTraining();
        taskManager.stopAll();
        handler.removeCallbacksAndMessages(null);
        hideProgress();
        setTaskState(state);
    }

    private void clearAutomationRecovery() {
        automationRecoveryAttempts = 0;
        failingRunId = 0;
        currentAutomationAction = null;
    }

    private void resetPrimaryTaskToTraining() {
        setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
    }

    private void setPrimaryTask(PrimaryTask task, Runnable action) {
        primaryTask = task;
        primaryTaskAction = action;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_PRIMARY_TASK, task.name())
                .apply();
    }

    private void restorePrimaryTask() {
        primaryTask = parsePrimaryTask(getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_PRIMARY_TASK, PrimaryTask.TRAINING.name()));
        if (primaryTask == PrimaryTask.BOSS) {
            primaryTaskAction = bossAutomation::start;
        } else if (primaryTask == PrimaryTask.DUNGEON) {
            primaryTaskAction = this::restartDungeonTask;
        } else {
            primaryTaskAction = trainingAutomation::start;
        }
        DiagnosticLog.info("AUTOMATION", "restored primary task=" + primaryTaskLabel(primaryTask));
    }

    private void resumePersistedPrimaryTask() {
        if (!automationRunning && primaryTask != PrimaryTask.TRAINING
                && !getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(PREF_MANUALLY_STOPPED, false)) {
            restartPrimaryTask(primaryTask);
        }
    }

    private void startInventoryMonitoring() {
        stopInventoryMonitoring();
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!inventorySelling && taskState == STATE_RUNNING
                && preferences.getBoolean(PREF_AUTO_SELL_ENABLED, false)
                && (primaryTask == PrimaryTask.TRAINING || primaryTask == PrimaryTask.BOSS)) {
            inventoryHandler.postDelayed(this::checkInventory, INVENTORY_CHECK_INTERVAL_MS);
        }
    }

    private void stopInventoryMonitoring() {
        inventoryHandler.removeCallbacksAndMessages(null);
        inventoryCheckRunning = false;
    }

    private void checkInventory() {
        if (!canCheckInventory() || inventoryCheckRunning) {
            startInventoryMonitoring();
            return;
        }
        inventoryCheckRunning = true;
        int minimumFreeSlots = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PREF_AUTO_SELL_MIN_FREE_SLOTS, 5);
        autoSellAutomation.checkNearlyFull(minimumFreeSlots, nearlyFull -> {
            inventoryCheckRunning = false;
            if (!canCheckInventory()) {
                return;
            }
            if (nearlyFull) {
                interruptForAutoSell();
            } else {
                startInventoryMonitoring();
            }
        });
    }

    private boolean canCheckInventory() {
        if (inventorySelling || taskState != STATE_RUNNING
                || !getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(PREF_AUTO_SELL_ENABLED, false)) {
            return false;
        }
        if (primaryTask == PrimaryTask.TRAINING) {
            return taskManager.hasCurrent();
        }
        return primaryTask == PrimaryTask.BOSS
                && automationRunning && currentAutomationAction == primaryTaskAction;
    }

    private boolean isTrainingPullActive() {
        return primaryTask == PrimaryTask.TRAINING
                && automationRunning
                && getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(PREF_TRAINING_PULL_FOR_OTHERS, false);
    }

    private void interruptForAutoSell() {
        inventorySelling = true;
        stopInventoryMonitoring();
        showProgress("Auto sell: backpack is nearly full");
        // ponytail: restart the primary task after selling instead of resuming a stale callback.
        autoSellAutomation.start(() -> {
            inventorySelling = false;
            resumePrimaryTask();
        });
    }

    private void restartPrimaryTask(PrimaryTask task) {
        automationRunning = false;
        DiagnosticLog.info("AUTOMATION", "resuming primary task=" + primaryTaskLabel(task));
        if (task == PrimaryTask.BOSS) {
            bossAutomation.start();
        } else if (task == PrimaryTask.DUNGEON) {
            restartDungeonTask();
        } else {
            setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
            startTrainingPrimaryTask(trainingAutomation::start);
        }
    }

    private void startDungeonSweep(boolean scheduled) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(PREF_DUNGEON_BATTLE_MODE, false)
                .putBoolean(PREF_DUNGEON_ELITE_MODE, false).apply();
        List<Integer> levels = selectedDungeonLevels(preferences);
        if (scheduled) {
            dungeonSweepAutomation.startScheduled(levels);
        } else {
            dungeonSweepAutomation.start(levels);
        }
    }

    private void startDungeonBattle() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(PREF_DUNGEON_BATTLE_MODE, true)
                .putBoolean(PREF_DUNGEON_ELITE_MODE, false).apply();
        dungeonBattleAutomation.start(selectedBattleLevels(preferences));
    }

    private void startDungeonElite() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(PREF_DUNGEON_BATTLE_MODE, true)
                .putBoolean(PREF_DUNGEON_ELITE_MODE, true).apply();
        dungeonBattleAutomation.startElite(selectedEliteLevels(preferences));
    }

    private static List<Integer> selectedBattleLevels(SharedPreferences preferences) {
        return selectedDungeonLevels(preferences, PREF_DUNGEON_BATTLE_LEVELS,
                DungeonBattleAutomation.LEVELS);
    }

    private static List<Integer> selectedEliteLevels(SharedPreferences preferences) {
        return selectedDungeonLevels(preferences, PREF_DUNGEON_ELITE_LEVELS,
                DungeonBattleAutomation.ELITE_LEVELS);
    }

    private void restartDungeonTask() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean battleMode = preferences.getBoolean(PREF_DUNGEON_BATTLE_MODE, false);
        boolean eliteMode = battleMode
                && preferences.getBoolean(PREF_DUNGEON_ELITE_MODE, false);
        List<Integer> levels;
        if (eliteMode) {
            levels = selectedEliteLevels(preferences);
        } else if (battleMode) {
            levels = selectedBattleLevels(preferences);
        } else {
            levels = selectedDungeonLevels(preferences);
        }
        if (levels.isEmpty()) {
            resetPrimaryTaskToTraining();
            startTrainingPrimaryTask(trainingAutomation::start);
        } else if (eliteMode) {
            dungeonBattleAutomation.startElite(levels);
        } else if (battleMode) {
            dungeonBattleAutomation.start(levels);
        } else {
            dungeonSweepAutomation.start(levels);
        }
    }

    static PrimaryTask parsePrimaryTask(String value) {
        try {
            return PrimaryTask.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return PrimaryTask.TRAINING;
        }
    }

    private static String primaryTaskLabel(PrimaryTask task) {
        if (task == PrimaryTask.BOSS) {
            return "BOSS";
        }
        if (task == PrimaryTask.DUNGEON) {
            return "副本";
        }
        return "练级";
    }

    static String formatProgress(PrimaryTask task, String value) {
        return "【" + primaryTaskLabel(task) + "】 " + value;
    }

    @Override
    public void showProgress(String value) {
        String displayValue = formatProgress(primaryTask, value);
        DiagnosticLog.info("PROGRESS", displayValue);
        if (progressView != null) {
            progressView.setText(displayValue);
            return;
        }
        if (windowManager == null) {
            return;
        }

        TextView text = new TextView(this);
        text.setText(displayValue);
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
            DiagnosticLog.error("OVERLAY", "Unable to show progress overlay", error);
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

    private void setMenuSize(int width, int height) {
        if (menuParams == null) {
            return;
        }
        menuParams.width = width;
        menuParams.height = height;
    }

    private void positionMenu() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int gap = dp(8);
        int menuWidth = menuParams.width;
        int childCount = menuView instanceof LinearLayout
                ? ((LinearLayout) menuView).getChildCount()
                : 6;
        int estimatedMenuHeight = menuParams.height > 0
                ? menuParams.height : dp(20 + childCount * 44);

        int margin = dp(16);
        menuParams.x = bubbleParams.x > screenWidth / 2
                ? bubbleParams.x - menuWidth - gap
                : bubbleParams.x + bubbleParams.width + gap;
        menuParams.x = Math.max(margin,
                Math.min(menuParams.x, screenWidth - menuWidth - margin));
        menuParams.y = Math.max(margin,
                Math.min(bubbleParams.y, screenHeight - estimatedMenuHeight - margin));
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
        DiagnosticLog.info("SERVICE", "accessibility service disconnected");
        instance = null;
        automationRunning = false;
        inventorySelling = false;
        clearAutomationRecovery();
        taskManager.stopAll();
        handler.removeCallbacksAndMessages(null);
        stopInventoryMonitoring();
        hideProgress();
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
        if (paddleDungeonTextRecognizer != null) {
            paddleDungeonTextRecognizer.close();
            paddleDungeonTextRecognizer = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
