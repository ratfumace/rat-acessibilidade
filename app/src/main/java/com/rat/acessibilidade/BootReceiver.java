package com.rat.acessibilidade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    public static final String ACTION_BROADCAST_START = "com.rat.acessibilidade.START";
    public static final String ACTION_BROADCAST_STOP = "com.rat.acessibilidade.STOP";
    public static final String ACTION_BROADCAST_REACTIVATE = "com.rat.acessibilidade.REACTIVATE";
    public static final String ACTION_BROADCAST_CYCLE = "com.rat.acessibilidade.CYCLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            if (PreferencesHelper.isWatchdogEnabled(context) && PreferencesHelper.isStartOnBoot(context)) {
                LogManager.log(context, "📱 Dispositivo reiniciado. Iniciando Guardião automaticamente no boot...");
                startServiceWithAction(context, A11yWatchdogService.ACTION_START);
            }
        } else if (ACTION_BROADCAST_START.equals(action)) {
            LogManager.log(context, "📡 Comando START recebido via Broadcast.");
            startServiceWithAction(context, A11yWatchdogService.ACTION_START);
        } else if (ACTION_BROADCAST_STOP.equals(action)) {
            LogManager.log(context, "📡 Comando STOP recebido via Broadcast.");
            Intent sIntent = new Intent(context, A11yWatchdogService.class);
            sIntent.setAction(A11yWatchdogService.ACTION_STOP);
            context.startService(sIntent);
        } else if (ACTION_BROADCAST_REACTIVATE.equals(action)) {
            LogManager.log(context, "📡 Comando REACTIVATE recebido via Broadcast.");
            startServiceWithAction(context, A11yWatchdogService.ACTION_REACTIVATE_NOW);
        } else if (ACTION_BROADCAST_CYCLE.equals(action)) {
            LogManager.log(context, "📡 Disparo de Ciclo Anti-Travamento executado!");
            startServiceWithAction(context, A11yWatchdogService.ACTION_CYCLE);
            if (PreferencesHelper.isDailyCycleEnabled(context)) {
                DailyCycleScheduler.schedule(context);
            }
        }
    }

    private void startServiceWithAction(Context context, String action) {
        Intent sIntent = new Intent(context, A11yWatchdogService.class);
        sIntent.setAction(action);
        try {
            ContextCompat.startForegroundService(context, sIntent);
        } catch (Exception e) {
            LogManager.log(context, "Erro ao iniciar serviço: " + e.getMessage());
        }
    }
}
