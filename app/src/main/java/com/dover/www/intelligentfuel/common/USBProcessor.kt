package com.dover.www.intelligentfuel.common

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Environment
import android.os.Looper
import android.view.Gravity
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ToastUtils
import com.dover.www.intelligentfuel.MainActivity
import com.dover.www.intelligentfuel.RobinApplication
import java.io.File

class USBProcessor constructor(private val context: Context, private val usbPath: String?) {

    companion object {
        const val TAG = "USBProcessor"
        val localStoragePath: String = Environment.getExternalStorageDirectory().absolutePath
    }

    fun copyUSBVideos() {
        if (usbPath != null) {
            Thread {
                Looper.prepare()
                val videoFolderPath = "$usbPath/Sinopec/videos"
                val fileVideoFolder = File(videoFolderPath)
                val usbVideos = arrayListOf<File>()

                if (fileVideoFolder.exists() && fileVideoFolder.isDirectory && fileVideoFolder.listFiles() != null) {
                    for (file in fileVideoFolder.listFiles()) {
                        if (file.extension == "mp4") usbVideos.add(file)
                    }
                    // 如果 U 盘中存在视频，则把设备本地的视频全部删掉，然后把 U 盘中的视频复制过去
                    if (usbVideos.size > 0) {
                        val localVideosPath = "$localStoragePath/Sinopec/videos"
                        // 判断目录是否存在，不存在则创建
                        FileUtils.createOrExistsDir(localVideosPath)
                        // 删除目录下所有东西
                        FileUtils.deleteAllInDir(localVideosPath)
                        // copy
                        ToastUtils.setBgColor(Color.WHITE)
                        ToastUtils.setGravity(Gravity.CENTER, 0, 0)
                        ToastUtils.setMsgColor(Color.parseColor("#ED1f29"))
                        ToastUtils.setMsgTextSize(32)
                        usbVideos.forEach { file ->
                            ToastUtils.showLong("正在复制视频 ${file.name}")
                            FileUtils.copyFile(file.absolutePath, "$localVideosPath/${file.name}")
                        }
                        val refreshIntent = Intent(context, MainActivity::class.java)
                        refreshIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(refreshIntent)
                    }
                }
                Looper.loop()
            }.start()
        }
    }
}