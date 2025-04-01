package com.example.pipcompose

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.pipcompose.ui.theme.PIPComposeTheme

class MainActivity : ComponentActivity() {
    private var videoViewBounds = Rect()
    private lateinit var exoPlayer: ExoPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PIPComposeTheme {
                val context = LocalContext.current
                val videoPath = ""
                // ExoPlayer Instance
                exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem =
                            MediaItem.fromUri("android.resource://$context.packageName/${videoPath}".toUri())
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true  // Auto-play video
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.stop()
                        exoPlayer.release() // Free resources
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false // Hide controls
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL // Stretch video
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()  // Stretch width
                        .height(300.dp)  // Fixed height
                        .background(Color.Black) // Background color
                )
            }
        }
    }

    class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            println("Clicked on PIP action")
        }
    }

    private val isPipSupported by lazy {
        packageManager.hasSystemFeature(
            PackageManager.FEATURE_PICTURE_IN_PICTURE
        )
    }

    private fun updatedPipParams(): PictureInPictureParams? {
        return PictureInPictureParams.Builder()
            .setSourceRectHint(videoViewBounds)
            .setAspectRatio(Rational(16, 9))
            .setActions(
                listOf(
                    RemoteAction(
                        Icon.createWithResource(
                            applicationContext,
                            R.drawable.ic_library_add
                        ),
                        "Add to library",
                        "Add to library",
                        PendingIntent.getBroadcast(
                            applicationContext,
                            0,
                            Intent(applicationContext, MyReceiver::class.java),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                )
            )
            .build()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            // User closed PIP mode (dragging it to dismiss)
            stopAndReleaseExoPlayer()
        }

    }

    private fun stopAndReleaseExoPlayer() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isPipSupported) {
            return
        }
        updatedPipParams()?.let { params ->
            enterPictureInPictureMode(params)
        }
    }

}
