/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.accessibility;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Process;
import android.service.quicksettings.TileService;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;

import java.util.List;
import java.util.Set;

/**
 * Utility class for validating accessibility tile services.
 *
 * <p>This class provides methods to validate that tile services associated with
 * accessibility services meet the required security criteria:
 * <ul>
 *   <li>Service must be resolvable via QS_TILE intent</li>
 *   <li>Service must be exported</li>
 *   <li>Service must be protected by BIND_QUICK_SETTINGS_TILE permission</li>
 *   <li>Service must be enabled</li>
 * </ul>
 *
 * @hide
 */
public abstract class AccessibilityTileUtils {

    private static final String TAG = "AccessibilityTileUtils";

    /**
     * Resolves whether a component is enabled based on its explicit enabled setting
     * and default state.
     *
     * @param enabledSetting The explicit enabled setting from PackageManager
     * @param defaultEnabled The default enabled state from ServiceInfo
     * @return true if the component is enabled
     */
    public static boolean resolveEnabledComponent(int enabledSetting, boolean defaultEnabled) {
        if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            return defaultEnabled;
        }
        return false;
    }

    /**
     * Validates that a component is a valid QS tile service.
     *
     * @param context The context
     * @param packageManagerInternal PackageManagerInternal instance
     * @param componentName The component to validate
     * @param userId The user ID
     * @return true if the component is a valid tile service
     */
    public static boolean isComponentValidTileService(
            @NonNull Context context,
            @NonNull PackageManagerInternal packageManagerInternal,
            @NonNull ComponentName componentName,
            @UserIdInt int userId) {
        Intent intent = new Intent(TileService.ACTION_QS_TILE);
        intent.setComponent(componentName);

        ResolveInfo resolveInfo = packageManagerInternal.resolveService(
                intent,
                intent.resolveTypeIfNeeded(context.getContentResolver()),
                0,
                userId,
                Process.myUid());

        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.w(TAG, "TileService could not be resolved: " + componentName);
            return false;
        }

        ServiceInfo serviceInfo = resolveInfo.serviceInfo;

        if (!serviceInfo.exported) {
            Slog.w(TAG, "TileService is not exported: " + componentName);
            return false;
        }

        if (!Manifest.permission.BIND_QUICK_SETTINGS_TILE.equals(serviceInfo.permission)) {
            Slog.w(TAG, "TileService is not protected by BIND_QUICK_SETTINGS_TILE permission: "
                    + componentName);
            return false;
        }

        int enabledSetting = packageManagerInternal.getComponentEnabledSetting(
                componentName, Process.myUid(), userId);
        if (!resolveEnabledComponent(enabledSetting, serviceInfo.enabled)) {
            Slog.w(TAG, "TileService is not enabled: " + componentName.flattenToShortString());
            return false;
        }

        return true;
    }

    /**
     * Gets the set of valid accessibility tile services from the provided lists.
     *
     * @param context The context
     * @param packageManagerInternal PackageManagerInternal instance
     * @param accessibilityServiceInfos List of AccessibilityServiceInfo
     * @param accessibilityShortcutInfos List of AccessibilityShortcutInfo
     * @param userId The user ID
     * @return Set of valid tile service ComponentNames
     */
    @NonNull
    public static Set<ComponentName> getValidA11yTileServices(
            @NonNull Context context,
            @Nullable PackageManagerInternal packageManagerInternal,
            @Nullable List<AccessibilityServiceInfo> accessibilityServiceInfos,
            @Nullable List<AccessibilityShortcutInfo> accessibilityShortcutInfos,
            @UserIdInt int userId) {
        ArraySet<ComponentName> validTileServices = new ArraySet<>();

        if (packageManagerInternal == null) {
            return validTileServices;
        }

        if (accessibilityServiceInfos != null) {
            for (AccessibilityServiceInfo info : accessibilityServiceInfos) {
                addValidTileServiceFromAccessibilityServiceInfo(
                        context, packageManagerInternal, userId, validTileServices, info);
            }
        }

        if (accessibilityShortcutInfos != null) {
            for (AccessibilityShortcutInfo info : accessibilityShortcutInfos) {
                addValidTileServiceFromAccessibilityShortcutInfo(
                        context, packageManagerInternal, userId, validTileServices, info);
            }
        }

        return validTileServices;
    }

    private static void addValidTileServiceFromAccessibilityServiceInfo(
            @NonNull Context context,
            @NonNull PackageManagerInternal packageManagerInternal,
            @UserIdInt int userId,
            @NonNull Set<ComponentName> validTileServices,
            @NonNull AccessibilityServiceInfo info) {
        String tileServiceName = info.getTileServiceName();
        if (TextUtils.isEmpty(tileServiceName)) {
            return;
        }

        ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
        ComponentName tileComponent = new ComponentName(serviceInfo.packageName, tileServiceName);

        if (isComponentValidTileService(context, packageManagerInternal, tileComponent, userId)) {
            validTileServices.add(tileComponent);
        }
    }

    private static void addValidTileServiceFromAccessibilityShortcutInfo(
            @NonNull Context context,
            @NonNull PackageManagerInternal packageManagerInternal,
            @UserIdInt int userId,
            @NonNull Set<ComponentName> validTileServices,
            @NonNull AccessibilityShortcutInfo info) {
        String tileServiceName = info.getTileServiceName();
        if (TextUtils.isEmpty(tileServiceName)) {
            return;
        }

        ComponentName tileComponent = new ComponentName(
                info.getComponentName().getPackageName(), tileServiceName);

        if (isComponentValidTileService(context, packageManagerInternal, tileComponent, userId)) {
            validTileServices.add(tileComponent);
        }
    }
}
