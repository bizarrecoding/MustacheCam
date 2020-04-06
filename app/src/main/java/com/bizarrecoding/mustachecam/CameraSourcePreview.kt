package com.bizarrecoding.mustachecam

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.vision.CameraSource
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Herik on 14/12/2015.
 */

private const val TAG = "CameraSourcePreview"

class CameraSourcePreview(private val mContext: Context, attrs: AttributeSet?) : ViewGroup(mContext, attrs), SurfaceHolder.Callback {
    private val mSurfaceView: SurfaceView = SurfaceView(mContext)
    private var mStartRequested = false
    private var mSurfaceAvailable = false
    private var mCameraSource: CameraSource? = null
    private var mOverlay: GraphicOverlay? = null
    private val isPortraitMode: Boolean
        get() {
            val orientation = mContext.resources.configuration.orientation
            return orientation == Configuration.ORIENTATION_PORTRAIT
        }

    init {
        mSurfaceView.holder.addCallback(this)
        addView(mSurfaceView)
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
        }
        mCameraSource = cameraSource
        if (mCameraSource != null) {
            mStartRequested = true
            startIfReady()
        }
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?, overlay: GraphicOverlay?) {
        mOverlay = overlay
        start(cameraSource)
    }

    fun stop() {
        if (mCameraSource != null) {
            mCameraSource!!.stop()
        }
    }

    fun release() {
        if (mCameraSource != null) {
            mCameraSource!!.release()
            mCameraSource = null
        }
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource!!.start(mSurfaceView.holder)
            if (mOverlay != null) {
                val size = mCameraSource!!.previewSize
                val min = min(size.width, size.height)
                val max = max(size.width, size.height)
                if (isPortraitMode) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees
                    mOverlay!!.setCameraInfo(min, max, mCameraSource!!.cameraFacing)
                } else {
                    mOverlay!!.setCameraInfo(max, min, mCameraSource!!.cameraFacing)
                }
                mOverlay!!.clear()
            }
            mStartRequested = false
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var width = 320
        var height = 240
        if (mCameraSource != null) {
            val size = mCameraSource!!.previewSize
            if (size != null) {
                width = size.width
                height = size.height
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode) {
            val tmp = width
            width = height
            height = tmp
        }
        val layoutWidth = right - left
        val layoutHeight = bottom - top

        // Computes height and width for potentially doing fit width.
        var childWidth = layoutWidth
        var childHeight =
            (layoutWidth.toFloat() / width.toFloat() * height).toInt()

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight
            childWidth = (layoutHeight.toFloat() / height.toFloat() * width).toInt()
        }
        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
        }
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Could not start camera source.",
                e
            )
        }
    }

    override fun surfaceCreated(surface: SurfaceHolder) {
        mSurfaceAvailable = true
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    override fun surfaceDestroyed(surface: SurfaceHolder) {
        mSurfaceAvailable = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }
}