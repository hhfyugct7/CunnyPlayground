package com.thevakhovske.cunnyplayground

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.util.Log

class MediaControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.thevakhovske.cunnyplayground.MEDIA_PLAY_PAUSE"
        const val ACTION_NEXT = "com.thevakhovske.cunnyplayground.MEDIA_NEXT"
        const val ACTION_PREV = "com.thevakhovske.cunnyplayground.MEDIA_PREV"
        const val ACTION_SEEK = "com.thevakhovske.cunnyplayground.MEDIA_SEEK"
        const val ACTION_CUSTOM = "com.thevakhovske.cunnyplayground.MEDIA_CUSTOM"
        const val EXTRA_TOKEN = "media_session_token"
        const val EXTRA_SEEK_PERCENT = "seek_percent"
        const val EXTRA_CUSTOM_ACTION = "custom_action"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val castId = intent.getIntExtra("cast_id", -1)
        @Suppress("DEPRECATION")
        val token = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_TOKEN, MediaSession.Token::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_TOKEN)
        }

        try {
            val controller = (if (castId != -1) NotificationCastListener.activeControllers[castId] else null)
                ?: token?.let { MediaController(context, it) }
                ?: return

            val transportControls = controller.transportControls
            val state = controller.playbackState

            Log.d("MediaControlReceiver", "onReceive action=$action castId=$castId state=${state?.state}")

            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                        transportControls.pause()
                    } else {
                        transportControls.play()
                    }
                }
                ACTION_NEXT -> transportControls.skipToNext()
                ACTION_PREV -> transportControls.skipToPrevious()
                ACTION_SEEK -> {
                    val percent = intent.getIntExtra(EXTRA_SEEK_PERCENT, -1)
                    val duration = controller.metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                    if (percent in 0..100 && duration > 0) {
                        transportControls.seekTo(duration * percent / 100)
                    }
                }
                ACTION_CUSTOM -> {
                    // A passthrough for the real player's custom actions (like/shuffle/etc.).
                    val custom = intent.getStringExtra(EXTRA_CUSTOM_ACTION)
                    if (custom != null) transportControls.sendCustomAction(custom, null)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaControlReceiver", "Failed to dispatch media control", e)
        }
    }
}
