package com.rat.acessibilidade;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SelectedAppsAdapter extends RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder> {

    public interface OnRemoveAppListener {
        void onRemove(ServiceItem item);
    }

    private final Context context;
    private final List<ServiceItem> items = new ArrayList<>();
    private final OnRemoveAppListener removeListener;

    public SelectedAppsAdapter(Context context, OnRemoveAppListener removeListener) {
        this.context = context;
        this.removeListener = removeListener;
    }

    public void setData(List<ServiceItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceItem item = items.get(position);

        if (item.getIcon() != null) {
            holder.imgIcon.setImageDrawable(item.getIcon());
        } else {
            holder.imgIcon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.txtAppName.setText(item.getAppName());

        if (item.isActive()) {
            holder.txtLiveStatus.setText("ATIVADO NO CELULAR");
            holder.txtLiveStatus.setTextColor(ContextCompat.getColor(context, R.color.neon_green));
            holder.txtLiveStatus.setBackgroundResource(R.drawable.bg_status_badge_green);
        } else {
            holder.txtLiveStatus.setText("DESLIGADO NO CELULAR");
            holder.txtLiveStatus.setTextColor(ContextCompat.getColor(context, R.color.crimson_red));
            holder.txtLiveStatus.setBackgroundResource(R.drawable.bg_status_badge_red);
        }

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtAppName;
        TextView txtLiveStatus;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_selected_app_icon);
            txtAppName = itemView.findViewById(R.id.txt_selected_app_name);
            txtLiveStatus = itemView.findViewById(R.id.txt_selected_live_status);
            btnRemove = itemView.findViewById(R.id.btn_remove_selected_app);
        }
    }
}
