package com.vibecoder.purrytify.presentation.features.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.data.local.model.SongEntity

class SongAdapter(

    private var songs: List<SongEntity>,

    private val onItemClick: (SongEntity) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val songCover: ImageView = itemView.findViewById(R.id.songCover)
        private val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        private val songArtist: TextView = itemView.findViewById(R.id.songArtist)


        fun bind(song: SongEntity) {
            songTitle.text = song.title
            songArtist.text = song.artist

            songCover.load(song.coverArtUri) {
                crossfade(true)
                placeholder(R.drawable.ic_song_placeholder)
                error(R.drawable.ic_song_placeholder)
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


    fun updateSongs(newSongs: List<SongEntity>) {

        songs = newSongs
        notifyDataSetChanged()
    }
}