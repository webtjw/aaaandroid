package com.dover.www.intelligentfuel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Camera
import android.media.Image
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener, SurfaceHolder.Callback {

    companion object {
        const val TAG = "MainActivity"

        private class mHandler constructor(activity: MainActivity) : Handler() {

            val mActivity = WeakReference<MainActivity>(activity)

            override fun handleMessage(msg: Message?) {
                val activity = mActivity.get()
                if (activity != null) {
                    when (msg?.what) {
                        0x01 -> {
                            val position = msg.arg1
                            if (position == 0) activity.loopImageRecyclerView.scrollToPosition(0)
                            else activity.loopImageRecyclerView.smoothScrollToPosition(position)
                        }
                    }
                }
            }
        }
    }

    // private var videoPlayer: VideoPlayer? = null
    private var camera: Camera? = null
    private var surfaceHolder: SurfaceHolder? = null
    private val mainHandler = mHandler(this@MainActivity)

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

        setLoopImageInMiddle()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
            R.id.takePictureTrigger -> {
                val bitmap = Bitmap.createBitmap(700, 700, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                cameraView.draw(canvas)
                canvas.save()
                val outputImage = File(Environment.getExternalStorageDirectory().absolutePath + "/Tokheim/" + System.currentTimeMillis() + ".jpg")
                val outputImageStream = FileOutputStream(outputImage)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputImageStream)
                outputImageStream.close()
                RobinApplication.toast(this@MainActivity, "功能维护中")
            }
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
        // prepare the widget
        mVideoView.setOnPreparedListener { mVideoView.start() }
        mVideoView.setOnCompletionListener { mVideoView.start() }
        mVideoView.setOnErrorListener { mediaPlayer, what, extra ->
            RobinApplication.log(TAG, "video error: ${what} / ${extra}")
            val restartIntent = Intent(this@MainActivity, MainActivity::class.java)
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this@MainActivity.startActivity(restartIntent)
            true
        }
        mVideoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.yijiezhuomaquan))
        // get all video files
        // videoPlayer = VideoPlayer(this@MainActivity, mVideoView, Environment.getExternalStorageDirectory().absolutePath + "/Tokheim/AdVideo", mVideoIndexBox)
        // videoPlayer?.prepare()
        // videoPlayer?.start()
    }

    private fun getCamera(): Camera? {
        var mCamera: Camera? = null
        try {
            camera?.release()
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), 1)
            }
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

    private fun setLoopImageInMiddle() {
        val layoutManager = LinearLayoutManager(this@MainActivity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL // 水平
        loopImageRecyclerView.layoutManager = layoutManager
        // 这里是需要显示的照片，可随意增删
        val imageList = ArrayList<Int>()
        imageList.add(R.mipmap.ad_4)
        imageList.add(R.mipmap.ad_5)
        imageList.add(R.mipmap.ad_6)
        imageList.add(R.mipmap.ad_7)
        imageList.add(R.mipmap.ad_8)
        imageList.add(R.mipmap.ad_9)
        imageList.add(R.mipmap.ad_10)
        // 一次显示 3 张
        val visibleNum = 3
        // 为了轮播的衔接效果，需要在列表尾部添加开头那几项
        for (i in 0 until visibleNum) {
            imageList.add(imageList[i])
        }
        // 适配器
        val imageAdapter = LoopImageAdapter(imageList)
        loopImageRecyclerView.adapter = imageAdapter
        // 设置了这个才可以以项为单位滚动
        LinearSnapHelper().attachToRecyclerView(loopImageRecyclerView)
        // 开始轮播
         animateLoopImage(visibleNum, visibleNum - 1, imageList.size - 1)
    }

    private fun animateLoopImage(currentIndex: Int, min: Int, max: Int) {

        var targetIndex = currentIndex
        val duration = 4000.toLong() // 实际效果是 duration 的 2 倍，修改要注意

        val thread = Thread {
            while (true) {

                Thread.sleep(duration)

                if (targetIndex > max) {
                    // 恢复真正的列表开头
                    val message = Message()
                    message.what = 0x01
                    message.arg1 = 0
                    mainHandler.sendMessage(message)

                    Thread.sleep(duration / 4)
                    targetIndex = min + 1
                }

                val message = Message()
                message.what = 0x01
                message.arg1 = targetIndex
                mainHandler.sendMessage(message)

                targetIndex += 1
            }
        }
        thread.start()
    }

    class LoopImageAdapter constructor(val images: List<Int>) : RecyclerView.Adapter<LoopImageAdapter.Companion.ViewHolder>() {

        companion object {
            class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
                val image: ImageView = view.findViewById(R.id.loopImage)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val customView = LayoutInflater.from(parent.context).inflate(R.layout.loop_image_item, parent, false)
            return ViewHolder(customView)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val imageItem = images[position]
            viewHolder.image.setImageResource(imageItem)
            val params = viewHolder.image.layoutParams
            params.width = 313
            params.height = 250
            viewHolder.image.layoutParams = params
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }
}
