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
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class A11yManager {

    private static final String TAG = "A11yManager";

    public static boolean hasWriteSecureSettings(Context context) {
        try {
            return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao verificar permissão WRITE_SECURE_SETTINGS", t);
            return false;
        }
    }

    public static Set<String> getEnabledServiceIds(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (raw == null || raw.trim().isEmpty()) {
                return new HashSet<>();
            }

            String[] parts = raw.split(":");
            Set<String> set = new HashSet<>();
            for (String p : parts) {
                if (p != null) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) {
                        set.add(trimmed);
                    }
                }
            }
            return set;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao ler serviços ativos em Settings.Secure", t);
            return new HashSet<>();
        }
    }

    public static List<ServiceItem> getInstalledServices(Context context, Set<String> monitoredSet) {
        try {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) return Collections.emptyList();

            PackageManager pm = context.getPackageManager();
            List<AccessibilityServiceInfo> installed = null;
            try {
                installed = am.getInstalledAccessibilityServiceList();
            } catch (Throwable t) {
                Log.e(TAG, "Erro ao listar serviços de acessibilidade instalados (possível falha IPC/OEM): " + t.getMessage());
            }

            Set<String> enabledSet = getEnabledServiceIds(context);
            List<ServiceItem> result = new ArrayList<>();

            if (installed != null) {
                for (AccessibilityServiceInfo info : installed) {
                    if (info == null) continue;
                    String id = info.getId();
                    if (id == null || id.trim().isEmpty()) continue;

                    ResolveInfo ri = info.getResolveInfo();
                    if (ri == null || ri.serviceInfo == null) continue;

                    String packageName = ri.serviceInfo.packageName != null ? ri.serviceInfo.packageName : "";
                    String serviceClass = ri.serviceInfo.name != null ? ri.serviceInfo.name : "";

                    // Tratamento defensivo especial para Xiaomi MIUI / HyperOS:
                    // applicationInfo pode ser nulo em apps clonados ou serviços do sistema MIUI
                    String appName = packageName;
                    if (ri.serviceInfo.applicationInfo != null && pm != null) {
                        try {
                            CharSequence appLabel = ri.serviceInfo.applicationInfo.loadLabel(pm);
                            if (appLabel != null && appLabel.length() > 0) {
                                appName = appLabel.toString();
                            }
                        } catch (Throwable ignored) {}
                    }

                    String serviceName = serviceClass;
                    if (pm != null) {
                        try {
                            CharSequence srvLabel = ri.serviceInfo.loadLabel(pm);
                            if (srvLabel != null && srvLabel.length() > 0) {
                                serviceName = srvLabel.toString();
                            }
                        } catch (Throwable ignored) {}
                    }

                    Drawable icon = null;
                    if (pm != null) {
                        try {
                            icon = ri.serviceInfo.loadIcon(pm);
                        } catch (Throwable ignored) {}
                    }

                    boolean isActive = enabledSet.contains(id);
                    boolean isMonitored = monitoredSet != null && monitoredSet.contains(id);

                    result.add(new ServiceItem(id, packageName, serviceClass, appName, serviceName, icon, isActive, isMonitored));
                }
            }

            // Ordena por nome do aplicativo de forma estritamente segura contra nulos
            result.sort((a, b) -> {
                String nameA = (a != null && a.getAppName() != null) ? a.getAppName() : "";
                String nameB = (b != null && b.getAppName() != null) ? b.getAppName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });

            return result;
        } catch (Throwable t) {
            Log.e(TAG, "Erro crítico ao processar getInstalledServices", t);
            return Collections.emptyList();
        }
    }

    public static synchronized boolean enableServices(Context context, Collection<String> targetIds) {
        if (!hasWriteSecureSettings(context) || targetIds == null || targetIds.isEmpty()) {
            return false;
        }

        try {
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
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao gravar em Settings.Secure (enableServices)", t);
            return false;
        }
    }

    public static synchronized boolean enableAllInstalledServices(Context context) {
        if (!hasWriteSecureSettings(context)) return false;

        try {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) return false;

            List<AccessibilityServiceInfo> installed = am.getInstalledAccessibilityServiceList();
            if (installed == null || installed.isEmpty()) return false;

            Set<String> allIds = new HashSet<>();
            for (AccessibilityServiceInfo info : installed) {
                if (info != null && info.getId() != null) {
                    allIds.add(info.getId());
                }
            }

            return enableServices(context, allIds);
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao ligar todas as acessibilidades", t);
            return false;
        }
    }

    public static synchronized boolean disableAllServices(Context context) {
        if (!hasWriteSecureSettings(context)) return false;

        try {
            ContentResolver resolver = context.getContentResolver();
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao desligar todas as acessibilidades", t);
            return false;
        }
    }

    public static synchronized void cycleRestartServices(Context context, Collection<String> targetIds, long pauseMs) {
        if (!hasWriteSecureSettings(context) || targetIds == null || targetIds.isEmpty()) {
            return;
        }

        try {
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
                if (target != null) {
                    activeSet.remove(target.trim());
                }
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
        } catch (Throwable t) {
            Log.e(TAG, "Erro durante cycleRestartServices", t);
        }
    }
}
