package com.rat.acessibilidade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class A11yWatchdogService extends Service {

    public static final String ACTION_START = "com.rat.acessibilidade.ACTION_START";
    public static final String ACTION_STOP = "com.rat.acessibilidade.ACTION_STOP";
    public static final String ACTION_REACTIVATE_NOW = "com.rat.acessibilidade.ACTION_REACTIVATE_NOW";
    public static final String ACTION_CYCLE = "com.rat.acessibilidade.ACTION_CYCLE";
    public static final String ACTION_RELOAD = "com.rat.acessibilidade.ACTION_RELOAD";

    private static final String CHANNEL_ID = "rat_a11y_watchdog_channel";
    private static final int NOTIFICATION_ID = 9988;

    private static boolean isRunning = false;

    private HandlerThread workerThread;
    private Handler workerHandler;
    private PowerManager.WakeLock wakeLock;

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            performCheckAndReactivate();
            int interval = PreferencesHelper.getCheckIntervalSeconds(A11yWatchdogService.this);
            if (interval < 5) interval = 5;
            if (workerHandler != null && isRunning) {
                workerHandler.postDelayed(this, interval * 1000L);
            }
        }
    };

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RatA11y:WatchdogLock");
            wakeLock.setReferenceCounted(false);
        }

        workerThread = new HandlerThread("A11yWatchdogThread");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;

        if (ACTION_STOP.equals(action)) {
            stopWatchdog();
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startForeground(NOTIFICATION_ID, buildNotification("Iniciando guardião..."));
        } catch (Throwable t) {
            // Previne crash em inicialização restrita de Foreground Service
        }
        isRunning = true;
        PreferencesHelper.setWatchdogEnabled(this, true);

        if (ACTION_REACTIVATE_NOW.equals(action)) {
            workerHandler.post(this::performCheckAndReactivate);
        } else if (ACTION_CYCLE.equals(action)) {
            workerHandler.post(this::performCycle);
        } else if (ACTION_RELOAD.equals(action)) {
            workerHandler.removeCallbacks(checkRunnable);
            workerHandler.post(checkRunnable);
        } else {
            workerHandler.removeCallbacks(checkRunnable);
            workerHandler.post(checkRunnable);
            LogManager.log(this, "🛡️ Guardião iniciado. Monitoramento em segundo plano ativo.");
        }

        return START_STICKY;
    }

    private void performCheckAndReactivate() {
        try {
            if (wakeLock != null) wakeLock.acquire(3000);

            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            if (monitored.isEmpty()) {
                updateNotification("Nenhum aplicativo marcado para monitorar.");
                return;
            }

            if (!A11yManager.hasWriteSecureSettings(this)) {
                LogManager.log(this, "⚠️ Alerta: Permissão WRITE_SECURE_SETTINGS ausente!");
                updateNotification("⚠️ Atenção: Conceda permissão WRITE_SECURE_SETTINGS");
                return;
            }

            Set<String> activeIds = A11yManager.getEnabledServiceIds(this);
            List<String> missing = new ArrayList<>();

            for (String target : monitored) {
                if (!activeIds.contains(target)) {
                    missing.add(target);
                }
            }

            if (missing.isEmpty()) {
                updateNotification("✅ " + monitored.size() + " serviços monitorados — 100% ativos");
            } else {
                LogManager.log(this, "⚠️ Detectado: " + missing.size() + " serviço(s) caíram! Reativando imediatamente...");
                boolean ok = A11yManager.enableServices(this, monitored);
                if (ok) {
                    LogManager.log(this, "✅ Reativação concluída com sucesso (" + missing.size() + " restaurados).");
                    updateNotification("⚡ Serviços restaurados e ativos!");
                } else {
                    LogManager.log(this, "❌ Erro ao reativar serviços via Settings.Secure.");
                    updateNotification("❌ Falha na reativação de serviços.");
                }
            }
        } catch (Exception e) {
            LogManager.log(this, "Erro durante checagem: " + e.getMessage());
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void performCycle() {
        try {
            if (wakeLock != null) wakeLock.acquire(4000);
            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            if (monitored.isEmpty()) {
                LogManager.log(this, "Nenhum aplicativo selecionado para ciclar.");
                return;
            }

            LogManager.log(this, "🔄 Iniciando Ciclo Anti-Travamento (Pausa 1s e Religa)...");
            updateNotification("🔄 Destravando serviços de acessibilidade...");
            A11yManager.cycleRestartServices(this, monitored, 1000);
            LogManager.log(this, "✅ Ciclo Anti-Travamento finalizado. Serviços reiniciados no Android!");
            updateNotification("✅ " + monitored.size() + " serviços reiniciados com sucesso!");
        } catch (Exception e) {
            LogManager.log(this, "Erro no ciclo: " + e.getMessage());
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void stopWatchdog() {
        isRunning = false;
        PreferencesHelper.setWatchdogEnabled(this, false);
        if (workerHandler != null) {
            workerHandler.removeCallbacksAndMessages(null);
        }
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        LogManager.log(this, "Guardião parado.");
    }

    @Override
    public void onDestroy() {
        stopWatchdog();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Vigia de Acessibilidade",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mantém serviços de acessibilidade sempre ligados");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent reactivateIntent = new Intent(this, A11yWatchdogService.class);
        reactivateIntent.setAction(ACTION_REACTIVATE_NOW);
        PendingIntent reactivatePending = PendingIntent.getService(
                this, 1, reactivateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Rat Acessibilidade")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_watchdog)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "⚡ Reativar", reactivatePending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }
}
