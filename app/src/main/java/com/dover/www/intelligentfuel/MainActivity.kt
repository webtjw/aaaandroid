package com.dover.www.intelligentfuel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import com.blankj.utilcode.util.FileUtils
import com.wonderkiln.camerakit.CameraKitEventCallback
import com.wonderkiln.camerakit.CameraKitImage
import com.zyao89.view.zloading.ZLoadingDialog
import com.zyao89.view.zloading.Z_TYPE
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener, CameraKitEventCallback<CameraKitImage> {

    companion object {
        const val TAG = "MainActivity"

        private class MainHandler constructor(activity: MainActivity) : Handler() {

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

    private val mHandler = MainHandler(this@MainActivity)
    private var loading: ZLoadingDialog? = null
    private var videoList: ArrayList<String> = arrayListOf()
    private var videoIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindClickEvent()
        setLoopImageInMiddle()
    }

    override fun onPause() {
        super.onPause()
        if (cameraView.isStarted) cameraView.stop()
    }

    override fun onResume() {
        super.onResume()
        if (cameraViewBox.visibility == View.VISIBLE && !cameraView.isStarted) cameraView.start()
    }

    override fun onStart() {
        super.onStart()
        playVideos()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        playVideos()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.openCamera -> openCamera()
            R.id.closeCameraView -> closeCamera()
            R.id.takePicture -> {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                } else {
                    takePicture()
                }
            }
            R.id.openAlbum -> this@MainActivity.startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) takePicture()
                else RobinApplication.toast(this@MainActivity, "未获取到存储权限，无法拍照")
            }
        }
    }

    override fun callback(cameraKitImage: CameraKitImage?) {
        Thread {
            var saveFolder: String = this@MainActivity.filesDir.path + File.separator

            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                saveFolder = Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_DCIM + File.separator + "Camera" + File.separator
            }

            val fileFolder = File(saveFolder)
            if (!fileFolder.exists()) fileFolder.mkdirs()

            val fileName = Date().time.toString() + ".jpg"
            val filePath: String = saveFolder + fileName

            // save file
            try {
                val out = FileOutputStream(filePath)
                if (cameraKitImage!!.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                    out.flush()
                    out.close()
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val imageFile = File(filePath)
            MediaStore.Images.Media.insertImage(this@MainActivity.contentResolver, imageFile.absolutePath, fileName, null)
            // 图片加入相册
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(imageFile)
            mediaScanIntent.data = contentUri
            this@MainActivity.sendBroadcast(mediaScanIntent)
            this@MainActivity.loading!!.dismiss()
        }.start()
    }

    private fun takePicture() {
        loading = ZLoadingDialog(this@MainActivity)
        loading!!.setLoadingBuilder(Z_TYPE.CIRCLE)
                .setLoadingColor(Color.parseColor("#ED1f29"))
                .setHintText("照片处理中...")
                .setHintTextSize(22f)
                .show()
        cameraView.captureImage(this)
    }

    private fun playVideos() {
        // video display should obey percentage 16:9
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val screenWidth = dm.widthPixels
        val videoViewBoxHeight = screenWidth / 16 * 9 + 140 // 上下增加 70dp 的黑边
        mVideoViewBox.layoutParams = LinearLayout.LayoutParams(screenWidth, videoViewBoxHeight)

        getLocalVideos()

        // prepare the widget
        mVideoView.setOnPreparedListener { mVideoView.start() }

        if (videoList.size == 0) {
            mVideoView.setOnCompletionListener { mVideoView.start() }
            mVideoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.yijiezhuomaquan))
        }
        else {
            mVideoView.setOnCompletionListener {
                videoIndex++
                if (videoIndex >= videoList.size) videoIndex = 0
                mVideoView.setVideoPath(videoList[videoIndex])
            }
            mVideoView.setVideoPath(videoList[videoIndex])
        }

        mVideoView.setOnErrorListener { _, what, extra ->
            RobinApplication.log(TAG, "video error: $what / $extra")
            val restartIntent = Intent(this@MainActivity, MainActivity::class.java)
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this@MainActivity.startActivity(restartIntent)
            true
        }
    }

    private fun setBackgroundAlpha(bgAlpha: Float) {
        val lp = window.attributes
        lp.alpha = bgAlpha
        window.attributes = lp
    }

    private fun bindClickEvent() {
        openCamera.setOnClickListener(this@MainActivity)
        openAlbum.setOnClickListener(this@MainActivity)
        closeCameraView.setOnClickListener(this@MainActivity)
        takePicture.setOnClickListener(this@MainActivity)
    }

    private fun setLoopImageInMiddle() {
        val layoutManager = LinearLayoutManager(this@MainActivity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL // 水平
        loopImageRecyclerView.layoutManager = layoutManager
        // 这里是需要显示的照片，可随意增删
        val imageList = ArrayList<Int>()
        imageList.add(R.mipmap.ad_1)
        imageList.add(R.mipmap.ad_2)
        imageList.add(R.mipmap.ad_3)
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
                    mHandler.sendMessage(message)

                    Thread.sleep(duration / 4)
                    targetIndex = min + 1
                }

                val message = Message()
                message.what = 0x01
                message.arg1 = targetIndex
                mHandler.sendMessage(message)

                targetIndex += 1
            }
        }
        thread.start()
    }

    class LoopImageAdapter constructor(private val images: List<Int>) : RecyclerView.Adapter<LoopImageAdapter.Companion.ViewHolder>() {

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

    private fun openCamera() {
        cameraView.start()
        cameraViewBox.visibility = View.VISIBLE
        setBackgroundAlpha(0.6f)
    }

    private fun closeCamera() {
        cameraView.stop()
        cameraViewBox.visibility = View.GONE
        setBackgroundAlpha(1f)
    }

    private fun getLocalVideos() {
        mVideoView!!.pause()

        if (videoList.size != 0) videoList = arrayListOf()
        val localVideosFolder = File("${Environment.getExternalStorageDirectory().absolutePath}/Sinopec/videos")
        if (localVideosFolder.exists() && localVideosFolder.listFiles() != null) {
            for (file in localVideosFolder.listFiles()) {
                if (file.extension == "mp4") videoList.add(file.absolutePath)
            }
        }
    }
}
