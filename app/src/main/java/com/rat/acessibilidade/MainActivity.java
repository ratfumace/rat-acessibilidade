package com.rat.acessibilidade;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // 1. Cabeçalho
    private TextView tvHeaderDot;
    private TextView tvHeaderStatus;
    private ImageButton btnRefresh;

    // 2. Abas de Navegação
    private MaterialButton tabBtnProtection;
    private MaterialButton tabBtnTutorial;
    private MaterialButton tabBtnHistory;
    private View viewTabProtection;
    private View viewTabTutorial;
    private View viewTabHistory;

    // 3. Controle Mestre
    private LinearLayout btnMasterToggle;
    private ImageView imgMasterPower;
    private TextView tvMasterTitle;
    private TextView tvMasterSubtitle;
    private SwitchMaterial switchMaster;

    // 4. Permissões
    private TextView tvIconSecure;
    private TextView tvStatusSecure;
    private View boxSecurePendingActions;
    private MaterialButton btnCopyAdbQuick;
    private MaterialButton btnOpenTutorialTab;
    private TextView tvIconBattery;
    private MaterialButton btnFixBattery;
    private SwitchMaterial switchBoot;
    private TextView tvIconNotification;
    private TextView tvNotificationSubtitle;
    private MaterialButton btnNotificationSettings;
    private SwitchMaterial switchNotification;

    // 5. Aplicativos Protegidos (Dashboard)
    private TextView tvSelectedAppsCount;
    private TextView tvEmptySelectedApps;
    private RecyclerView recyclerSelectedApps;
    private SelectedAppsAdapter selectedAppsAdapter;
    private LinearLayout btnOpenAppPicker;

    // 6. Ações Gerais
    private LinearLayout btnActionCycle;
    private LinearLayout btnActionEnableAll;
    private LinearLayout btnActionDisableAll;

    // 7. Frequência & Horário Fixo
    private Spinner spinnerInterval;
    private SwitchMaterial switchDailyFixed;
    private LinearLayout boxFixedTimePicker;
    private MaterialButton btnFixedTime;

    // 8. Tutorial PC
    private MaterialButton btnTutorialCopyAdb;
    private LinearLayout btnTutorialVideoYoutube;
    private LinearLayout btnTutorialAiPrompt;
    private LinearLayout btnTutorialShareWhatsapp;

    // 9. Histórico (Logs)
    private TextView tvLogsConsole;
    private MaterialButton btnLogsCopy;
    private MaterialButton btnLogsShareZip;
    private MaterialButton btnLogsClear;

    private final int[] INTERVAL_VALUES = {10, 30, 60, 300, 900, 3600};
    private final String[] INTERVAL_LABELS = {
            "A cada 10 segundos",
            "A cada 30 segundos",
            "A cada 1 minuto",
            "A cada 5 minutos",
            "A cada 15 minutos",
            "A cada 1 hora"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            initViews();
            setupTabs();
            setupMasterToggle();
            setupPermissions();
            setupAppPickerSection();
            setupActionButtons();
            setupScheduling();
            setupTutorialSection();
            setupHistorySection();

            LogManager.init(this);

            if (PreferencesHelper.isDailyCycleEnabled(this)) {
                DailyCycleScheduler.schedule(this);
            }
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Falha durante onCreate: " + t.getMessage(), t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            refreshAllData();

            LogManager.setListener(fullLogs -> {
                if (tvLogsConsole != null) {
                    tvLogsConsole.setText(fullLogs);
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Falha durante onResume: " + t.getMessage(), t);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            LogManager.setListener(null);
        } catch (Throwable ignored) {}
    }

    private void refreshAllData() {
        try {
            updatePermissionStatus();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Erro em updatePermissionStatus", t);
        }
        try {
            updateNotificationUi();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Erro em updateNotificationUi", t);
        }
        try {
            updateMasterToggleUi();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Erro em updateMasterToggleUi", t);
        }
        try {
            loadSelectedAppsOnly();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Erro em loadSelectedAppsOnly", t);
        }
    }

    private void initViews() {
        // Header
        tvHeaderDot = findViewById(R.id.tv_header_dot);
        tvHeaderStatus = findViewById(R.id.tv_header_status);
        btnRefresh = findViewById(R.id.btn_refresh);

        // Abas
        tabBtnProtection = findViewById(R.id.tab_btn_protection);
        tabBtnTutorial = findViewById(R.id.tab_btn_tutorial);
        tabBtnHistory = findViewById(R.id.tab_btn_history);
        viewTabProtection = findViewById(R.id.view_tab_protection);
        viewTabTutorial = findViewById(R.id.view_tab_tutorial);
        viewTabHistory = findViewById(R.id.view_tab_history);

        // Master
        btnMasterToggle = findViewById(R.id.btn_master_toggle);
        imgMasterPower = findViewById(R.id.img_master_power);
        tvMasterTitle = findViewById(R.id.tv_master_title);
        tvMasterSubtitle = findViewById(R.id.tv_master_subtitle);
        switchMaster = findViewById(R.id.switch_master);

        // Perms
        tvIconSecure = findViewById(R.id.tv_icon_secure);
        tvStatusSecure = findViewById(R.id.tv_status_secure);
        boxSecurePendingActions = findViewById(R.id.box_secure_pending_actions);
        btnCopyAdbQuick = findViewById(R.id.btn_copy_adb_quick);
        btnOpenTutorialTab = findViewById(R.id.btn_open_tutorial_tab);
        tvIconBattery = findViewById(R.id.tv_icon_battery);
        btnFixBattery = findViewById(R.id.btn_fix_battery);
        switchBoot = findViewById(R.id.switch_boot);
        tvIconNotification = findViewById(R.id.tv_icon_notification);
        tvNotificationSubtitle = findViewById(R.id.tv_notification_subtitle);
        btnNotificationSettings = findViewById(R.id.btn_notification_settings);
        switchNotification = findViewById(R.id.switch_notification);

        // Apps Dashboard
        tvSelectedAppsCount = findViewById(R.id.tv_selected_apps_count);
        tvEmptySelectedApps = findViewById(R.id.tv_empty_selected_apps);
        recyclerSelectedApps = findViewById(R.id.recycler_selected_apps);
        btnOpenAppPicker = findViewById(R.id.btn_open_app_picker);

        // Actions
        btnActionCycle = findViewById(R.id.btn_action_cycle);
        btnActionEnableAll = findViewById(R.id.btn_action_enable_all);
        btnActionDisableAll = findViewById(R.id.btn_action_disable_all);

        // Scheduling
        spinnerInterval = findViewById(R.id.spinner_interval);
        switchDailyFixed = findViewById(R.id.switch_daily_fixed);
        boxFixedTimePicker = findViewById(R.id.box_fixed_time_picker);
        btnFixedTime = findViewById(R.id.btn_fixed_time);

        // Tutorial
        btnTutorialCopyAdb = findViewById(R.id.btn_tutorial_copy_adb);
        btnTutorialVideoYoutube = findViewById(R.id.btn_tutorial_video_youtube);
        btnTutorialAiPrompt = findViewById(R.id.btn_tutorial_ai_prompt);
        btnTutorialShareWhatsapp = findViewById(R.id.btn_tutorial_share_whatsapp);

        // Logs
        tvLogsConsole = findViewById(R.id.tv_logs_console);
        tvLogsConsole.setMovementMethod(new ScrollingMovementMethod());
        btnLogsCopy = findViewById(R.id.btn_logs_copy);
        btnLogsShareZip = findViewById(R.id.btn_logs_share_zip);
        btnLogsClear = findViewById(R.id.btn_logs_clear);

        btnRefresh.setOnClickListener(v -> {
            refreshAllData();
            Toast.makeText(this, "Painel atualizado", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabs() {
        tabBtnProtection.setOnClickListener(v -> switchTab(0));
        tabBtnTutorial.setOnClickListener(v -> switchTab(1));
        tabBtnHistory.setOnClickListener(v -> switchTab(2));
        btnOpenTutorialTab.setOnClickListener(v -> switchTab(1));
    }

    private void switchTab(int index) {
        int colorActiveBg = 0xFF00FF66;
        int colorActiveText = 0xFF09090B;
        int colorInactiveBg = 0xFF242630;
        int colorInactiveText = 0xFFF5F5F7;

        viewTabProtection.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewTabTutorial.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewTabHistory.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        tabBtnProtection.setBackgroundTintList(ColorStateList.valueOf(index == 0 ? colorActiveBg : colorInactiveBg));
        tabBtnProtection.setTextColor(index == 0 ? colorActiveText : colorInactiveText);

        tabBtnTutorial.setBackgroundTintList(ColorStateList.valueOf(index == 1 ? colorActiveBg : colorInactiveBg));
        tabBtnTutorial.setTextColor(index == 1 ? colorActiveText : colorInactiveText);

        tabBtnHistory.setBackgroundTintList(ColorStateList.valueOf(index == 2 ? colorActiveBg : colorInactiveBg));
        tabBtnHistory.setTextColor(index == 2 ? colorActiveText : colorInactiveText);
    }

    private void setupMasterToggle() {
        btnMasterToggle.setOnClickListener(v -> {
            boolean currentState = A11yWatchdogService.isServiceRunning() || PreferencesHelper.isWatchdogEnabled(this);
            toggleGuard(!currentState);
        });
    }

    private void toggleGuard(boolean enable) {
        PreferencesHelper.setWatchdogEnabled(this, enable);

        if (enable) {
            Intent startIntent = new Intent(this, A11yWatchdogService.class);
            startIntent.setAction(A11yWatchdogService.ACTION_START);
            ContextCompat.startForegroundService(this, startIntent);
            Toast.makeText(this, "🛡️ Proteção Ligada!", Toast.LENGTH_SHORT).show();
        } else {
            Intent stopIntent = new Intent(this, A11yWatchdogService.class);
            stopIntent.setAction(A11yWatchdogService.ACTION_STOP);
            startService(stopIntent);
            Toast.makeText(this, "Proteção Desligada.", Toast.LENGTH_SHORT).show();
        }

        updateMasterToggleUi();
    }

    private void updateMasterToggleUi() {
        boolean running = A11yWatchdogService.isServiceRunning() || PreferencesHelper.isWatchdogEnabled(this);
        switchMaster.setChecked(running);

        int colorNeon = ContextCompat.getColor(this, R.color.neon_green);
        int colorRed = ContextCompat.getColor(this, R.color.crimson_red);
        int colorDim = ContextCompat.getColor(this, R.color.text_dim);

        if (running) {
            btnMasterToggle.setBackgroundResource(R.drawable.bg_master_btn_on);
            imgMasterPower.setImageResource(R.drawable.ic_power_led);
            tvMasterTitle.setText("PROTEÇÃO LIGADA (24/7)");
            tvMasterTitle.setTextColor(colorNeon);

            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            int interval = PreferencesHelper.getCheckIntervalSeconds(this);
            tvMasterSubtitle.setText(monitored.size() + " app(s) sob vigilância contínua (a cada " + interval + "s)");

            tvHeaderDot.setTextColor(colorNeon);
            tvHeaderStatus.setText("Protegendo 24/7");
            tvHeaderStatus.setTextColor(colorNeon);
        } else {
            btnMasterToggle.setBackgroundResource(R.drawable.bg_btn_action_crimson);
            imgMasterPower.setImageResource(R.drawable.ic_power_led_off);
            tvMasterTitle.setText("PROTEÇÃO DESLIGADA");
            tvMasterTitle.setTextColor(colorRed);
            tvMasterSubtitle.setText("Toque aqui para LIGAR o Guardião 24/7");

            tvHeaderDot.setTextColor(colorDim);
            tvHeaderStatus.setText("Desligado");
            tvHeaderStatus.setTextColor(colorDim);
        }
    }

    private void setupPermissions() {
        btnCopyAdbQuick.setOnClickListener(v -> {
            String cmd = "adb shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";
            copyToClipboard(cmd, "Comando ADB copiado!");
        });

        btnFixBattery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ignored) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception ignored2) {}
                    }
                }
            }
        });

        switchBoot.setChecked(PreferencesHelper.isStartOnBoot(this));
        switchBoot.setOnCheckedChangeListener((buttonView, isChecked) -> PreferencesHelper.setStartOnBoot(this, isChecked));

        setupNotificationSection();
    }

    private void setupNotificationSection() {
        if (switchNotification == null || btnNotificationSettings == null) return;

        updateNotificationUi();

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // Ignora alterações programáticas

            if (isChecked) {
                if (!areNotificationsEffectivelyEnabled(this)) {
                    showEnableNotificationDialog();
                    switchNotification.setChecked(false);
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                    }
                }
                PreferencesHelper.setShowNotification(this, true);
                updateNotificationUi();
                notifyServiceReload();
                Toast.makeText(this, "Notificação ativada na barra", Toast.LENGTH_SHORT).show();
            } else {
                PreferencesHelper.setShowNotification(this, false);
                updateNotificationUi();
                notifyServiceReload();
                showHideNotificationInfoDialog();
            }
        });

        btnNotificationSettings.setOnClickListener(v -> openNotificationSettings());
    }

    private void updateNotificationUi() {
        if (switchNotification == null || tvNotificationSubtitle == null || tvIconNotification == null || btnNotificationSettings == null) {
            return;
        }

        boolean systemEnabled = areNotificationsEffectivelyEnabled(this);
        boolean prefShow = PreferencesHelper.isShowNotification(this);

        if (!systemEnabled) {
            switchNotification.setChecked(false);
            tvIconNotification.setText("🔕");
            tvNotificationSubtitle.setText("Desativada no sistema Android (Guardião ativo 24/7)");
            btnNotificationSettings.setText("⚙️ Notificações Desativadas no Sistema (OK)");
            btnNotificationSettings.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (!prefShow) {
            switchNotification.setChecked(false);
            tvIconNotification.setText("🔕");
            tvNotificationSubtitle.setText("Oculta da barra (Guardião segue ativo 24/7)");
            btnNotificationSettings.setText("⚙️ Ocultar 100% no Sistema Android");
            btnNotificationSettings.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow));
        } else {
            switchNotification.setChecked(true);
            tvIconNotification.setText("🔔");
            tvNotificationSubtitle.setText("Exibe o status do guardião na barra");
            btnNotificationSettings.setText("⚙️ Ocultar / Gerenciar no Sistema Android");
            btnNotificationSettings.setTextColor(ContextCompat.getColor(this, R.color.accent_blue));
        }
    }

    private boolean areNotificationsEffectivelyEnabled(Context context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel normalChannel = nm.getNotificationChannel(A11yWatchdogService.CHANNEL_ID);
                NotificationChannel silentChannel = nm.getNotificationChannel(A11yWatchdogService.CHANNEL_SILENT_ID);
                if (normalChannel != null && normalChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    if (silentChannel == null || silentChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void openNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void showHideNotificationInfoDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🔕 Notificação Minimizada")
                .setMessage("O ícone do Guardião foi removido da barra de status superior.\n\n"
                        + "💡 Caso queira ocultá-la 100% (inclusive da lista quando puxa a barra para baixo), você pode desativar as notificações deste app no Android.\n\n"
                        + "O Guardião continuará funcionando e protegendo seus apps normalmente 24 horas por dia!")
                .setPositiveButton("Ocultar no Sistema", (dialog, which) -> openNotificationSettings())
                .setNegativeButton("OK, Entendi", null)
                .show();
    }

    private void showEnableNotificationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🔔 Notificações Desativadas")
                .setMessage("As notificações do Rat Acessibilidade estão desativadas nas configurações do celular.\n\n"
                        + "Para exibi-la novamente na barra de status, ative as notificações nas configurações do sistema.")
                .setPositiveButton("Abrir Configurações", (dialog, which) -> openNotificationSettings())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updatePermissionStatus() {
        try {
            boolean hasSecure = A11yManager.hasWriteSecureSettings(this);
            boolean batteryIgnored = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
                } catch (Throwable ignored) {}
            }

            if (tvIconSecure != null && tvStatusSecure != null && boxSecurePendingActions != null) {
                if (hasSecure) {
                    tvIconSecure.setText("✅");
                    tvStatusSecure.setText("CONCEDIDA");
                    tvStatusSecure.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                    tvStatusSecure.setBackgroundResource(R.drawable.bg_status_badge_green);
                    boxSecurePendingActions.setVisibility(View.GONE);
                } else {
                    tvIconSecure.setText("❌");
                    tvStatusSecure.setText("PENDENTE NO PC");
                    tvStatusSecure.setTextColor(ContextCompat.getColor(this, R.color.crimson_red));
                    tvStatusSecure.setBackgroundResource(R.drawable.bg_status_badge_red);
                    boxSecurePendingActions.setVisibility(View.VISIBLE);
                }
            }

            if (tvIconBattery != null && btnFixBattery != null) {
                if (batteryIgnored) {
                    tvIconBattery.setText("✅");
                    btnFixBattery.setText("ISENTO (OK)");
                    btnFixBattery.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                } else {
                    tvIconBattery.setText("⚠️");
                    btnFixBattery.setText("DESATIVAR");
                    btnFixBattery.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow));
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Erro em updatePermissionStatus", t);
        }
    }

    private void setupAppPickerSection() {
        recyclerSelectedApps.setLayoutManager(new LinearLayoutManager(this));
        selectedAppsAdapter = new SelectedAppsAdapter(this, item -> {
            if (item != null) {
                PreferencesHelper.removeMonitoredService(this, item.getId());
                LogManager.log(this, "Removido da proteção: " + item.getAppName());
                loadSelectedAppsOnly();
                notifyServiceReload();
            }
        });
        recyclerSelectedApps.setAdapter(selectedAppsAdapter);

        btnOpenAppPicker.setOnClickListener(v -> openAppPickerDialog());
    }

    private void loadSelectedAppsOnly() {
        new Thread(() -> {
            try {
                Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
                List<ServiceItem> allInstalled = A11yManager.getInstalledServices(this, monitored);
                List<ServiceItem> onlySelected = new ArrayList<>();

                if (allInstalled != null) {
                    for (ServiceItem item : allInstalled) {
                        if (item != null && item.isMonitored()) {
                            onlySelected.add(item);
                        }
                    }
                }

                runOnUiThread(() -> {
                    try {
                        if (tvSelectedAppsCount != null) {
                            tvSelectedAppsCount.setText("APLICATIVOS PROTEGIDOS (" + onlySelected.size() + ")");
                        }
                        if (onlySelected.isEmpty()) {
                            if (tvEmptySelectedApps != null) tvEmptySelectedApps.setVisibility(View.VISIBLE);
                            if (recyclerSelectedApps != null) recyclerSelectedApps.setVisibility(View.GONE);
                        } else {
                            if (tvEmptySelectedApps != null) tvEmptySelectedApps.setVisibility(View.GONE);
                            if (recyclerSelectedApps != null) {
                                recyclerSelectedApps.setVisibility(View.VISIBLE);
                                if (selectedAppsAdapter != null) {
                                    selectedAppsAdapter.setData(onlySelected);
                                }
                            }
                        }
                        updateMasterToggleUi();
                    } catch (Throwable t) {
                        android.util.Log.e("MainActivity", "Erro ao atualizar UI dos apps", t);
                    }
                });
            } catch (Throwable t) {
                android.util.Log.e("MainActivity", "Erro ao carregar apps na thread de fundo", t);
            }
        }).start();
    }

    private void openAppPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_apps);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etSearch = dialog.findViewById(R.id.et_dialog_search);
        RecyclerView recycler = dialog.findViewById(R.id.recycler_dialog_apps);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_dialog_save);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        ServiceListAdapter dialogAdapter = new ServiceListAdapter(this, (item, isMonitored) -> {
            if (item != null) {
                if (isMonitored) {
                    PreferencesHelper.addMonitoredService(this, item.getId());
                    LogManager.log(this, "Adicionado: " + item.getAppName());
                } else {
                    PreferencesHelper.removeMonitoredService(this, item.getId());
                    LogManager.log(this, "Removido: " + item.getAppName());
                }
            }
        });
        recycler.setAdapter(dialogAdapter);

        new Thread(() -> {
            try {
                Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
                List<ServiceItem> list = A11yManager.getInstalledServices(this, monitored);
                runOnUiThread(() -> {
                    try {
                        dialogAdapter.setData(list);
                    } catch (Throwable ignored) {}
                });
            } catch (Throwable t) {
                android.util.Log.e("MainActivity", "Erro ao carregar diálogo de apps", t);
            }
        }).start();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                dialogAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            loadSelectedAppsOnly();
            notifyServiceReload();
            Toast.makeText(this, "Lista de proteção atualizada!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void setupActionButtons() {
        // 1. LIGAR TODOS (APENAS APPS DA SUA LISTA DE PROTEGIDOS)
        btnActionEnableAll.setOnClickListener(v -> {
            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            if (monitored.isEmpty()) {
                Toast.makeText(this, "Toque em 'ESCOLHER APLICATIVOS' para marcar seus apps!", Toast.LENGTH_LONG).show();
                openAppPickerDialog();
                return;
            }
            Toast.makeText(this, "Ligando aplicativos da sua lista...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean ok = A11yManager.enableServices(this, monitored);
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, "🟢 Todos os apps da sua lista foram LIGADOS! (" + monitored.size() + " apps)", Toast.LENGTH_LONG).show();
                        LogManager.log(this, "🟢 Todos os apps protegidos da lista foram ligados (" + monitored.size() + " apps).");
                        loadSelectedAppsOnly();
                    } else {
                        Toast.makeText(this, "⚠️ Conceda a permissão ADB primeiro!", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        // 2. DESLIGAR TODOS (APENAS APPS DA SUA LISTA)
        btnActionDisableAll.setOnClickListener(v -> {
            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            if (monitored.isEmpty()) {
                Toast.makeText(this, "Nenhum aplicativo protegido na lista.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Desligando aplicativos da sua lista...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean ok = A11yManager.disableServices(this, monitored);
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, "⛔ Apps da sua lista foram DESLIGADOS!", Toast.LENGTH_LONG).show();
                        LogManager.log(this, "⛔ Todos os apps da sua lista foram desligados.");
                        loadSelectedAppsOnly();
                    } else {
                        Toast.makeText(this, "⚠️ Conceda a permissão ADB primeiro!", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        // Clique longo em DESLIGAR TODOS: Desativa geral (emergência)
        btnActionDisableAll.setOnLongClickListener(v -> {
            Toast.makeText(this, "⛔ Desligando absolutamente tudo do sistema...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean ok = A11yManager.disableAllServices(this);
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, "⛔ Todos os serviços de acessibilidade foram desligados!", Toast.LENGTH_LONG).show();
                        LogManager.log(this, "⛔ Desativação geral executada.");
                        loadSelectedAppsOnly();
                    }
                });
            }).start();
            return true;
        });

        // 3. REINICIAR SERVIÇOS (1s) (Destrava conexões congeladas dos apps da lista)
        btnActionCycle.setOnClickListener(v -> {
            Set<String> monitored = PreferencesHelper.getMonitoredServices(this);
            if (monitored.isEmpty()) {
                Toast.makeText(this, "Toque em 'ESCOLHER APLICATIVOS' primeiro!", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "🔄 Reiniciando serviços (Pausa 1s e religa)...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                A11yManager.cycleRestartServices(this, monitored, 1000);
                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Conexões destravadas!", Toast.LENGTH_SHORT).show();
                    LogManager.log(this, "🔄 Ciclo de destravamento de 1s concluído (" + monitored.size() + " apps).");
                    loadSelectedAppsOnly();
                });
            }).start();
        });
    }

    private void setupScheduling() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, INTERVAL_LABELS);
        spinnerInterval.setAdapter(adapter);

        int currentInterval = PreferencesHelper.getCheckIntervalSeconds(this);
        int selectedIndex = 1; // 30s
        for (int i = 0; i < INTERVAL_VALUES.length; i++) {
            if (INTERVAL_VALUES[i] == currentInterval) {
                selectedIndex = i;
                break;
            }
        }
        spinnerInterval.setSelection(selectedIndex);

        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int chosen = INTERVAL_VALUES[position];
                if (chosen != PreferencesHelper.getCheckIntervalSeconds(MainActivity.this)) {
                    PreferencesHelper.setCheckIntervalSeconds(MainActivity.this, chosen);
                    Toast.makeText(MainActivity.this, INTERVAL_LABELS[position], Toast.LENGTH_SHORT).show();
                    notifyServiceReload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean dailyEnabled = PreferencesHelper.isDailyCycleEnabled(this);
        switchDailyFixed.setChecked(dailyEnabled);
        boxFixedTimePicker.setVisibility(dailyEnabled ? View.VISIBLE : View.GONE);
        updateFixedTimeButtonLabel();

        switchDailyFixed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferencesHelper.setDailyCycleEnabled(this, isChecked);
            boxFixedTimePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                DailyCycleScheduler.schedule(this);
                Toast.makeText(this, "Destravamento diário ativado", Toast.LENGTH_SHORT).show();
            } else {
                DailyCycleScheduler.cancel(this);
                Toast.makeText(this, "Destravamento diário desativado", Toast.LENGTH_SHORT).show();
            }
        });

        btnFixedTime.setOnClickListener(v -> {
            int h = PreferencesHelper.getDailyCycleHour(this);
            int m = PreferencesHelper.getDailyCycleMinute(this);
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                PreferencesHelper.setDailyCycleHour(this, hourOfDay);
                PreferencesHelper.setDailyCycleMinute(this, minute);
                updateFixedTimeButtonLabel();
                DailyCycleScheduler.schedule(this);
                Toast.makeText(this, "Horário salvo!", Toast.LENGTH_SHORT).show();
            }, h, m, true).show();
        });
    }

    private void updateFixedTimeButtonLabel() {
        int h = PreferencesHelper.getDailyCycleHour(this);
        int m = PreferencesHelper.getDailyCycleMinute(this);
        btnFixedTime.setText(String.format(Locale.getDefault(), "⏰ %02d:%02d DA MANHÃ", h, m));
    }

    private void setupTutorialSection() {
        String cmd = "adb shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";

        btnTutorialCopyAdb.setOnClickListener(v -> copyToClipboard(cmd, "Comando ADB copiado com sucesso!"));

        btnTutorialVideoYoutube.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=NPzyF_HK9fw"));
                startActivity(intent);
            } catch (Exception e) {
                Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=como+instalar+adb+e+fastboot+no+pc+passo+a+passo"));
                startActivity(searchIntent);
            }
        });

        btnTutorialAiPrompt.setOnClickListener(v -> {
            String prompt = "Olá! Preciso da sua ajuda para conceder a permissão WRITE_SECURE_SETTINGS para o aplicativo \"Rat Acessibilidade\" no meu celular Android.\n\n"
                    + "Informações do aplicativo:\n"
                    + "- Nome: Rat Acessibilidade\n"
                    + "- Pacote: " + getPackageName() + "\n"
                    + "- Comando exato para rodar no terminal do computador:\n"
                    + cmd + "\n\n"
                    + "Por favor, me explique de forma muito simples e passo a passo:\n"
                    + "1. Me pergunte qual é o sistema operacional do meu PC (Windows, Mac ou Linux).\n"
                    + "2. Como baixar e extrair o ADB (Android Platform-Tools) no meu computador.\n"
                    + "3. Como ativar a Depuração USB no meu celular Android.\n"
                    + "4. Como conectar o cabo USB, autorizar na tela do celular e rodar o comando acima.\n"
                    + "5. Como resolver erros comuns (como 'device unauthorized' ou 'command not found').";

            copyToClipboard(prompt, "🤖 Prompt para IA copiado! Cole no ChatGPT, Claude ou Gemini.");
        });

        btnTutorialShareWhatsapp.setOnClickListener(v -> {
            String msg = "🛡️ *Como ativar o Rat Acessibilidade no seu aparelho:*\n\n"
                    + "1. Ative a *Depuração USB* nas Configurações > Opções do Desenvolvedor.\n"
                    + "2. Baixe o ADB no PC e conecte o cabo USB.\n"
                    + "3. Cole este comando no Prompt de Comando (CMD) do PC:\n\n"
                    + "`" + cmd + "`\n\n"
                    + "🎥 Vídeo ensinando passo a passo no YouTube: https://www.youtube.com/watch?v=NPzyF_HK9fw\n\n"
                    + "4. Abra o app, clique em 'ESCOLHER APLICATIVOS' e LIGUE o Guardião!";

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(share, "Enviar tutorial para..."));
        });
    }

    private void setupHistorySection() {
        btnLogsCopy.setOnClickListener(v -> copyToClipboard(LogManager.getFormattedLogs(), "Histórico copiado!"));
        if (btnLogsShareZip != null) {
            btnLogsShareZip.setOnClickListener(v -> {
                String currentLogs = tvLogsConsole != null ? tvLogsConsole.getText().toString() : "";
                DiagnosticManager.shareDiagnosticZip(this, currentLogs);
            });
        }
        btnLogsClear.setOnClickListener(v -> {
            LogManager.clear(this);
            Toast.makeText(this, "Histórico limpo", Toast.LENGTH_SHORT).show();
        });
    }

    private void notifyServiceReload() {
        if (switchMaster.isChecked()) {
            Intent reloadIntent = new Intent(this, A11yWatchdogService.class);
            reloadIntent.setAction(A11yWatchdogService.ACTION_RELOAD);
            startService(reloadIntent);
        }
        updateMasterToggleUi();
    }

    private void copyToClipboard(String text, String toastMsg) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData clip = ClipData.newPlainText("Rat Acessibilidade", text);
            cm.setPrimaryClip(clip);
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }
}
