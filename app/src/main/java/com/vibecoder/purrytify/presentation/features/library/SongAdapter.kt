package com.vibecoder.purrytify.presentation.features.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.data.local.model.SongEntity

class SongAdapter(
        private var songs: List<SongEntity>,
        private var currentSongId: Long = -1,
        private var isPlaying: Boolean = false,
        private val onItemClick: (SongEntity) -> Unit,
        private val onPlayPauseClick: (SongEntity) -> Unit,
        private val onMoreOptionsClick: (SongEntity) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songCover: ImageView = itemView.findViewById(R.id.songCover)
        private val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        private val songArtist: TextView = itemView.findViewById(R.id.songArtist)
        private val playPauseButton: ImageButton = itemView.findViewById(R.id.playPauseButton)
        private val moreOptionsButton: ImageButton = itemView.findViewById(R.id.moreOptionsButton)

        fun bind(song: SongEntity) {
            val isCurrentSong = song.id == currentSongId
            val isSongPlaying = isCurrentSong && isPlaying

            songTitle.text = song.title
            songArtist.text = song.artist

            songCover.load(song.coverArtUri) {
                crossfade(true)
                placeholder(R.drawable.ic_song_placeholder)
                error(R.drawable.ic_song_placeholder)
            }

            // Update play/pause button icon
            playPauseButton.setImageResource(
                    when {
                        isSongPlaying -> R.drawable.ic_pause
                        else -> R.drawable.ic_play
                    }
            )

            // Set click listeners
            itemView.setOnClickListener { onItemClick(song) }
            playPauseButton.setOnClickListener { onPlayPauseClick(song) }
            moreOptionsButton.setOnClickListener { onMoreOptionsClick(song) }

            // Update colors based on current song
            if (isCurrentSong) {
                songTitle.setTextColor(itemView.context.getColor(R.color.green))
                songArtist.setTextColor(itemView.context.getColor(R.color.green))
            } else {
                songTitle.setTextColor(itemView.context.getColor(R.color.white))
                songArtist.setTextColor(itemView.context.getColor(R.color.gray))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<SongEntity>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun updateCurrentSong(songId: Long, playing: Boolean) {
        if (currentSongId != songId || isPlaying != playing) {
            currentSongId = songId
            isPlaying = playing
            notifyDataSetChanged()
        }
    }
}
