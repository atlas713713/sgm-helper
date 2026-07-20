package com.local.sgmhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class MainActivity extends Activity {
    public static final String EXTRA_SHOW_CONTROLS = "show_controls";
    public static final String EXTRA_START_TRAINING = "start_training";

    private static final String GAME_PACKAGE = "hk.phx.khm.cs";
    private static final int COLOR_TEXT = Color.rgb(31, 35, 40);
    private static final int COLOR_MUTED = Color.rgb(87, 96, 106);
    private static final int COLOR_OK = Color.rgb(222, 247, 228);
    private static final int COLOR_INFO = Color.rgb(231, 240, 255);
    private static final int COLOR_ERROR = Color.rgb(255, 232, 232);

    private boolean testPending;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable testTimeout = () -> {
        if (testPending) {
            failTest(getString(R.string.test_timeout));
        }
    };

    private TextView statusView;
    private Button runTestButton;
    private Button testTargetButton;
    private boolean showControls;
    private boolean startTraining;
    private boolean launchAttempted;
    private int testCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams windowParams = getWindow().getAttributes();
        windowParams.alpha = 0.7f;
        getWindow().setAttributes(windowParams);
        showControls = getIntent().getBooleanExtra(EXTRA_SHOW_CONTROLS, false);
        startTraining = getIntent().getBooleanExtra(EXTRA_START_TRAINING, false);
        setContentView(createContentView());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showControls = intent.getBooleanExtra(EXTRA_SHOW_CONTROLS, false);
        startTraining = intent.getBooleanExtra(EXTRA_START_TRAINING, false);
        launchAttempted = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isAccessibilityServiceEnabled();
        updateServiceStatus(enabled);

        if (!showControls && enabled && !launchAttempted) {
            launchAttempted = true;
            openGame(true);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(247, 247, 245));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(28), dp(28), dp(36));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(R.string.title);
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(30);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView disclosure = new TextView(this);
        disclosure.setText(R.string.disclosure);
        disclosure.setTextColor(COLOR_MUTED);
        disclosure.setTextSize(17);
        disclosure.setLineSpacing(0, 1.2f);
        addWithTopMargin(content, disclosure, 14);

        TextView restrictedHint = new TextView(this);
        restrictedHint.setText(R.string.restricted_settings_hint);
        restrictedHint.setTextColor(COLOR_TEXT);
        restrictedHint.setTextSize(15);
        restrictedHint.setPadding(dp(16), dp(14), dp(16), dp(14));
        restrictedHint.setBackground(roundedBackground(Color.rgb(255, 246, 211), 12));
        addWithTopMargin(content, restrictedHint, 20);

        addButton(content, R.string.open_app_details, view -> openAppDetails());
        addButton(content, R.string.open_accessibility_settings, view -> openAccessibilitySettings());
        addButton(content, R.string.open_game, view -> openGame(true));

        statusView = new TextView(this);
        statusView.setTextSize(16);
        statusView.setLineSpacing(0, 1.15f);
        statusView.setPadding(dp(16), dp(14), dp(16), dp(14));
        addWithTopMargin(content, statusView, 20);

        runTestButton = addButton(content, R.string.run_safe_test, view -> runSafeTest());

        testTargetButton = new Button(this);
        testTargetButton.setAllCaps(false);
        testTargetButton.setText(getString(R.string.test_target, testCount));
        testTargetButton.setTextSize(16);
        testTargetButton.setMinHeight(dp(64));
        testTargetButton.setOnClickListener(view -> onTestTargetClicked());
        addWithTopMargin(content, testTargetButton, 12);

        return scrollView;
    }

    private Button addButton(LinearLayout parent, int textResource, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(textResource);
        button.setTextSize(16);
        button.setMinHeight(dp(52));
        button.setOnClickListener(listener);
        addWithTopMargin(parent, button, 12);
        return button;
    }

    private void addWithTopMargin(LinearLayout parent, View view, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMarginDp);
        parent.addView(view, params);
    }

    private void openAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void openGame(boolean finishAfterLaunch) {
        if (!isAccessibilityServiceEnabled()) {
            showControls = true;
            setStatus(getString(R.string.service_disabled), true);
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
        if (launchIntent == null) {
            showControls = true;
            setStatus(getString(R.string.game_missing), true);
            return;
        }

        try {
            HelperAccessibilityService service = HelperAccessibilityService.getInstance();
            if (service != null) {
                boolean autoStart = getSharedPreferences(
                        HelperAccessibilityService.PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(
                                HelperAccessibilityService.PREF_START_TRAINING_ON_LAUNCH,
                                true);
                if (startTraining && autoStart) {
                    service.startTrainingMain();
                } else {
                    service.startLoginOnly();
                }
            } else {
                startActivity(launchIntent);
            }
            if (finishAfterLaunch) {
                finish();
            }
        } catch (ActivityNotFoundException | SecurityException error) {
            showControls = true;
            setStatus(getString(R.string.game_launch_failed, error.getMessage()), true);
        }
    }

    private void runSafeTest() {
        HelperAccessibilityService service = HelperAccessibilityService.getInstance();
        if (service == null) {
            setStatus(getString(R.string.service_disabled), true);
            return;
        }

        handler.removeCallbacks(testTimeout);
        testPending = true;
        runTestButton.setEnabled(false);
        setStatus(getString(R.string.test_preparing), false);

        testTargetButton.postDelayed(() -> {
            if (!testPending) {
                return;
            }

            Rect visibleBounds = new Rect();
            if (!testTargetButton.getLocalVisibleRect(visibleBounds) || visibleBounds.isEmpty()) {
                failTest(getString(R.string.test_target_not_visible));
                return;
            }

            int[] screenLocation = new int[2];
            testTargetButton.getLocationOnScreen(screenLocation);
            float tapX = screenLocation[0] + visibleBounds.exactCenterX();
            float tapY = screenLocation[1] + visibleBounds.exactCenterY();

            boolean accepted = service.tap(
                    tapX,
                    tapY,
                    new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(android.accessibilityservice.GestureDescription gestureDescription) {
                            if (testPending) {
                                setStatus(getString(R.string.test_dispatched), false);
                            }
                        }

                        @Override
                        public void onCancelled(android.accessibilityservice.GestureDescription gestureDescription) {
                            failTest(getString(R.string.test_cancelled));
                        }
                    });

            if (!accepted) {
                failTest(getString(R.string.test_dispatch_rejected));
                return;
            }

            handler.postDelayed(testTimeout, 1500);
        }, 250);
    }

    private void onTestTargetClicked() {
        if (!testPending) {
            setStatus(getString(R.string.test_manual_hint), false);
            return;
        }

        handler.removeCallbacks(testTimeout);
        testPending = false;
        testCount++;
        testTargetButton.setText(getString(R.string.test_target, testCount));
        runTestButton.setEnabled(true);
        setStatus(getString(R.string.test_passed), false, COLOR_OK);
    }

    private void failTest(String message) {
        handler.removeCallbacks(testTimeout);
        testPending = false;
        if (runTestButton != null) {
            runTestButton.setEnabled(HelperAccessibilityService.getInstance() != null);
        }
        setStatus(message, true);
    }

    private void updateServiceStatus(boolean enabled) {
        boolean connected = HelperAccessibilityService.getInstance() != null;
        runTestButton.setEnabled(connected && !testPending);

        if (!enabled) {
            setStatus(getString(R.string.service_disabled), true);
        } else if (!connected) {
            setStatus(getString(R.string.service_connecting), false);
        } else if (!testPending) {
            setStatus(getString(R.string.service_ready), false, COLOR_INFO);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : services) {
            ResolveInfo resolveInfo = service.getResolveInfo();
            if (resolveInfo != null
                    && resolveInfo.serviceInfo != null
                    && getPackageName().equals(resolveInfo.serviceInfo.packageName)
                    && HelperAccessibilityService.class.getName().equals(resolveInfo.serviceInfo.name)) {
                return true;
            }
        }
        return false;
    }

    private void setStatus(String message, boolean error) {
        setStatus(message, error, error ? COLOR_ERROR : COLOR_INFO);
    }

    private void setStatus(String message, boolean error, int backgroundColor) {
        if (statusView == null) {
            return;
        }
        statusView.setText(message);
        statusView.setTextColor(error ? Color.rgb(145, 24, 24) : COLOR_TEXT);
        statusView.setBackground(roundedBackground(backgroundColor, 12));
    }

    private GradientDrawable roundedBackground(int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
