package com.scrollstop.app.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import com.scrollstop.app.service.ScrollTrackerService;
import java.util.List;

public class PermissionHelper {

    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        );
        if (enabledServices == null) return false;

        String packageName = context.getPackageName();
        String serviceName = ScrollTrackerService.class.getName();

        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                if (packageName.equals(info.getResolveInfo().serviceInfo.packageName) &&
                    serviceName.equals(info.getResolveInfo().serviceInfo.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean allPermissionsGranted(Context context) {
        return canDrawOverlays(context) && isAccessibilityServiceEnabled(context);
    }

    public static Intent overlaySettingsIntent(Context context) {
        return new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())
        );
    }

    public static Intent accessibilitySettingsIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }
}
