package com.dover.www.intelligentfuel

import android.content.Context
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.*
import com.dover.www.intelligentfuel.common.VideoPlayer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    companion object {
        const val TAG = "MainActivity"
    }

    private var videoPlayer: VideoPlayer? = null
    private var camera: Camera? = null
    private var surfaceHolder: SurfaceHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceHolder = cameraView.holder
        surfaceHolder?.addCallback(this@MainActivity)
    }

    override fun onPause() {
        super.onPause()
        clearCameraData()
    }

    override fun onResume() {
        super.onResume()
        if (camera == null) {
            camera = getCamera()
            try {
                camera?.setPreviewDisplay(surfaceHolder)
                camera?.startPreview()
            } catch (e: Exception) {
                RobinApplication.log(TAG + "onResume", e.message.toString())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playVideos()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        camera = getCamera()
        try {
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            RobinApplication.log(TAG + "surfaceCreated", e.message.toString())
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        if (surfaceHolder?.surface == null) return
        try {
            camera?.stopPreview()
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            RobinApplication.log(TAG + "surfaceChanged", e.message.toString())
        }
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        surfaceHolder?.removeCallback(this@MainActivity)
        clearCameraData()
    }

    private fun playVideos() {
        // video display should obey percentage 16:9
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val screenWidth = dm.widthPixels
        val videoViewBoxHeight = screenWidth / 16 * 9 + 140 // 上下增加 70dp 的黑边
        mVideoViewBox.layoutParams = LinearLayout.LayoutParams(screenWidth, videoViewBoxHeight)
        // get all video files
        videoPlayer = VideoPlayer(this@MainActivity, mVideoView, Environment.getExternalStorageDirectory().absolutePath + "/Tokheim/AdVideo", mVideoIndexBox)
        videoPlayer?.prepare()
        videoPlayer?.start()
    }

    private fun getCamera(): Camera? {
        var mCamera: Camera? = null
        try {
            camera?.release()
            mCamera = android.hardware.Camera.open(0)
        } catch (e: Exception) {
            RobinApplication.log(TAG + "getCamera", e.message.toString())
        }
        return mCamera
    }

    private fun clearCameraData() {
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        RobinApplication.log(TAG, "clearCameraData")
        camera = null
    }
}
