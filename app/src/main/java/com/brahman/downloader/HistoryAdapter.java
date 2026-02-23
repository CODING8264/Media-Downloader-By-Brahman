package com.brahman.downloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<DownloadItem> items;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public HistoryAdapter(Context context, List<DownloadItem> items) {
        this.context = context;
        this.items = items;
    }

    public void updateItems(List<DownloadItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = items.get(position);

        holder.tvTitle.setText(item.title != null ? item.title : "Unknown");
        holder.tvSub.setText(
                (item.format != null ? item.format.toUpperCase() : "") +
                (item.quality != null && !"best".equals(item.quality) ? " · " + item.quality + "p" : "") +
                (item.filesize != null && !item.filesize.isEmpty() ? " · " + item.filesize : "") +
                " · " + sdf.format(new Date(item.date))
        );

        // Status chip
        if ("done".equals(item.status)) {
            holder.chipStatus.setText("✅ Done");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_done_bg);
        } else if ("failed".equals(item.status)) {
            holder.chipStatus.setText("❌ Failed");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_fail_bg);
        } else {
            holder.chipStatus.setText("⏳ " + item.status);
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_active_bg);
        }

        // Type tag
        holder.tvType.setText(item.isAudio() ? "🎵 AUDIO" : "🎬 VIDEO");

        // Thumbnail
        if (item.thumbnail != null && !item.thumbnail.isEmpty()) {
            Glide.with(context).load(item.thumbnail)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgThumb);
        } else {
            holder.imgThumb.setImageResource(
                    item.isAudio() ? R.drawable.ic_audio : R.drawable.ic_video);
        }

        // Long press to delete
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete")
                    .setMessage("Remove from history?")
                    .setPositiveButton("Delete", (d, w) -> {
                        HistoryManager.getInstance(context).removeItem(item.id);
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_ID) {
                            items.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView tvTitle, tvSub, tvType;
        Chip chipStatus;

        ViewHolder(View view) {
            super(view);
            imgThumb = view.findViewById(R.id.img_thumb);
            tvTitle = view.findViewById(R.id.tv_title);
            tvSub = view.findViewById(R.id.tv_sub);
            tvType = view.findViewById(R.id.tv_type);
            chipStatus = view.findViewById(R.id.chip_status);
        }
    }
}
