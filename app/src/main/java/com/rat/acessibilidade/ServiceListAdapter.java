package com.rat.acessibilidade;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceListAdapter.ViewHolder> {

    public interface OnMonitorToggleListener {
        void onToggle(ServiceItem item, boolean isMonitored);
    }

    private final Context context;
    private final List<ServiceItem> fullList = new ArrayList<>();
    private final List<ServiceItem> displayList = new ArrayList<>();
    private final OnMonitorToggleListener toggleListener;

    public ServiceListAdapter(Context context, OnMonitorToggleListener toggleListener) {
        this.context = context;
        this.toggleListener = toggleListener;
    }

    public void setData(List<ServiceItem> items) {
        fullList.clear();
        displayList.clear();
        if (items != null) {
            fullList.addAll(items);
            displayList.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lower = query.toLowerCase(Locale.getDefault());
            for (ServiceItem item : fullList) {
                if (item.getAppName().toLowerCase(Locale.getDefault()).contains(lower)
                        || item.getPackageName().toLowerCase(Locale.getDefault()).contains(lower)
                        || item.getServiceLabel().toLowerCase(Locale.getDefault()).contains(lower)) {
                    displayList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<ServiceItem> getFullList() {
        return fullList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceItem item = displayList.get(position);

        if (item.getIcon() != null) {
            holder.imgIcon.setImageDrawable(item.getIcon());
        } else {
            holder.imgIcon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.txtAppName.setText(item.getAppName());
        holder.txtServiceName.setText(item.getId());

        if (item.isActive()) {
            holder.txtLiveStatus.setText("ATIVO");
            holder.txtLiveStatus.setTextColor(ContextCompat.getColor(context, R.color.neon_green));
            holder.txtLiveStatus.setBackgroundResource(R.drawable.bg_status_badge_green);
        } else {
            holder.txtLiveStatus.setText("DESLIGADO");
            holder.txtLiveStatus.setTextColor(ContextCompat.getColor(context, R.color.crimson_red));
            holder.txtLiveStatus.setBackgroundResource(R.drawable.bg_status_badge_red);
        }

        // Evita disparar o listener durante o bind
        holder.switchMonitor.setOnCheckedChangeListener(null);
        holder.switchMonitor.setChecked(item.isMonitored());

        holder.switchMonitor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setMonitored(isChecked);
            if (toggleListener != null) {
                toggleListener.onToggle(item, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.switchMonitor.toggle());
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtAppName;
        TextView txtServiceName;
        TextView txtLiveStatus;
        SwitchMaterial switchMonitor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_app_icon);
            txtAppName = itemView.findViewById(R.id.txt_app_name);
            txtServiceName = itemView.findViewById(R.id.txt_service_name);
            txtLiveStatus = itemView.findViewById(R.id.txt_live_status);
            switchMonitor = itemView.findViewById(R.id.switch_monitor);
        }
    }
}
