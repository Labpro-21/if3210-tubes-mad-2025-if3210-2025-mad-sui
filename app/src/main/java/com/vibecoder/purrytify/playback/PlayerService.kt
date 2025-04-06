package com.vibecoder.purrytify.playback

import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint


private const val TAG = "PlayerService"


@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private lateinit var player: ExoPlayer


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        initializePlayerAndSession()
    }

    private fun initializePlayerAndSession() {

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()


        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG,"Player state changed: $playbackState")

            }

        })



        mediaSession = MediaSession.Builder(this, player)
            .build()

        Log.d(TAG, "Player and MediaSession initialized.")
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession for controller: ${controllerInfo.packageName}")
        return mediaSession
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession?.run {

            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }


}