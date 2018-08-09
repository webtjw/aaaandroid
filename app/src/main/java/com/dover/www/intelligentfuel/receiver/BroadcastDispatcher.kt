package com.dover.www.intelligentfuel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dover.www.intelligentfuel.MainActivity
import com.dover.www.intelligentfuel.RobinApplication

class BroadcastDispatcher : BroadcastReceiver() {
    companion object {
        const val TAG = "BroadcastDispatcher"
    }

    override fun onReceive(ctx: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val mainIntent = Intent(ctx, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx?.startActivity(mainIntent)
            }
            else -> RobinApplication.log(TAG, "unresolved broadcast: ${intent?.action}")
        }
    }
}