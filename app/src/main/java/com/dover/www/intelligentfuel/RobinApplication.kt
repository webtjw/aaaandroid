package com.dover.www.intelligentfuel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.Utils

class RobinApplication : Application() {
    companion object {
        fun log(tag: String, message: String, level: Int? = 1) {
            val tagPrefix = "robin"
            val wholeTag = "$tagPrefix $tag"

            when (level) {
                2 -> Log.w(wholeTag, message)
                3 -> Log.e(wholeTag, message)
                else -> Log.i(wholeTag, message)
            }
        }

        fun toast(ctx: Context, message: String) {
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
    }
}