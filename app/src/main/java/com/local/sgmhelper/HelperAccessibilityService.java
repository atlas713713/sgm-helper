package com.local.sgmhelper;

import android.annotation.SuppressLint;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

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
    static final String PREFS_NAME = "schedule";
    static final String PREF_HOUR = "hour";
    static final String PREF_MINUTE = "minute";
    static final String PREF_LEGION_HOUR = "legion_hour";
    static final String PREF_LEGION_MINUTE = "legion_minute";
    static final String PREF_MILITARY_HOUR = "military_hour";
    static final String PREF_MILITARY_MINUTE = "military_minute";
    static final String PREF_WELFARE_HOUR = "welfare_hour";
    static final String PREF_WELFARE_MINUTE = "welfare_minute";
    static final String PREF_WORSHIP_ENABLED = "worship_enabled";
    static final String PREF_MILITARY_ENABLED = "military_enabled";
    static final String PREF_WELFARE_ENABLED = "welfare_enabled";
    static final String PREF_LEGION_REWARD_ENABLED = "legion_reward_enabled";
    static final String PREF_MILITARY_RETRY_AT = "military_retry_at";
    static final String PREF_SUPPLY_RETRY_AT = "supply_retry_at";
    static final String PREF_WILDERNESS_RETRY_AT = "wilderness_retry_at";
    private static final String PREF_DUNGEON_SWEEP_LEVELS = "dungeon_sweep_levels";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TrainingAutomation trainingAutomation = new TrainingAutomation(this);
    private final TaskAutomation taskAutomation = new TaskAutomation(this, trainingAutomation);
    private final RewardAutomation rewardAutomation =
            new RewardAutomation(this, trainingAutomation);
    private final WelfareAutomation welfareAutomation =
            new WelfareAutomation(this, trainingAutomation);
    private final LoginAutomation loginAutomation = new LoginAutomation(this);
    private final DungeonSweepAutomation dungeonSweepAutomation =
            new DungeonSweepAutomation(this);
    private final BossAutomation bossAutomation = new BossAutomation(this);

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
    private Runnable currentAutomationAction;
    private PrimaryTask primaryTask = PrimaryTask.TRAINING;
    private Runnable primaryTaskAction = trainingAutomation::start;
    private TextRecognizer textRecognizer;

    static HelperAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        DiagnosticLog.info("SERVICE", "accessibility service connected");
        showOverlay();
        WorshipAlarmReceiver.scheduleAll(this);
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
            DiagnosticLog.error("OVERLAY", "Unable to add floating menu", error);
        }
    }

    private void populateMainMenu(LinearLayout menu) {
        menu.removeAllViews();
        stateView = addMenuText(menu, R.string.menu_status_idle);
        addMenuButton(menu, R.string.menu_training, view -> startTrainingMain());
        addMenuButton(menu, R.string.menu_boss, view -> showBossMenu());
        addMenuButton(menu, R.string.menu_dungeon_sweep, view -> showDungeonSweepMenu());
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

    private void startTrainingMain() {
        if (automationRunning) {
            showProgress("已有任务正在运行，请先终止");
            closeMenu();
            return;
        }
        setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(PREF_MILITARY_ENABLED, true)) {
            showProgress("初始化：重新检测军务任务");
            startScheduledMilitary();
        } else {
            startScheduledAutomation("自动练级：打开游戏", trainingAutomation::start);
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
        addMenuButton(menu, R.string.settings_login, view -> showLoginSettings());
        addMenuButton(menu, R.string.settings_back, view -> populateMainMenu(menu));
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
        if (!(menuView instanceof LinearLayout)) {
            return;
        }
        LinearLayout menu = (LinearLayout) menuView;
        menu.removeAllViews();
        stateView = null;
        showBossPage(menu);
    }

    private void showDungeonSweepMenu() {
        if (!(menuView instanceof LinearLayout)) {
            return;
        }
        LinearLayout menu = (LinearLayout) menuView;
        menu.removeAllViews();
        stateView = null;
        addMenuText(menu, R.string.dungeon_sweep_title);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> selected = new HashSet<>(preferences.getStringSet(
                PREF_DUNGEON_SWEEP_LEVELS, Collections.emptySet()));
        List<CheckBox> boxes = new ArrayList<>();
        for (int start = 0; start < DungeonSweepAutomation.LEVELS.length; start += 3) {
            LinearLayout row = new LinearLayout(this);
            for (int index = start;
                    index < Math.min(start + 3, DungeonSweepAutomation.LEVELS.length);
                    index++) {
                int level = DungeonSweepAutomation.LEVELS[index];
                CheckBox box = new CheckBox(this);
                box.setText(String.valueOf(level));
                box.setTextSize(12);
                box.setPadding(0, 0, 0, 0);
                box.setChecked(selected.contains(String.valueOf(level)));
                box.setOnCheckedChangeListener((button, checked) -> {
                    Set<String> updated = new HashSet<>(preferences.getStringSet(
                            PREF_DUNGEON_SWEEP_LEVELS, Collections.emptySet()));
                    if (checked) {
                        updated.add(String.valueOf(level));
                    } else {
                        updated.remove(String.valueOf(level));
                    }
                    preferences.edit().putStringSet(PREF_DUNGEON_SWEEP_LEVELS, updated).apply();
                });
                boxes.add(box);
                row.addView(box, new LinearLayout.LayoutParams(0, dp(34), 1));
            }
            menu.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        }

        LinearLayout controls = new LinearLayout(this);
        Button selectAll = createMenuButton(R.string.dungeon_select_all,
                view -> boxes.forEach(box -> box.setChecked(true)));
        Button clear = createMenuButton(R.string.dungeon_clear,
                view -> boxes.forEach(box -> box.setChecked(false)));
        controls.addView(selectAll, new LinearLayout.LayoutParams(0, dp(40), 1));
        controls.addView(clear, new LinearLayout.LayoutParams(0, dp(40), 1));
        menu.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        addMenuButton(menu, R.string.dungeon_start_sweep,
                view -> dungeonSweepAutomation.start(selectedDungeonLevels(preferences)));
        addMenuButton(menu, R.string.settings_back, view -> populateMainMenu(menu));
        updateMenuPosition();
    }

    private static List<Integer> selectedDungeonLevels(SharedPreferences preferences) {
        Set<String> selected = preferences.getStringSet(
                PREF_DUNGEON_SWEEP_LEVELS, Collections.emptySet());
        List<Integer> levels = new ArrayList<>();
        for (int level : DungeonSweepAutomation.LEVELS) {
            if (selected.contains(String.valueOf(level))) {
                levels.add(level);
            }
        }
        return levels;
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

    private void showBossPage(LinearLayout menu) {
        addMenuText(menu, R.string.settings_boss);
        addMenuButton(menu, R.string.boss_wilderness, view -> bossAutomation.start());
        addMenuText(menu, R.string.boss_world_pending);
        addMenuButton(menu, R.string.settings_back, view -> populateMainMenu(menu));
        updateMenuPosition();
    }

    private void showTimerPage(LinearLayout menu) {
        addMenuText(menu, R.string.settings_timer);
        addTimerRow(menu, R.string.timer_worship, PREF_HOUR, PREF_MINUTE,
                PREF_WORSHIP_ENABLED, true, 10, 0,
                () -> WorshipAlarmReceiver.scheduleWorship(this), this::startScheduledWorship);
        addTimerRow(menu, R.string.timer_military,
                PREF_MILITARY_HOUR, PREF_MILITARY_MINUTE,
                PREF_MILITARY_ENABLED, true, -1, -1,
                () -> WorshipAlarmReceiver.scheduleMilitary(this),
                this::startScheduledMilitary);
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
        addMenuButton(menu, R.string.settings_back, view -> showSettingsMenu());
        updateMenuPosition();
    }

    private void addTimerRow(LinearLayout menu, int titleResource,
            String hourKey, String minuteKey, String enabledKey, boolean defaultEnabled,
            int defaultHour, int defaultMinute,
            Runnable reschedule, Runnable runNow) {
        LinearLayout row = new LinearLayout(this);
        CheckBox enabled = new CheckBox(this);
        Button timer = createMenuButton(titleResource, null);
        timer.setTextSize(13);
        Button execute = createMenuButton(R.string.timer_run_now,
                runNow == null ? null : view -> runNow.run());
        execute.setTextSize(13);

        if (hourKey == null) {
            enabled.setEnabled(false);
            timer.setEnabled(false);
            execute.setEnabled(false);
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

        row.addView(enabled, new LinearLayout.LayoutParams(dp(42), dp(44)));
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

    void startScheduledMilitary() {
        if (!shouldRunMilitary(primaryTask)) {
            DiagnosticLog.info("MILITARY",
                    "skipped; primary task=" + primaryTaskLabel(primaryTask));
            return;
        }
        taskAutomation.start();
    }

    private void startScheduledAutomation(String progress, Runnable firstAction) {
        startGameAutomation(progress, firstAction, firstAction);
    }

    void startLoginOnly() {
        startGameAutomation("登录：打开游戏", this::completeAutomation, null);
    }

    private void startGameAutomation(
            String progress, Runnable afterLogin, Runnable recoveryAction) {
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
            currentAutomationAction = recoveryAction;
            automationRecoveryAttempts = 0;
            handler.postDelayed(() -> loginAutomation.start(afterLogin), 10_000);
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
        return automationRunning;
    }

    @Override
    public void startAutomation(String progress, Runnable firstAction) {
        startScheduledAutomation(progress, firstAction);
    }

    @Override
    public void startPrimaryAutomation(
            PrimaryTask task, String progress, Runnable firstAction) {
        setPrimaryTask(task, firstAction);
        startScheduledAutomation(progress, firstAction);
    }

    @Override
    public void postDelayed(Runnable action, long delayMillis) {
        handler.postDelayed(action, delayMillis);
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

    @Override
    public void enterTraining(long nextMilitaryAt) {
        automationRunning = false;
        clearAutomationRecovery();
        setTaskState(STATE_RUNNING);
        showProgress(nextMilitaryAt > 0
                ? "自动练级中 · 下次军务 " + formatTime(nextMilitaryAt)
                : "自动练级中");
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
        if (!automationRunning) {
            return;
        }
        boolean accepted = tap(x, y, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                scheduleNext(next, nextDelayMillis);
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
        scheduleNext(next, CLICK_DELAY_MS);
    }

    private void scheduleNext(Runnable next, int delayMillis) {
        if (automationRunning) {
            handler.postDelayed(next, delayMillis);
        }
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
    public void ensureAutoAttackDisabled(Runnable next) {
        ensureGameHudVisible(
                () -> ensureAutoAttackState(false, next, SCREEN_WAIT_RETRY_COUNT));
    }

    @Override
    public void ensureAutoAttackEnabled(Runnable next) {
        ensureGameHudVisible(
                () -> ensureAutoAttackState(true, next, SCREEN_WAIT_RETRY_COUNT));
    }

    @Override
    public void ensureGameHudVisible(Runnable next) {
        showProgress("准备游戏画面：关闭遮挡窗口（1/2）");
        performTap(640, 20, () -> {
            showProgress("准备游戏画面：关闭遮挡窗口（2/2）");
            performTap(640, 20, () -> closeWelfareWindowIfNeeded(next));
        });
    }

    private void closeWelfareWindowIfNeeded(Runnable next) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            List<String> values = new ArrayList<>();
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    values.add(line.getText());
                }
            }
            if (ScreenGuard.isWelfareWindow(values)) {
                closeWelfareWindow(next);
            } else {
                next.run();
            }
        });
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
        closeAutoPathPanelIfNeeded(next, TEXT_RETRY_COUNT);
    }

    private void closeAutoPathPanelIfNeeded(Runnable next, int remainingAttempts) {
        recognizeScreenText(text -> {
            if (!automationRunning) {
                return;
            }
            if (hasAutoPathPanel(text)) {
                if (remainingAttempts > 0) {
                    showProgress("自动军务：关闭自动寻路栏");
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

    private boolean hasAutoPathPanel(Text text) {
        List<String> values = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
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
                        DiagnosticLog.error("OCR", "Text recognition failed", error);
                        failAutomation("Text recognition failed");
                    })
                    .addOnCompleteListener(task -> bitmap.recycle());
        });
    }

    @Override
    public void recognizeText(Consumer<Text> result) {
        recognizeScreenText(result);
    }

    @Override
    public void recognizeMapCoordinate(Consumer<String> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                result.accept("");
                return;
            }
            int left = 430 * bitmap.getWidth() / 1280;
            int top = 630 * bitmap.getHeight() / 720;
            int right = 850 * bitmap.getWidth() / 1280;
            int bottom = 710 * bitmap.getHeight() / 720;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, left, top, right - left, bottom - top);
            bitmap.recycle();
            Bitmap enlarged = Bitmap.createScaledBitmap(
                    cropped, cropped.getWidth() * 3, cropped.getHeight() * 3, true);
            cropped.recycle();

            if (textRecognizer == null) {
                textRecognizer = TextRecognition.getClient(
                        new ChineseTextRecognizerOptions.Builder().build());
            }
            textRecognizer.process(InputImage.fromBitmap(enlarged, 0))
                    .addOnSuccessListener(text -> result.accept(text.getText()))
                    .addOnFailureListener(error -> {
                        DiagnosticLog.warn("OCR",
                                "Map coordinate recognition failed: " + error.getMessage());
                        result.accept("");
                    })
                    .addOnCompleteListener(task -> enlarged.recycle());
        });
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
                        DiagnosticLog.error("OCR", "Quick arrival text recognition failed", error);
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
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String value = normalizeText(line.getText());
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && isHorizontalMatch(
                            bounds.centerX(), minX, maxX)
                            && (exact ? value.equals(target) : value.contains(target))) {
                        performTap(bounds.centerX(), bounds.centerY(), next, nextDelayMillis);
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                handler.postDelayed(
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

    @Override
    public void waitForText(String expected, int attempts, Runnable next) {
        waitForScreenText(expected, attempts, next);
    }

    private static String normalizeText(String value) {
        return value.replaceAll("\\s+", "")
                .replace('(', '（')
                .replace(')', '）');
    }

    @Override
    public void captureScreenshot(Consumer<Bitmap> result) {
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
                    DiagnosticLog.error("SCREENSHOT", "Unable to inspect screenshot", error);
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
                    DiagnosticLog.warn("SCREENSHOT", "busy; retrying, code=" + errorCode
                            + " remainingAttempts=" + (remainingAttempts - 1));
                    handler.postDelayed(
                            () -> captureScreenshot(result, remainingAttempts - 1),
                            ACTION_DELAY_MS);
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
        DiagnosticLog.info("AUTOMATION", "completed");
        boolean completedPrimaryDungeon = currentAutomationAction == primaryTaskAction
                && primaryTask == PrimaryTask.DUNGEON;
        automationRunning = false;
        clearAutomationRecovery();
        if (completedPrimaryDungeon) {
            resetPrimaryTaskToTraining();
        }
        hideProgress();
        setTaskState(STATE_COMPLETED);
    }

    @Override
    public void failAutomation(String message) {
        DiagnosticLog.error("AUTOMATION", message);
        handler.removeCallbacksAndMessages(null);
        if (currentAutomationAction == null) {
            automationRunning = false;
            setTaskState(STATE_STOPPED);
            showProgress("错误：" + message);
            return;
        }

        recoverPrimaryTask(message);
    }

    private void recoverPrimaryTask(String message) {
        automationRecoveryAttempts++;
        boolean quickRetry = shouldRetryAutomation(automationRecoveryAttempts);
        automationRunning = true;
        currentAutomationAction = primaryTaskAction;
        setTaskState(STATE_RUNNING);
        showProgress("错误：" + message + " · "
                + (quickRetry ? "5秒后" : "连续失败，60秒后")
                + "恢复主要任务：" + primaryTaskLabel(primaryTask));
        handler.postDelayed(() -> {
            if (!automationRunning || primaryTaskAction == null) {
                return;
            }
            showProgress("错误恢复：恢复主要任务 · " + primaryTaskLabel(primaryTask));
            primaryTaskAction.run();
        }, quickRetry ? RECOVERY_START_DELAY_MS : RECOVERY_LONG_DELAY_MS);
    }

    private void stopAutomation(int state) {
        DiagnosticLog.info("AUTOMATION", "stopped state=" + state);
        automationRunning = false;
        clearAutomationRecovery();
        resetPrimaryTaskToTraining();
        handler.removeCallbacksAndMessages(null);
        hideProgress();
        setTaskState(state);
    }

    private void clearAutomationRecovery() {
        automationRecoveryAttempts = 0;
        currentAutomationAction = null;
    }

    private void resetPrimaryTaskToTraining() {
        setPrimaryTask(PrimaryTask.TRAINING, trainingAutomation::start);
    }

    private void setPrimaryTask(PrimaryTask task, Runnable action) {
        primaryTask = task;
        primaryTaskAction = action;
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
        DiagnosticLog.info("SERVICE", "accessibility service disconnected");
        instance = null;
        automationRunning = false;
        clearAutomationRecovery();
        handler.removeCallbacksAndMessages(null);
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
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
