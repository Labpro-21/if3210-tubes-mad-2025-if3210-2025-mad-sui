package com.vibecoder.purrytify.presentation.features.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.domain.model.Song

class SongAdapter(
    private var songs: List<Song>,
    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songCover: ImageView = itemView.findViewById(R.id.songCover)
        val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        val songArtist: TextView = itemView.findViewById(R.id.songArtist)

        fun bind(song: Song) {
            println("Song title view: $songTitle")
            println("Song artist view: $songArtist")
            songTitle?.text = song.title ?: "No title"
            songArtist?.text = song.artist ?: "No artist"
            songCover.load(song.coverUrl) {
                crossfade(true)

            }
            itemView.setOnClickListener { onItemClick(song) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size


    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}