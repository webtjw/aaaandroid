package com.dover.www.intelligentfuel.common

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.VideoView
import com.dover.www.intelligentfuel.R
import com.dover.www.intelligentfuel.RobinApplication
import java.io.File

class VideoPlayer constructor(ctx: Context, mVideoView: VideoView, videoFolder: String, mVideoIndexBox: LinearLayout) {
    private val context = ctx
    private val videoView = mVideoView
    private val folder = File(videoFolder)
    private val videoIndexBox = mVideoIndexBox

    val videoList = ArrayList<String>()
    var playIndex = 0

    fun prepare() {
        if (folder.isDirectory) {
            val folderFiles = folder.listFiles()
            if (folderFiles != null) {
                for (fileItem in folderFiles) {
                    if (fileItem.extension == "mp4") {
                        videoList.add(fileItem.absolutePath)
                    }
                }
            }
        }
    }

    fun start() {
        // tip for no videos
        if (videoList.size == 0) RobinApplication.toast(context, "未发现可播放的 mp4 视频文件")
        else {
            videoView.setOnCompletionListener {
                if (playIndex >= videoList.size - 1) playIndex = 0
                else playIndex += 1
                videoView.setVideoPath(videoList.get(playIndex))
            }
            videoView.setOnPreparedListener {
                videoView.start()
                makeIndexIndicator()
            }
            videoView.setVideoPath(videoList.get(playIndex))
        }
    }

    private fun makeIndexIndicator() {
        videoIndexBox.removeAllViews()
        var forIndex = 0
        for (videoItem in videoList) {
            val linearLayout = LinearLayout(context)
            videoIndexBox.addView(linearLayout)
            val linearLayoutParams = linearLayout.layoutParams
            linearLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            linearLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            linearLayout.layoutParams = linearLayoutParams
            linearLayout.setPadding(4, 0, 4, 0)
            val image = ImageView(context)
            image.setImageResource(R.drawable.video_index_dot)
            linearLayout.addView(image)
            if (forIndex == playIndex) {
                val imageParams = image.layoutParams
                imageParams.width = 13
                imageParams.height = 13
                image.layoutParams = imageParams
            }
            forIndex++
        }
    }
}