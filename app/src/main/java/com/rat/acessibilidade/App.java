package com.rat.acessibilidade;

import android.app.Application;
import android.util.Log;

public class App extends Application {
    private static final String TAG = "RatAcessibilidadeApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializa o gerenciador de logs persistente
        try {
            LogManager.init(this);
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao inicializar LogManager: " + t.getMessage(), t);
        }

        // Tratamento global de exceções para proteger o ciclo de vida do app
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                String error = "🚨 FALHA INESPERADA na thread [" + thread.getName() + "]: " + throwable.getMessage();
                Log.e(TAG, error, throwable);
                LogManager.log(App.this, error);
            } catch (Throwable ignored) {
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}
