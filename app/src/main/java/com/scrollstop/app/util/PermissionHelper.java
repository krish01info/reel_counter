package com.scrollstop.app.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
        // Deep-link directly to THIS app's overlay permission page
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent accessibilitySettingsIntent(Context context) {
        // On Android 9+ (API 28+), try to open directly to our service details
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // ComponentName of our accessibility service
                String componentName = context.getPackageName()
                        + "/" + ScrollTrackerService.class.getName();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Bundle that highlights our service in the list
                Bundle bundle = new Bundle();
                bundle.putString(":settings:fragment_args_key", componentName);
                intent.putExtra(":settings:show_fragment_args", bundle);
                return intent;
            } catch (Exception ignored) { /* fall through */ }
        }
        // Fallback: open the main Accessibility settings
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
