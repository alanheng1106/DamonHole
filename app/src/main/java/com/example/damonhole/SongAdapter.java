package com.example.damonhole;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<SongItem> songs = new ArrayList<>();
    private final OnSongClickListener listener;
    private boolean showDragHandle = false;

    public interface OnSongClickListener {
        void onSongClick(SongItem song);
        void onMoreClick(View view, SongItem song);
    }

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<SongItem> newSongs) {
        // DiffUtil calculates the minimal set of changes — no full redraw
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return songs.size(); }
            @Override public int getNewListSize() { return newSongs.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return songs.get(oldPos).videoId.equals(newSongs.get(newPos).videoId);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                SongItem o = songs.get(oldPos), n = newSongs.get(newPos);
                return o.title.equals(n.title) && o.author.equals(n.author);
            }
        });
        songs = newSongs;
        diffResult.dispatchUpdatesTo(this); // only redraws changed items
    }

    public List<SongItem> getSongs() {
        return songs;
    }

    public void setShowDragHandle(boolean show) {
        this.showDragHandle = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongItem song = songs.get(position);
        holder.tvTitle.setText(song.title);
        holder.tvAuthor.setText(song.author);
        
        String thumbnailUrl = "https://img.youtube.com/vi/" + song.videoId + "/hqdefault.jpg";
        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_music_note)
                .centerCrop()
                .into(holder.ivThumb);
                
        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        holder.btnMore.setOnClickListener(v -> listener.onMoreClick(v, song));

        if (showDragHandle) {
            holder.btnMore.setVisibility(View.GONE);
            holder.ivDragHandle.setVisibility(View.VISIBLE);
        } else {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.ivDragHandle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor;
        MaterialButton btnMore;
        android.widget.ImageView ivDragHandle;
        com.google.android.material.imageview.ShapeableImageView ivThumb;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            btnMore = itemView.findViewById(R.id.btnMore);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            ivThumb = itemView.findViewById(R.id.ivThumb);
        }
    }
}