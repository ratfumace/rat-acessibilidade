package com.rat.acessibilidade;

import android.graphics.drawable.Drawable;

public class ServiceItem {
    private final String id; // packageName/serviceClassName
    private final String packageName;
    private final String serviceClassName;
    private final String appName;
    private final String serviceLabel;
    private final Drawable icon;
    private boolean isActive;
    private boolean isMonitored;

    public ServiceItem(String id, String packageName, String serviceClassName, String appName, String serviceLabel, Drawable icon, boolean isActive, boolean isMonitored) {
        this.id = id;
        this.packageName = packageName;
        this.serviceClassName = serviceClassName;
        this.appName = appName;
        this.serviceLabel = serviceLabel;
        this.icon = icon;
        this.isActive = isActive;
        this.isMonitored = isMonitored;
    }

    public String getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public String getAppName() {
        return appName;
    }

    public String getServiceLabel() {
        return serviceLabel;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public void setMonitored(boolean monitored) {
        isMonitored = monitored;
    }
}
