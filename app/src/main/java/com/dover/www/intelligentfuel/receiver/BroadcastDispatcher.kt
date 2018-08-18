package com.dover.www.intelligentfuel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.dover.www.intelligentfuel.MainActivity
import com.dover.www.intelligentfuel.RobinApplication
import com.dover.www.intelligentfuel.common.USBProcessor
import java.io.File

class BroadcastDispatcher : BroadcastReceiver() {
    companion object {
        const val TAG = "BroadcastDispatcher"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val mainIntent = Intent(context, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context?.startActivity(mainIntent)
            }
            Intent.ACTION_MEDIA_MOUNTED -> {
                val manager = context?.getSystemService(Context.USB_SERVICE) as UsbManager
                val usbDeviceList = manager.deviceList
                val deviceIterator = usbDeviceList.values.iterator()
                while (deviceIterator.hasNext()) {
                    val usbDevice = deviceIterator.next()
                    val usbDeviceType = usbDevice.getInterface(0).interfaceClass
                    // 8 为 u 盘，证明当前有 U 盘接入了，但不能确认本 intent 是否由该 U 盘发起，过渡性的代码
                    if (usbDeviceType == 8 && intent.data != null) {
                        val usbProcessor = USBProcessor(context, intent.data?.path)
                        usbProcessor.copyUSBVideos()
                    }
                }
            }
            else -> RobinApplication.log(TAG, "unresolved broadcast: ${intent?.action}")
        }
    }
}