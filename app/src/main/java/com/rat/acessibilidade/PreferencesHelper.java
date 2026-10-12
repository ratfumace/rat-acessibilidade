package com.rat.acessibilidade;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferencesHelper {
    private static final String PREF_NAME = "rat_a11y_prefs";
    private static final String KEY_MONITORED_SERVICES = "monitored_services";
    private static final String KEY_WATCHDOG_ENABLED = "watchdog_enabled";
    private static final String KEY_INTERVAL_SECONDS = "interval_seconds";
    private static final String KEY_START_ON_BOOT = "start_on_boot";
    private static final String KEY_SAVED_LOGS = "saved_logs";
    private static final String KEY_DAILY_CYCLE_ENABLED = "daily_cycle_enabled";
    private static final String KEY_DAILY_CYCLE_HOUR = "daily_cycle_hour";
    private static final String KEY_DAILY_CYCLE_MINUTE = "daily_cycle_minute";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static Set<String> getMonitoredServices(Context context) {
        Set<String> set = getPrefs(context).getStringSet(KEY_MONITORED_SERVICES, null);
        if (set == null) {
            set = new HashSet<>();
        }
        return new HashSet<>(set);
    }

    public static void setMonitoredServices(Context context, Set<String> services) {
        getPrefs(context).edit().putStringSet(KEY_MONITORED_SERVICES, new HashSet<>(services)).apply();
    }

    public static void addMonitoredService(Context context, String serviceId) {
        Set<String> current = getMonitoredServices(context);
        current.add(serviceId);
        setMonitoredServices(context, current);
    }

    public static void removeMonitoredService(Context context, String serviceId) {
        Set<String> current = getMonitoredServices(context);
        current.remove(serviceId);
        setMonitoredServices(context, current);
    }

    public static boolean isWatchdogEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_WATCHDOG_ENABLED, false);
    }

    public static void setWatchdogEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_WATCHDOG_ENABLED, enabled).apply();
    }

    public static int getCheckIntervalSeconds(Context context) {
        return getPrefs(context).getInt(KEY_INTERVAL_SECONDS, 30);
    }

    public static void setCheckIntervalSeconds(Context context, int seconds) {
        getPrefs(context).edit().putInt(KEY_INTERVAL_SECONDS, seconds).apply();
    }

    public static boolean isStartOnBoot(Context context) {
        return getPrefs(context).getBoolean(KEY_START_ON_BOOT, true);
    }

    public static void setStartOnBoot(Context context, boolean startOnBoot) {
        getPrefs(context).edit().putBoolean(KEY_START_ON_BOOT, startOnBoot).apply();
    }

    public static String getSavedLogs(Context context) {
        return getPrefs(context).getString(KEY_SAVED_LOGS, "");
    }

    public static void saveLogs(Context context, String logs) {
        getPrefs(context).edit().putString(KEY_SAVED_LOGS, logs).apply();
    }

    public static boolean isDailyCycleEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DAILY_CYCLE_ENABLED, true);
    }

    public static void setDailyCycleEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_DAILY_CYCLE_ENABLED, enabled).apply();
    }

    public static int getDailyCycleHour(Context context) {
        return getPrefs(context).getInt(KEY_DAILY_CYCLE_HOUR, 5); // Default 05:00
    }

    public static void setDailyCycleHour(Context context, int hour) {
        getPrefs(context).edit().putInt(KEY_DAILY_CYCLE_HOUR, hour).apply();
    }

    public static int getDailyCycleMinute(Context context) {
        return getPrefs(context).getInt(KEY_DAILY_CYCLE_MINUTE, 0);
    }

    public static void setDailyCycleMinute(Context context, int minute) {
        getPrefs(context).edit().putInt(KEY_DAILY_CYCLE_MINUTE, minute).apply();
    }

    public static boolean isFirstRun(Context context) {
        return getPrefs(context).getBoolean("first_run", true);
    }

    public static void setFirstRunDone(Context context) {
        getPrefs(context).edit().putBoolean("first_run", false).apply();
    }
}
