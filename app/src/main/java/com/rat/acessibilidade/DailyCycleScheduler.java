package com.rat.acessibilidade;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class DailyCycleScheduler {

    private static final String TAG = "DailyCycleScheduler";
    private static final int ALARM_REQUEST_CODE = 7711;

    public static void schedule(Context context) {
        try {
            if (!PreferencesHelper.isDailyCycleEnabled(context)) {
                cancel(context);
                return;
            }

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            int hour = PreferencesHelper.getDailyCycleHour(context);
            int minute = PreferencesHelper.getDailyCycleMinute(context);

            Calendar now = Calendar.getInstance();
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, BootReceiver.class);
            intent.setAction(BootReceiver.ACTION_BROADCAST_CYCLE);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);

            long triggerMs = target.getTimeInMillis();

            // Em Android 12+ (API 31, 33, 34) e especialmente Xiaomi MIUI / HyperOS:
            // AlarmManager.setExactAndAllowWhileIdle lança SecurityException se o sistema negar
            // a permissão SCHEDULE_EXACT_ALARM. Fazemos fallback seguro para setAndAllowWhileIdle.
            boolean exactScheduled = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
                        exactScheduled = true;
                    }
                } catch (SecurityException se) {
                    Log.w(TAG, "Permissão de alarme exato não concedida pelo sistema (Xiaomi/Android 12+): " + se.getMessage());
                }
            }

            if (!exactScheduled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi);
                }
            }

            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            LogManager.log(context, "⏰ Ciclo diário programado para às " + timeStr + ".");
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao programar alarme diário: " + t.getMessage(), t);
        }
    }

    public static void cancel(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, BootReceiver.class);
            intent.setAction(BootReceiver.ACTION_BROADCAST_CYCLE);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);
            am.cancel(pi);
            LogManager.log(context, "⏰ Agendamento diário cancelado.");
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao cancelar alarme: " + t.getMessage(), t);
        }
    }
}
