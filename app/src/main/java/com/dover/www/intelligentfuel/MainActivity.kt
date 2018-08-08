package com.dover.www.intelligentfuel

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.*
import com.dover.www.intelligentfuel.common.VideoPlayer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private var videoPlayer: VideoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        playVideos()
    }

    private fun playVideos() {
        // video display should obey percentage 16:9
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val screenWidth = dm.widthPixels
        val videoViewBoxHeight = screenWidth / 16 * 9 + 140 // 上下增加 70dp 的黑边
        mVideoViewBox.layoutParams = LinearLayout.LayoutParams(screenWidth, videoViewBoxHeight)
        RobinApplication.log(TAG, videoViewBoxHeight.toString())
        // get all video files
        videoPlayer = VideoPlayer(this@MainActivity, mVideoView, Environment.getExternalStorageDirectory().absolutePath + "/Tokheim/AdVideo", mVideoIndexBox)
        videoPlayer?.prepare()
        videoPlayer?.start()
    }
}
