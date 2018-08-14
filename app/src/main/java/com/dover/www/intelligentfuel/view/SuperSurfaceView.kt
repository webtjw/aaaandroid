package com.dover.www.intelligentfuel.view

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView

abstract class SuperSurfaceView(ctx: Context) : SurfaceView(ctx, null), SurfaceHolder.Callback {

    val surfaceHolder: SurfaceHolder = this.holder

    init {
        surfaceHolder.addCallback(this)
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        Thread(SuperThread(surfaceHolder)).start()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        surfaceHolder.removeCallback(this)
    }

    class SuperThread(holder: SurfaceHolder): Runnable {

        val mHolder = holder

        override fun run() {
            val canvas = mHolder.lockCanvas(null)
            val paint = Paint()
            paint.color = Color.BLUE
            canvas.drawRect(Rect(0, 0, 700, 700), paint)
            mHolder.unlockCanvasAndPost(canvas)
        }
    }

    abstract fun doDraw(canvas: Canvas)

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        doDraw(canvas)
        return bitmap
    }
}