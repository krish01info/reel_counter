package com.scrollstop.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREFS_NAME    = "scroll_stop_prefs";
    private static final String KEY_THRESHOLD = "alert_threshold";
    private static final String KEY_IG_COUNT  = "ig_count";
    private static final String KEY_YT_COUNT  = "yt_count";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Load initial state into ScrollCounterState
        ScrollCounterState.init(context);
    }

    public static void saveThreshold(int value) {
        if (prefs != null) {
            prefs.edit().putInt(KEY_THRESHOLD, value).apply();
        }
    }

    public static void saveCounters() {
        if (prefs != null) {
            prefs.edit()
                .putInt(KEY_IG_COUNT, ScrollCounterState.getIgReelsCount())
                .putInt(KEY_YT_COUNT, ScrollCounterState.getYtShortsCount())
                .apply();
        }
    }

    public static void resetCounters() {
        if (prefs != null) {
            prefs.edit()
                .putInt(KEY_IG_COUNT, 0)
                .putInt(KEY_YT_COUNT, 0)
                .apply();
        }
    }
}
