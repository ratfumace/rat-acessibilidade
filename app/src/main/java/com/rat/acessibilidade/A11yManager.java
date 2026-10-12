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
import java.util.Locale;
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

    /**
     * Verifica se um serviço é um leitor de tela (TalkBack) ou serviço crítico de sistema (ex: Menu Assistente flutuante)
     * que NUNCA deve ser ativado em lote, prevenindo desespero e travamento de interface do usuário.
     */
    public static boolean isDangerousSystemService(AccessibilityServiceInfo info, String serviceId) {
        if (serviceId != null) {
            String lower = serviceId.toLowerCase(Locale.ROOT);
            if (lower.contains("talkback")
                    || lower.contains("switchaccess")
                    || lower.contains("voiceaccess")
                    || lower.contains("selecttospeak")
                    || lower.contains("accessibilitymenu")
                    || lower.contains("assistantmenu")
                    || lower.contains("assistant_menu")
                    || lower.contains("samsung.accessibility")
                    || lower.contains("screenmirroring")
                    || lower.contains("screen_mirroring")
                    || lower.contains("soundamplifier")
                    || lower.contains("screenreader")
                    || lower.contains("screen_reader")
                    || lower.contains("marvin")
                    || lower.contains("braille")
                    || lower.contains("magnification")
                    || lower.contains("smartswitch")
                    || lower.contains("accessibility.scribe")
                    || lower.contains("voiceassistant")
                    || lower.contains("voice_assistant")) {
                return true;
            }
        }
        if (info != null) {
            // Se possui feedback falado (spoken feedback), trata-se de um leitor de tela (ex: TalkBack)
            if ((info.feedbackType & AccessibilityServiceInfo.FEEDBACK_SPOKEN) != 0) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean disableServices(Context context, Collection<String> targetIds) {
        if (!hasWriteSecureSettings(context) || targetIds == null || targetIds.isEmpty()) return false;
        try {
            ContentResolver resolver = context.getContentResolver();
            String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (raw == null || raw.trim().isEmpty()) return true;

            LinkedHashSet<String> activeSet = new LinkedHashSet<>();
            for (String p : raw.split(":")) {
                String t = p.trim();
                if (!t.isEmpty()) activeSet.add(t);
            }

            boolean changed = false;
            for (String target : targetIds) {
                if (target != null && activeSet.remove(target.trim())) {
                    changed = true;
                }
            }

            if (changed) {
                String newString = TextUtils.join(":", activeSet);
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newString);
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, activeSet.isEmpty() ? 0 : 1);
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao desativar serviços selecionados", t);
            return false;
        }
    }

    public static synchronized boolean keepOnlyMonitoredServices(Context context, Collection<String> monitoredIds) {
        if (!hasWriteSecureSettings(context)) return false;
        try {
            ContentResolver resolver = context.getContentResolver();
            LinkedHashSet<String> newActive = new LinkedHashSet<>();
            if (monitoredIds != null) {
                for (String id : monitoredIds) {
                    if (id != null && !id.trim().isEmpty()) {
                        newActive.add(id.trim());
                    }
                }
            }
            String newString = TextUtils.join(":", newActive);
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newString);
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, newActive.isEmpty() ? 0 : 1);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao manter apenas serviços monitorados", t);
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

            Set<String> safeIds = new HashSet<>();
            for (AccessibilityServiceInfo info : installed) {
                if (info != null && info.getId() != null) {
                    String id = info.getId();
                    if (isDangerousSystemService(info, id)) {
                        Log.w(TAG, "🛡️ Ignorando serviço perigoso/TalkBack por segurança: " + id);
                        continue;
                    }
                    safeIds.add(id);
                }
            }

            return enableServices(context, safeIds);
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao ligar todas as acessibilidades", t);
            return false;
        }
    }

    /**
     * Desativa especificamente leitores de tela como TalkBack caso tenham sido ativados acidentalmente.
     */
    public static synchronized boolean disableTalkBackAndScreenReaders(Context context) {
        if (!hasWriteSecureSettings(context)) return false;
        try {
            ContentResolver resolver = context.getContentResolver();
            String raw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (raw == null || raw.trim().isEmpty()) return true;

            LinkedHashSet<String> safeActive = new LinkedHashSet<>();
            boolean removedAny = false;
            for (String p : raw.split(":")) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    if (isDangerousSystemService(null, t)) {
                        removedAny = true;
                        Log.i(TAG, "Removendo serviço de acessibilidade de leitor de tela: " + t);
                    } else {
                        safeActive.add(t);
                    }
                }
            }

            if (removedAny) {
                String newString = TextUtils.join(":", safeActive);
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newString);
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, safeActive.isEmpty() ? 0 : 1);
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao desativar TalkBack", t);
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
