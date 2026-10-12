package com.rat.acessibilidade;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class LogManager {
    public interface OnLogListener {
        void onNewLog(String fullLogs);
    }

    private static final int MAX_LOG_LINES = 60;
    private static final LinkedList<String> logLines = new LinkedList<>();
    private static OnLogListener currentListener = null;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static boolean initialized = false;

    public static synchronized void init(Context context) {
        if (initialized) return;
        String saved = PreferencesHelper.getSavedLogs(context);
        if (saved != null && !saved.trim().isEmpty()) {
            String[] lines = saved.split("\n");
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    logLines.add(l);
                }
            }
        }
        initialized = true;
    }

    public static synchronized void log(Context context, String message) {
        init(context);
        String timestamp = timeFormat.format(new Date());
        String entry = "[" + timestamp + "] " + message;
        logLines.addFirst(entry);
        while (logLines.size() > MAX_LOG_LINES) {
            logLines.removeLast();
        }

        final String fullText = getFormattedLogs();
        PreferencesHelper.saveLogs(context, fullText);

        if (currentListener != null) {
            mainHandler.post(() -> {
                if (currentListener != null) {
                    currentListener.onNewLog(fullText);
                }
            });
        }
    }

    public static synchronized String getFormattedLogs() {
        StringBuilder sb = new StringBuilder();
        for (String line : logLines) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    public static synchronized void clear(Context context) {
        logLines.clear();
        PreferencesHelper.saveLogs(context, "");
        if (currentListener != null) {
            mainHandler.post(() -> {
                if (currentListener != null) {
                    currentListener.onNewLog("Logs limpos.");
                }
            });
        }
    }

    public static synchronized void setListener(OnLogListener listener) {
        currentListener = listener;
        if (listener != null) {
            final String fullText = getFormattedLogs();
            listener.onNewLog(fullText.isEmpty() ? "Nenhum evento registrado ainda." : fullText);
        }
    }
}
