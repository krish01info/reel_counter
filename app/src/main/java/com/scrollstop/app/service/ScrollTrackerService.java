package com.scrollstop.app.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.scrollstop.app.data.ScrollCounterState;
import com.scrollstop.app.overlay.FloatingOverlayManager;
import java.util.HashSet;
import java.util.Set;

public class ScrollTrackerService extends AccessibilityService {

    private static final String TAG = "ScrollTrackerService";
    private static final String PKG_INSTAGRAM = "com.instagram.android";
    private static final String PKG_YOUTUBE   = "com.google.android.youtube";

    private static final Set<String> IG_REELS_VIEW_IDS = new HashSet<>();
    private static final Set<String> YT_SHORTS_VIEW_IDS = new HashSet<>();

    static {
        IG_REELS_VIEW_IDS.add("com.instagram.android:id/clips_viewer_view_pager");
        IG_REELS_VIEW_IDS.add("com.instagram.android:id/reel_viewer_root");

        YT_SHORTS_VIEW_IDS.add("com.google.android.youtube:id/shorts_container");
        YT_SHORTS_VIEW_IDS.add("com.google.android.youtube:id/reel_player_page_container");
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FloatingOverlayManager overlayManager;

    private boolean isInReels  = false;
    private boolean isInShorts = false;

    // Broadcast receiver to stop the service via disableSelf()
    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.scrollstop.app.ACTION_STOP_SERVICE".equals(intent.getAction())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    disableSelf();
                }
            }
        }
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED |
                          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100L;
        info.packageNames = new String[]{PKG_INSTAGRAM, PKG_YOUTUBE};
        setServiceInfo(info);

        overlayManager = new FloatingOverlayManager(getApplicationContext());

        // Register Stop Broadcast Receiver
        IntentFilter filter = new IntentFilter("com.scrollstop.app.ACTION_STOP_SERVICE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopServiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopServiceReceiver, filter);
        }
        
        mainHandler.post(() -> ScrollCounterState.setServiceRunning(true));
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            unregisterReceiver(stopServiceReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Unregister receiver failed", e);
        }
        if (overlayManager != null) {
            overlayManager.hide();
        }
        mainHandler.post(() -> ScrollCounterState.setServiceRunning(false));
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkgCharSeq = event.getPackageName();
        if (pkgCharSeq == null) return;
        String pkg = pkgCharSeq.toString();

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: {
                boolean oldState = isInReels || isInShorts;
                switch (pkg) {
                    case PKG_INSTAGRAM:
                        refreshInstagramContext();
                        break;
                    case PKG_YOUTUBE:
                        refreshYoutubeContext();
                        break;
                    default:
                        isInReels = false;
                        isInShorts = false;
                        break;
                }

                boolean newState = isInReels || isInShorts;
                if (newState != oldState) {
                    mainHandler.post(() -> {
                        if (newState) {
                            overlayManager.show();
                        } else {
                            overlayManager.hide();
                        }
                    });
                }
                break;
            }

            case AccessibilityEvent.TYPE_VIEW_SCROLLED: {
                switch (pkg) {
                    case PKG_INSTAGRAM:
                        handleInstagramScroll(event);
                        break;
                    case PKG_YOUTUBE:
                        handleYoutubeScroll(event);
                        break;
                }
                break;
            }
        }
    }

    private void refreshInstagramContext() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        isInReels = containsAnyViewId(root, IG_REELS_VIEW_IDS);
        root.recycle();
    }

    private void handleInstagramScroll(AccessibilityEvent event) {
        if (!isInReels) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                String srcId = source.getViewIdResourceName();
                if (srcId != null && IG_REELS_VIEW_IDS.contains(srcId)) {
                    isInReels = true;
                }
                source.recycle();
            }
        }

        if (isInReels) {
            mainHandler.post(() -> ScrollCounterState.incrementIg(getApplicationContext()));
        }
    }

    private void refreshYoutubeContext() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        isInShorts = containsAnyViewId(root, YT_SHORTS_VIEW_IDS);
        root.recycle();
    }

    private void handleYoutubeScroll(AccessibilityEvent event) {
        if (!isInShorts) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                String srcId = source.getViewIdResourceName();
                if (srcId != null && YT_SHORTS_VIEW_IDS.contains(srcId)) {
                    isInShorts = true;
                }
                source.recycle();
            }
        }

        if (isInShorts) {
            mainHandler.post(() -> ScrollCounterState.incrementYt(getApplicationContext()));
        }
    }

    private boolean containsAnyViewId(AccessibilityNodeInfo node, Set<String> ids) {
        if (node == null) return false;
        String nodeId = node.getViewIdResourceName();
        if (nodeId != null && ids.contains(nodeId)) {
            return true;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            if (containsAnyViewId(child, ids)) {
                child.recycle();
                return true;
            }
            child.recycle();
        }
        return false;
    }
}
