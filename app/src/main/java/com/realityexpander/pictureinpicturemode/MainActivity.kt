package com.realityexpander.pictureinpicturemode

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.ProgressDialog
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.realityexpander.pictureinpicturemode.ui.theme.PictureInPictureModeTheme

// Articles
// https://en.proft.me/2018/05/30/android-picture-picture-mode/
// https://medium.com/code-procedure-and-rants/android-picture-in-picture-28f8ca61bcf
// https://blog.logrocket.com/implementing-picture-in-picture-mode-android/
// https://developer.android.com/guide/topics/ui/picture-in-picture

// Google sample
// https://github.com/googlearchive/android-PictureInPicture/blob/master/app/src/main/java/com/example/android/pictureinpicture/MainActivity.java

// Other samples
// https://www.geeksforgeeks.org/how-to-play-video-from-url-in-android/

class MainActivity : ComponentActivity() {


    companion object {
        lateinit var mediaPlayer: MediaPlayer
    }

    class MyReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            println("Clicked on PIP action")

            if(mediaPlayer.isPlaying)
                mediaPlayer.pause()
            else
                mediaPlayer.start()

        }
    }

    private val isPipSupported by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            )
        } else {
            false
        }
    }

    private var videoViewBounds = Rect()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PictureInPictureModeTheme {

                val context = LocalContext.current

                //progressDialog = ProgressDialog(context)
                val progressDialog = ProgressDialog.show(context, "", "Loading Video…")

                AndroidView( // XML
                    factory = {
                        VideoView(it, null).apply {
                            //setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.sample}"))
//                            setVideoURI(Uri.parse("https://media.geeksforgeeks.org/wp-content/uploads/20201217192146/Screenrecorder-2020-12-17-19-17-36-828.mp4"))
                            setVideoURI(Uri.parse("https://images.all-free-download.com/footage_preview/mp4/horse_joyful_on_grass_farmland_6892377.mp4"))

                            val mediaController = MediaController(context)

                            // anchor view for the videoView
                            mediaController.setAnchorView(this)

                            // sets the media player to the videoView
                            mediaController.setMediaPlayer(this)

                            // sets the media controller to the videoView
                            setMediaController(mediaController)

                            setOnPreparedListener { mp ->
                                mediaPlayer = mp
                                progressDialog.dismiss()

                                mediaPlayer.isLooping = true
                                start()

                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            videoViewBounds = it
                                .boundsInWindow()
                                .toAndroidRect()
                        }
                )
            }
        }
    }

    private fun updatedPipParams(): PictureInPictureParams? {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder()
                .setSourceRectHint(videoViewBounds)
                .setAspectRatio(Rational(16, 9))
                .setActions(
                    listOf(
                        RemoteAction(
                            Icon.createWithResource(
                                applicationContext,
                                R.drawable.ic_baseline_pause_24
                            ),
                            "Pause",
                            "Pause",
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
        } else
            null // cant use PIP below API 26 (Oreo)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(!isPipSupported) {
            return
        }
        updatedPipParams()?.let { params ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(params)
            }
        }
    }
}