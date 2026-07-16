package com.scrollstop.app.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.scrollstop.app.R;
import com.scrollstop.app.data.ScrollCounterState;
import com.scrollstop.app.util.PermissionHelper;

public class MainActivity extends AppCompatActivity implements ScrollCounterState.StateListener {

    // Cards & Counters
    private MaterialCardView cardIgReels;
    private TextView textIgCount;
    private ProgressBar progressIg;

    private MaterialCardView cardYtShorts;
    private TextView textYtCount;
    private ProgressBar progressYt;

    // Alert Banner
    private View alertBanner;

    // Permissions
    private ImageView iconPermissionOverlay;
    private ImageView iconPermissionAccessibility;

    // Buttons
    private MaterialButton btnGrantPermissions;
    private MaterialButton btnToggleService;
    private MaterialButton btnResetCounters;

    // Threshold Slider
    private TextView textThresholdLabel;
    private SeekBar sliderThreshold;

    // Header Frame
    private View headerPulseCircle;
    private ObjectAnimator headerGlowAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize state
        ScrollCounterState.init(getApplicationContext());

        // Bind Views
        cardIgReels = findViewById(R.id.card_ig_reels);
        textIgCount = findViewById(R.id.text_ig_count);
        progressIg = findViewById(R.id.progress_ig);

        cardYtShorts = findViewById(R.id.card_yt_shorts);
        textYtCount = findViewById(R.id.text_yt_count);
        progressYt = findViewById(R.id.progress_yt);

        alertBanner = findViewById(R.id.alert_banner);

        iconPermissionOverlay = findViewById(R.id.icon_permission_overlay);
        iconPermissionAccessibility = findViewById(R.id.icon_permission_accessibility);

        btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
        btnToggleService = findViewById(R.id.btn_toggle_service);
        btnResetCounters = findViewById(R.id.btn_reset_counters);

        textThresholdLabel = findViewById(R.id.text_threshold_label);
        sliderThreshold = findViewById(R.id.slider_threshold);

        headerPulseCircle = findViewById(R.id.header_pulse_circle);

        // Setup Header Glow Animation (Pulse alpha 0.4 to 1.0)
        setupHeaderAnimation();

        // Setup Action Listeners
        btnGrantPermissions.setOnClickListener(v -> {
            boolean overlayOk = PermissionHelper.canDrawOverlays(MainActivity.this);
            Intent intent;
            if (!overlayOk) {
                intent = PermissionHelper.overlaySettingsIntent(MainActivity.this);
            } else {
                intent = PermissionHelper.accessibilitySettingsIntent();
            }
            startActivity(intent);
        });

        btnToggleService.setOnClickListener(v -> {
            boolean isRunning = ScrollCounterState.isServiceRunning();
            if (!isRunning) {
                // Activate
                startActivity(PermissionHelper.accessibilitySettingsIntent());
            } else {
                // Deactivate
                sendBroadcast(new Intent("com.scrollstop.app.ACTION_STOP_SERVICE"));
            }
        });

        btnResetCounters.setOnClickListener(v -> ScrollCounterState.resetAll(MainActivity.this));

        sliderThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textThresholdLabel.setText("Alert threshold: " + progress + " scrolls");
                if (fromUser) {
                    ScrollCounterState.setThreshold(MainActivity.this, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Register observer listener
        ScrollCounterState.addListener(this);

        // Perform initial UI render
        updateUi();

        // Check for updates from GitHub in the background
        checkForUpdates();
    }

    private void setupHeaderAnimation() {
        headerGlowAnimator = ObjectAnimator.ofFloat(headerPulseCircle, "alpha", 0.4f, 1.0f);
        headerGlowAnimator.setDuration(1200);
        headerGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        headerGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        headerGlowAnimator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Permission statuses might have changed in settings
        updateUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScrollCounterState.removeListener(this);
        if (headerGlowAnimator != null) {
            headerGlowAnimator.cancel();
        }
    }

    @Override
    public void onStateChanged() {
        updateUi();
    }

    private void updateUi() {
        // Read current state
        int igCount = ScrollCounterState.getIgReelsCount();
        int ytCount = ScrollCounterState.getYtShortsCount();
        int threshold = ScrollCounterState.getAlertThreshold();
        boolean isRunning = ScrollCounterState.isServiceRunning();
        boolean isAnyAlert = ScrollCounterState.isAnyAlertActive();

        boolean overlayOk = PermissionHelper.canDrawOverlays(this);
        boolean accessOk = PermissionHelper.isAccessibilityServiceEnabled(this);
        boolean allGranted = overlayOk && accessOk;

        // Update counts
        textIgCount.setText(String.valueOf(igCount));
        textYtCount.setText(String.valueOf(ytCount));

        // Update progress bounds & progress
        progressIg.setMax(threshold);
        progressIg.setProgress(igCount);

        progressYt.setMax(threshold);
        progressYt.setProgress(ytCount);

        float density = getResources().getDisplayMetrics().density;

        // IG Reels Card styling (Red border and text on alert, else standard style)
        if (ScrollCounterState.isIgAlertActive()) {
            progressIg.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF5252")));
            cardIgReels.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF5252")));
            cardIgReels.setStrokeWidth(Math.round(2 * density));
            textIgCount.setTextColor(Color.parseColor("#FF5252"));
        } else {
            progressIg.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E91E8C"))); // IG Pink
            cardIgReels.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#10FFFFFF")));
            cardIgReels.setStrokeWidth(Math.round(1 * density));
            textIgCount.setTextColor(Color.parseColor("#E0E0F0"));
        }

        // YT Shorts Card styling (Red border and text on alert, else standard style)
        if (ScrollCounterState.isYtAlertActive()) {
            progressYt.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF5252")));
            cardYtShorts.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF5252")));
            cardYtShorts.setStrokeWidth(Math.round(2 * density));
            textYtCount.setTextColor(Color.parseColor("#FF5252"));
        } else {
            progressYt.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF0000"))); // YT Red
            cardYtShorts.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#10FFFFFF")));
            cardYtShorts.setStrokeWidth(Math.round(1 * density));
            textYtCount.setTextColor(Color.parseColor("#E0E0F0"));
        }

        // Alert Banner visibility
        alertBanner.setVisibility(isAnyAlert ? View.VISIBLE : View.GONE);

        // Permission status indicators
        iconPermissionOverlay.setImageResource(overlayOk ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle);
        iconPermissionAccessibility.setImageResource(accessOk ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle);

        // Grant Permissions Button
        btnGrantPermissions.setVisibility(allGranted ? View.GONE : View.VISIBLE);

        // Service Toggle Button states (Activate / Deactivate)
        btnToggleService.setEnabled(allGranted || isRunning);
        if (isRunning) {
            btnToggleService.setText("Deactivate Overlay");
            btnToggleService.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF5252"))); // Red
        } else {
            btnToggleService.setText("Activate Overlay");
            btnToggleService.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C4DFF"))); // Electric Violet
        }

        // Sync seekbar threshold value
        if (sliderThreshold.getProgress() != threshold) {
            sliderThreshold.setProgress(threshold);
        }
        textThresholdLabel.setText("Alert threshold: " + threshold + " scrolls");
    }

    private void checkForUpdates() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://raw.githubusercontent.com/krish01info/reel_counter/main/version.json");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    java.io.InputStream in = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String json = sb.toString();
                    int remoteVersionCode = parseJsonInt(json, "versionCode");
                    String remoteVersionName = parseJsonString(json, "versionName");
                    String updateUrl = parseJsonString(json, "updateUrl");

                    int localVersionCode;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        localVersionCode = (int) getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
                    } else {
                        localVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    }

                    if (remoteVersionCode > localVersionCode) {
                        runOnUiThread(() -> showUpdateDialog(remoteVersionName, updateUrl));
                    }
                }
            } catch (Exception e) {
                // Fail silently in background if offline
            }
        }).start();
    }

    private void showUpdateDialog(String newVersionName, String updateUrl) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Available!")
                .setMessage("A new version (v" + newVersionName + ") of Scroll Stop is available. Would you like to update now?")
                .setPositiveButton("Update", (dialog, which) -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private int parseJsonInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private String parseJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
