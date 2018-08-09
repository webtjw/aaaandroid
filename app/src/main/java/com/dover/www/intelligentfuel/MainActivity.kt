package com.dover.www.intelligentfuel

import android.content.Context
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.dover.www.intelligentfuel.common.VideoPlayer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, SurfaceHolder.Callback {

    companion object {
        const val TAG = "MainActivity"
    }

    private var videoPlayer: VideoPlayer? = null
    private var camera: Camera? = null
    private var surfaceHolder: SurfaceHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindClickEvent()
    }

    override fun onPause() {
        super.onPause()
        clearCameraData()
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.moreButton -> playCamera()
            R.id.closeCameraTrigger -> clearCameraData()
            R.id.takePictureTrigger -> RobinApplication.toast(this@MainActivity, "尚未开放")
        }
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

    private fun playCamera() {
        cameraViewBox.visibility = View.VISIBLE

        if (surfaceHolder == null) {
            surfaceHolder = cameraView.holder
            surfaceHolder?.addCallback(this@MainActivity)
        }

        if (camera == null) {
            camera = getCamera()
            try {
                camera?.setPreviewDisplay(surfaceHolder)
                camera?.startPreview()
            } catch (e: Exception) {
                RobinApplication.log(TAG + "playCamera", e.message.toString())
            }
        }
    }

    private fun clearCameraData() {
        cameraViewBox.visibility = View.GONE
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun bindClickEvent() {
        moreButton.setOnClickListener(this@MainActivity)
        closeCameraTrigger.setOnClickListener(this@MainActivity)
        takePictureTrigger.setOnClickListener(this@MainActivity)
    }
}
