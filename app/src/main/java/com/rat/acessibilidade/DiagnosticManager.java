package com.rat.acessibilidade;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * DiagnosticManager — Gerador de pacote de diagnóstico completo (.ZIP) do Rat Acessibilidade.
 * Permite empacotar logs do guardião, dados do sistema e configurações
 * para compartilhamento direto via WhatsApp, Telegram, Google Drive ou E-mail com o suporte.
 */
public final class DiagnosticManager {

    private static final String TAG = "DiagnosticManager";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat FILE_SDF = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private DiagnosticManager() {}

    private static File getDiagnosticsBaseDir(Context context) {
        File base = context.getExternalFilesDir("diagnostics");
        if (base == null) {
            base = new File(context.getFilesDir(), "diagnostics");
        }
        if (!base.exists()) {
            base.mkdirs();
        }
        return base;
    }

    private static String getDeviceFriendlyTag() {
        String model = Build.MODEL != null ? Build.MODEL : "Device";
        return model.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String getAppVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "1.0.1";
        }
    }

    private static long getAppVersionCode(Context context) {
        try {
            android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return pi.getLongVersionCode();
            } else {
                return pi.versionCode;
            }
        } catch (Throwable t) {
            return 2L;
        }
    }

    /**
     * Gera o arquivo ZIP com todas as informações de diagnóstico.
     */
    public static File exportDiagnosticZip(Context context, String uiConsoleLogs) {
        try {
            File baseDir = getDiagnosticsBaseDir(context);
            String timeStamp = FILE_SDF.format(new Date());
            String devTag = getDeviceFriendlyTag();
            String zipName = "RatAcessibilidade_v" + getAppVersionName(context) + "_" + devTag + "_Diagnostico_" + timeStamp + ".zip";
            File zipFile = new File(baseDir, zipName);

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {

                // 1. Relatório do Sistema e Configurações
                addZipEntry(zos, "1_info_sistema.txt", buildSystemInfo(context));

                // 2. Histórico de Logs do Guardião
                StringBuilder logsSb = new StringBuilder();
                logsSb.append("═════════════════════════════════════════════════════════════════════\n");
                logsSb.append("              HISTÓRICO DE LOGS DO GUARDIÃO                          \n");
                logsSb.append("═════════════════════════════════════════════════════════════════════\n\n");
                String savedLogs = LogManager.getFormattedLogs();
                if (savedLogs != null && !savedLogs.isEmpty()) {
                    logsSb.append(savedLogs).append("\n\n");
                }
                if (uiConsoleLogs != null && !uiConsoleLogs.trim().isEmpty() && !uiConsoleLogs.equals(savedLogs)) {
                    logsSb.append("--- LOGS DO CONSOLE VISUAL ---\n").append(uiConsoleLogs).append("\n");
                }
                addZipEntry(zos, "2_historico_logs.txt", logsSb.toString());

                // 3. SharedPreferences Sanitizado
                addZipEntry(zos, "3_preferencias_sanitizadas.xml", buildSanitizedPreferences(context));
            }

            Log.i(TAG, "ZIP de diagnóstico gerado com sucesso: " + zipFile.getAbsolutePath() + " (" + zipFile.length() + " bytes)");
            return zipFile;
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao exportar ZIP de diagnóstico", t);
            return null;
        }
    }

    private static String buildSystemInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("═════════════════════════════════════════════════════════════════════\n");
        sb.append("   RELATÓRIO DE DIAGNÓSTICO DO SISTEMA — RAT ACESSIBILIDADE          \n");
        sb.append("═════════════════════════════════════════════════════════════════════\n");
        sb.append("Data/Hora: ").append(SDF.format(new Date())).append("\n");
        sb.append("Versão: ").append(getAppVersionName(context)).append(" (Build ").append(getAppVersionCode(context)).append(")\n");
        sb.append("Dispositivo: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(")\n");
        sb.append("Hardware: ").append(Build.HARDWARE).append("\n");
        sb.append("Versão Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        try {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            sb.append("Resolução de Tela: ").append(dm.widthPixels).append("x").append(dm.heightPixels).append(" (DPI ").append(dm.densityDpi).append(")\n");
        } catch (Throwable ignored) {}

        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                sb.append("Nível de Bateria: ").append(level).append("%\n");
            }
        } catch (Throwable ignored) {}

        sb.append("\n--- STATUS DAS PERMISSÕES ---\n");
        boolean hasSecure = A11yManager.hasWriteSecureSettings(context);
        sb.append("WRITE_SECURE_SETTINGS: ").append(hasSecure ? "CONCEDIDA (OK)" : "NÃO CONCEDIDA (PENDENTE ADB)").append("\n");

        boolean batteryIgnored = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
            } catch (Throwable ignored) {}
        }
        sb.append("Isenção de Bateria (Doze Mode): ").append(batteryIgnored ? "ISENTO (OK)" : "OTIMIZAÇÃO ATIVA").append("\n");
        sb.append("Iniciar no Boot: ").append(PreferencesHelper.isStartOnBoot(context) ? "SIM" : "NÃO").append("\n");

        sb.append("\n--- CONFIGURAÇÕES DO GUARDIÃO ---\n");
        sb.append("Serviço em Execução: ").append(A11yWatchdogService.isServiceRunning() ? "SIM (ATIVO 24/7)" : "NÃO").append("\n");
        sb.append("Intervalo de Verificação: ").append(PreferencesHelper.getCheckIntervalSeconds(context)).append(" segundos\n");
        sb.append("Ciclo Diário Fixo Ativo: ").append(PreferencesHelper.isDailyCycleEnabled(context) ? "SIM" : "NÃO").append("\n");
        sb.append(String.format(Locale.getDefault(), "Horário do Ciclo Diário: %02d:%02d\n",
                PreferencesHelper.getDailyCycleHour(context),
                PreferencesHelper.getDailyCycleMinute(context)));

        sb.append("\n--- APLICATIVOS MONITORADOS PELO USUÁRIO ---\n");
        Set<String> monitored = PreferencesHelper.getMonitoredServices(context);
        sb.append("Total Selecionados: ").append(monitored.size()).append("\n");
        for (String id : monitored) {
            sb.append(" - ").append(id).append("\n");
        }

        sb.append("\n--- SERVIÇOS DE ACESSIBILIDADE ATIVOS NO SISTEMA ---\n");
        try {
            String rawActive = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            sb.append("Raw Settings.Secure: ").append(rawActive != null ? rawActive : "(vazio)").append("\n");
        } catch (Throwable t) {
            sb.append("Erro ao ler Settings.Secure: ").append(t.getMessage()).append("\n");
        }

        sb.append("\n--- TODOS OS SERVIÇOS DE ACESSIBILIDADE INSTALADOS NO APARELHO ---\n");
        try {
            List<ServiceItem> installed = A11yManager.getInstalledServices(context, monitored);
            for (ServiceItem item : installed) {
                sb.append(String.format(Locale.getDefault(), " [%s] %s (%s) - Ativo: %b | Protegido: %b\n",
                        item.getId(),
                        item.getAppName(),
                        item.getPackageName(),
                        item.isActive(),
                        item.isMonitored()));
            }
        } catch (Throwable t) {
            sb.append("Erro ao listar instalados: ").append(t.getMessage()).append("\n");
        }

        sb.append("═════════════════════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    private static String buildSanitizedPreferences(Context context) {
        StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n");
        try {
            SharedPreferences sp = context.getSharedPreferences("rat_a11y_prefs", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (key != null && (key.toLowerCase(Locale.ROOT).contains("password")
                        || key.toLowerCase(Locale.ROOT).contains("secret")
                        || key.toLowerCase(Locale.ROOT).contains("token"))) {
                    sb.append("    <string name=\"").append(escapeXml(key)).append("\">[REDACTED]</string>\n");
                } else if (val instanceof Boolean) {
                    sb.append("    <boolean name=\"").append(escapeXml(key)).append("\" value=\"").append(val).append("\" />\n");
                } else if (val instanceof Integer) {
                    sb.append("    <int name=\"").append(escapeXml(key)).append("\" value=\"").append(val).append("\" />\n");
                } else if (val instanceof Long) {
                    sb.append("    <long name=\"").append(escapeXml(key)).append("\" value=\"").append(val).append("\" />\n");
                } else if (val instanceof Float) {
                    sb.append("    <float name=\"").append(escapeXml(key)).append("\" value=\"").append(val).append("\" />\n");
                } else if (val != null) {
                    sb.append("    <string name=\"").append(escapeXml(key)).append("\">").append(escapeXml(val.toString())).append("</string>\n");
                }
            }
        } catch (Throwable t) {
            sb.append("    <!-- Erro ao ler SharedPreferences: ").append(escapeXml(t.getMessage())).append(" -->\n");
        }
        sb.append("</map>\n");
        return sb.toString();
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void addZipEntry(ZipOutputStream zos, String entryName, String content) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    /**
     * Gera o pacote de diagnóstico completo e abre o menu de compartilhamento nativo do Android
     * (WhatsApp, Telegram, Google Drive, E-mail, etc.).
     */
    public static void shareDiagnosticZip(Context context, String uiConsoleLogs) {
        if (context == null) return;
        Toast.makeText(context, "📦 Gerando pacote de diagnóstico (.zip)...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                File zipFile = exportDiagnosticZip(context, uiConsoleLogs);
                if (zipFile == null || !zipFile.exists()) {
                    android.os.Handler mainH = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainH.post(() -> Toast.makeText(context, "❌ Falha ao gerar arquivo de diagnóstico!", Toast.LENGTH_LONG).show());
                    return;
                }

                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        zipFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico Rat Acessibilidade - [" + Build.MODEL + "]");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Segue em anexo o arquivo de diagnóstico completo (.zip) do Rat Acessibilidade para suporte.");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(shareIntent, "Compartilhar Diagnóstico (.zip)");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);

                LogManager.log(context, "📦 Pacote de diagnóstico (.zip) gerado e pronto para envio.");
            } catch (Throwable t) {
                Log.e(TAG, "Erro ao compartilhar diagnóstico: " + t.getMessage(), t);
                android.os.Handler mainH = new android.os.Handler(android.os.Looper.getMainLooper());
                mainH.post(() -> Toast.makeText(context, "Erro ao abrir compartilhamento: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
