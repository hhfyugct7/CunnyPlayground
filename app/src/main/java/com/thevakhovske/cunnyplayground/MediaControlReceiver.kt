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
        const val EXTRA_TOKEN = "media_session_token"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val token = intent.getParcelableExtra<MediaSession.Token>(EXTRA_TOKEN) ?: return

        try {
            val controller = MediaController(context, token)
            val transportControls = controller.transportControls

            when (action) {
                ACTION_PLAY_PAUSE -> {
                    val state = controller.playbackState
                    if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                        transportControls.pause()
                    } else {
                        transportControls.play()
                    }
                }
                ACTION_NEXT -> transportControls.skipToNext()
                ACTION_PREV -> transportControls.skipToPrevious()
            }
        } catch (e: Exception) {
            Log.e("MediaControlReceiver", "Failed to dispatch media control", e)
        }
    }
}
