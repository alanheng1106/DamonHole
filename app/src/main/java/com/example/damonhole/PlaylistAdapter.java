package com.example.damonhole;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlayClick(Playlist playlist);
        void onDeleteClick(Playlist playlist);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvName.setText(playlist.name);

        String author = playlist.author;
        if (author != null && (author.equalsIgnoreCase("Me") || author.equalsIgnoreCase("Saya") || author.equals("我"))) {
            author = holder.itemView.getContext().getString(R.string.me);
        }

        String detail = holder.itemView.getContext().getString(
                R.string.playlist_details,
                author,
                playlist.songs.size()
        );
        holder.tvDetails.setText(detail);

        // Row click on the card (not the RelativeLayout) so it doesn't
        // compete with the FAB's touch target
        holder.card.setOnClickListener(v -> listener.onPlaylistClick(playlist));

        // FAB click — stopPropagation not needed since the card click is on
        // the card itself and the FAB is a separate view on top
        holder.btnPlay.setOnClickListener(v -> listener.onPlayClick(playlist));

        // Delete button click
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(playlist));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvDetails;
        FloatingActionButton btnPlay;
        View btnDelete;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardPlaylist);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvDetails = itemView.findViewById(R.id.tvPlaylistDetails);
            btnPlay = itemView.findViewById(R.id.btnPlayPlaylist);
            btnDelete = itemView.findViewById(R.id.btnDeletePlaylist);
        }
    }
}