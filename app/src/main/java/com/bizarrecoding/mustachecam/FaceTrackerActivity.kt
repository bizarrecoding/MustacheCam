package com.bizarrecoding.mustachecam

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_face_tracker.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap

private const val TAG = "FaceTracker"
private const val REQUEST_CODE_PERMISSIONS = 10

class FaceTrackerActivity : AppCompatActivity(), CameraSource.PictureCallback {
    private var mCameraSource: CameraSource? = null
    private var mDetector: FaceDetector? = null
    private var isFront = false
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_tracker)
        isFront = true
        turn_camera.setOnCheckedChangeListener { buttonView, isChecked ->
            try {
                preview.release()
                preview.stop()
                createCameraSource(if(isChecked) CameraSource.CAMERA_FACING_BACK else CameraSource.CAMERA_FACING_FRONT)
                isFront = mCameraSource?.cameraFacing == CameraSource.CAMERA_FACING_FRONT
                Log.i("isFront", isFront.toString() + "")
                preview.start(mCameraSource)
            } catch (ex: IOException) {
                Log.e(TAG + "5x", ex.message!!)
            }
        }
        capture.setOnClickListener {
            mCameraSource?.takePicture(null, this)
            sendBroadcast(Intent(ACTION_MEDIA_SCANNER_SCAN_FILE, externalMediaDirs.first().toUri()))
        }
        if (allPermissionsGranted()) {
            createCameraSource(CameraSource.CAMERA_FACING_FRONT)
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun createCameraSource(facing: Int) {
        if (mDetector != null) {
            mDetector!!.release()
            mDetector = null
        }
        val context = applicationContext
        mDetector = FaceDetector.Builder(context).apply {
            setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
        }.build().also {
            it.setProcessor(
                MultiProcessor.Builder(
                    GraphicFaceTrackerFactory()
                ).build()
            )
        }
        if (mDetector==null || mDetector?.isOperational==false) {
            Log.w(TAG, "Face detector dependencies are not yet available.")
        }
        mCameraSource = CameraSource.Builder(context, mDetector)
            .setRequestedPreviewSize(640, 480)
            .setFacing(facing)
            .setRequestedFps(30.0f)
            .build()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCameraSource()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraSource?.release()
    }

    private fun startCameraSource() {
        if (mCameraSource != null) {
            try {
                preview?.start(mCameraSource, faceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource?.release()
                mCameraSource = null
            }
        }
    }

    override fun onPictureTaken(data: ByteArray) {
        //SAVED IMAGE ROTATION
        val opt = BitmapFactory.Options()
        opt.inSampleSize = 2
        val bm = BitmapFactory.decodeByteArray(data, 0, data.size, opt)
        val matrix = Matrix()
        val info = Camera.CameraInfo()
        Log.i("Orientation info", "" + info.orientation)
        if (bm.width > bm.height) {
            matrix.setRotate(
                270f,
                bm.width.toFloat() / 2,
                bm.height.toFloat() / 2
            )
            Log.i("Rotation", "Camera output rotated 270 degrees.")
        }
        val rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        val start = System.currentTimeMillis()
        Log.i("FUSE", "start")
        fuseBitmap(rotatedBitmap)
        Log.i("FUSE", "end, duration = " + (System.currentTimeMillis() - start))
    }

    private fun fuseBitmap(photo: Bitmap) {
        val w = photo.width
        val h = photo.height
        var bmData = faceOverlay.drawToBitmap(Bitmap.Config.ARGB_8888)
        Log.i("Photo", w.toString() + "x" + h + "\t data:  " + bmData.width + "x" + bmData.height)
        bmData = Bitmap.createScaledBitmap(bmData, w, h, false)
        val overlaybitmap = Bitmap.createBitmap(w, h, photo.config)

        val mat = Matrix()
        if (!isFront) {
            mat.setRotate(180f)
            Log.i("isFront", isFront.toString() + "")
        } else {
            mat.preScale(-1f, 1f)
        }
        val mirroredPhoto = mirrorBitmap(photo, mat)
        val canvas = Canvas(overlaybitmap)
        canvas.drawBitmap(mirroredPhoto, 0f, 0f, null)
        canvas.drawBitmap(bmData, 0f, 0f, null)
        saveCurrentBitmap(overlaybitmap)
        takenFrame.setImageBitmap(overlaybitmap)
        Snackbar.make(root,  "Photo Saved at Pictures/Mustache", Snackbar.LENGTH_LONG ).show()
    }

    private inner class GraphicFaceTrackerFactory: MultiProcessor.Factory<Face> {
        override fun create(face: Face): GraphicFaceTracker {
            return GraphicFaceTracker(faceOverlay)
        }
    }

    private inner class GraphicFaceTracker(private val mOverlay: GraphicOverlay?) : Tracker<Face?>() {

        private val mFaceGraphic: FaceGraphic = FaceGraphic(mOverlay, applicationContext)

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face?) {
            //mFaceGraphic.setId(faceId)
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        override fun onUpdate(detectionResults: Detections<Face?>, face: Face?) {
            mOverlay?.add(mFaceGraphic)
            mFaceGraphic.updateFace(face)
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detections<Face?>) {
            mOverlay?.remove(mFaceGraphic)
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            mOverlay!!.remove(mFaceGraphic)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                createCameraSource(CameraSource.CAMERA_FACING_FRONT)
                startCameraSource()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(applicationContext, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
    }


    private fun saveCurrentBitmap(data: Bitmap?) {
        val pictureFile = File(externalMediaDirs.first(),"Mustache_${System.currentTimeMillis()}.png")
        ByteArrayOutputStream().let { baos->
            data?.compress(Bitmap.CompressFormat.PNG, 100, baos)
            try {
                FileOutputStream(pictureFile).run {
                    write(baos.toByteArray())
                    close()
                }
            }catch (ex: IOException){
                Log.e("Mustache", "Error saving bitmap cache${ex.message}" )
            }
            baos.close()
        }
    }

    private fun mirrorBitmap(bm: Bitmap, mat: Matrix?): Bitmap {
        return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, mat, false)
    }
}