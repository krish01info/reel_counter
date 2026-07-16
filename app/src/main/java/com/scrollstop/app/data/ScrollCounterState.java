package com.scrollstop.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class ScrollCounterState {

    private static final String SHARED_PREFS_NAME = "scroll_stop_prefs";
    private static final String KEY_ALERT_THRESHOLD = "alert_threshold";
    private static final String KEY_IG_COUNT = "ig_count";
    private static final String KEY_YT_COUNT = "yt_count";
    private static final int DEFAULT_ALERT_THRESHOLD = 15;

    private static int alertThreshold = DEFAULT_ALERT_THRESHOLD;
    private static int igReelsCount = 0;
    private static int ytShortsCount = 0;
    private static boolean isServiceRunning = false;

    public interface StateListener {
        void onStateChanged();
    }

    private static final List<StateListener> listeners = new ArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void addListener(StateListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public static void removeListener(StateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private static void notifyListeners() {
        mainHandler.post(() -> {
            List<StateListener> copy;
            synchronized (listeners) {
                copy = new ArrayList<>(listeners);
            }
            for (StateListener listener : copy) {
                listener.onStateChanged();
            }
        });
    }

    public static void init(Context context) {
        loadAll(context);
    }

    private static void saveAll(Context context) {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ALERT_THRESHOLD, alertThreshold)
                .putInt(KEY_IG_COUNT, igReelsCount)
                .putInt(KEY_YT_COUNT, ytShortsCount)
                .apply();
        notifyListeners();
    }

    private static void loadAll(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        alertThreshold = prefs.getInt(KEY_ALERT_THRESHOLD, DEFAULT_ALERT_THRESHOLD);
        igReelsCount = prefs.getInt(KEY_IG_COUNT, 0);
        ytShortsCount = prefs.getInt(KEY_YT_COUNT, 0);
        notifyListeners();
    }

    public static int getAlertThreshold() {
        return alertThreshold;
    }

    public static void setThreshold(Context context, int newThreshold) {
        if (newThreshold > 0) {
            alertThreshold = newThreshold;
            saveAll(context);
        }
    }

    public static int getIgReelsCount() {
        return igReelsCount;
    }

    public static int getYtShortsCount() {
        return ytShortsCount;
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    public static void setServiceRunning(boolean running) {
        if (isServiceRunning != running) {
            isServiceRunning = running;
            notifyListeners();
        }
    }

    public static boolean isIgAlertActive() {
        return igReelsCount >= alertThreshold;
    }

    public static boolean isYtAlertActive() {
        return ytShortsCount >= alertThreshold;
    }

    public static boolean isAnyAlertActive() {
        return isIgAlertActive() || isYtAlertActive();
    }

    public static void resetAll(Context context) {
        igReelsCount = 0;
        ytShortsCount = 0;
        saveAll(context);
    }

    public static void incrementIg(Context context) {
        igReelsCount++;
        saveAll(context);
    }

    public static void incrementYt(Context context) {
        ytShortsCount++;
        saveAll(context);
    }
}
