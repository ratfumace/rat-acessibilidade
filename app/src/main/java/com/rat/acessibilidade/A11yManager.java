package com.rat.acessibilidade;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class A11yManager {

    public static boolean hasWriteSecureSettings(Context context) {
        return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Set<String> getEnabledServiceIds(Context context) {
        ContentResolver resolver = context.getContentResolver();
        String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (raw == null || raw.trim().isEmpty()) {
            return new HashSet<>();
        }

        String[] parts = raw.split(":");
        Set<String> set = new HashSet<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    public static List<ServiceItem> getInstalledServices(Context context, Set<String> monitoredSet) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return Collections.emptyList();

        PackageManager pm = context.getPackageManager();
        List<AccessibilityServiceInfo> installed = am.getInstalledAccessibilityServiceList();
        Set<String> enabledSet = getEnabledServiceIds(context);

        List<ServiceItem> result = new ArrayList<>();
        if (installed != null) {
            for (AccessibilityServiceInfo info : installed) {
                String id = info.getId();
                ResolveInfo ri = info.getResolveInfo();
                if (ri == null || ri.serviceInfo == null) continue;

                String packageName = ri.serviceInfo.packageName;
                String serviceClass = ri.serviceInfo.name;

                CharSequence appLabel = ri.serviceInfo.applicationInfo.loadLabel(pm);
                String appName = appLabel != null ? appLabel.toString() : packageName;

                CharSequence srvLabel = ri.serviceInfo.loadLabel(pm);
                String serviceName = srvLabel != null ? srvLabel.toString() : serviceClass;

                Drawable icon = ri.serviceInfo.loadIcon(pm);

                boolean isActive = enabledSet.contains(id);
                boolean isMonitored = monitoredSet != null && monitoredSet.contains(id);

                result.add(new ServiceItem(id, packageName, serviceClass, appName, serviceName, icon, isActive, isMonitored));
            }
        }

        // Ordena por nome do aplicativo
        result.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));

        return result;
    }

    public static synchronized boolean enableServices(Context context, Collection<String> targetIds) {
        if (!hasWriteSecureSettings(context) || targetIds == null || targetIds.isEmpty()) {
            return false;
        }

        ContentResolver resolver = context.getContentResolver();
        String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        LinkedHashSet<String> activeSet = new LinkedHashSet<>();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String p : raw.split(":")) {
                String t = p.trim();
                if (!t.isEmpty()) activeSet.add(t);
            }
        }

        boolean changed = false;
        for (String target : targetIds) {
            if (target != null && !target.trim().isEmpty()) {
                if (activeSet.add(target.trim())) {
                    changed = true;
                }
            }
        }

        if (changed || Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 0) {
            String newString = TextUtils.join(":", activeSet);
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newString);
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        }
        return true;
    }

    public static synchronized boolean enableAllInstalledServices(Context context) {
        if (!hasWriteSecureSettings(context)) return false;

        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> installed = am.getInstalledAccessibilityServiceList();
        if (installed == null || installed.isEmpty()) return false;

        Set<String> allIds = new HashSet<>();
        for (AccessibilityServiceInfo info : installed) {
            allIds.add(info.getId());
        }

        return enableServices(context, allIds);
    }

    public static synchronized boolean disableAllServices(Context context) {
        if (!hasWriteSecureSettings(context)) return false;

        ContentResolver resolver = context.getContentResolver();
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
        return true;
    }

    public static synchronized void cycleRestartServices(Context context, Collection<String> targetIds, long pauseMs) {
        if (!hasWriteSecureSettings(context) || targetIds == null || targetIds.isEmpty()) {
            return;
        }

        ContentResolver resolver = context.getContentResolver();
        String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        LinkedHashSet<String> activeSet = new LinkedHashSet<>();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String p : raw.split(":")) {
                String t = p.trim();
                if (!t.isEmpty()) activeSet.add(t);
            }
        }

        // ETAPA 1: Desativa os serviços selecionados
        for (String target : targetIds) {
            activeSet.remove(target);
        }

        String disabledString = TextUtils.join(":", activeSet);
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, disabledString);

        // ETAPA 2: Pausa para o Android desmontar a conexão
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException ignored) {}

        // ETAPA 3: Religa todos os serviços selecionados
        for (String target : targetIds) {
            if (target != null && !target.trim().isEmpty()) {
                activeSet.add(target.trim());
            }
        }

        String enabledString = TextUtils.join(":", activeSet);
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledString);
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
    }
}
