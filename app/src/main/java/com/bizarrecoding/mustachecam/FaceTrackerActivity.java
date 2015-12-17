package com.bizarrecoding.mustachecam;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FaceTrackerActivity extends AppCompatActivity {

    private static final String TAG = "FaceTracker";
    private static View parentLayout;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private static GraphicOverlay mGraphicOverlay;
    private FaceDetector mDetector;
    private static boolean isFront;

    private static ImageView mImageView;
    private Button takePhoto=null;
    private static FrameLayout thumbnail = null;
    private ToggleButton switchCam = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_tracker);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        isFront=true;
        parentLayout = findViewById(R.id.root);
        thumbnail = (FrameLayout) findViewById(R.id.takenFrame);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mImageView = new ImageView(getApplicationContext());
        createCameraSource(CameraSource.CAMERA_FACING_FRONT);

        switchCam = (ToggleButton) findViewById(R.id.toggleButton);
        switchCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    mPreview.release();
                    mPreview.stop();
                    if (isChecked) {
                        createCameraSource(CameraSource.CAMERA_FACING_BACK);
                    } else {
                        createCameraSource(CameraSource.CAMERA_FACING_FRONT);
                    }
                    Boolean change = mCameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT;
                    setFront(change);
                    Log.i("isFront", isFront + "");
                    mPreview.start(mCameraSource);
                } catch (Exception ex) {
                    Log.e(TAG + "5x", ex.getLocalizedMessage());
                }
            }
        });
        takePhoto = (Button) findViewById(R.id.button);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCameraSource.takePicture(null, new PhotoHandler(getApplicationContext(), mCameraSource));
                try {
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+File.separator+"Mustache")));
                }catch (Exception ex){
                    Log.e("Intent Failed",ex.getMessage());
                }
            }
        });
    }
    public static void setFront(Boolean front) {
        isFront = front;
    }

    public static Bitmap mirrorBitmap(Bitmap bm, Matrix mat){
        bm=Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight(),mat,false);
        return bm;
    }

    public static void fuseBitmap(Bitmap photo){
        int w = photo.getWidth();
        int h = photo.getHeight();

        Bitmap bmData = mGraphicOverlay.getDrawingCache();
        Log.i("Photo",w+"x"+h+"\t data:  "+bmData.getWidth()+"x"+bmData.getHeight());
        bmData = Bitmap.createScaledBitmap(bmData, w, h, false);
        Bitmap overlaybitmap = Bitmap.createBitmap(w,h, photo.getConfig());
        Canvas canvas = new Canvas(overlaybitmap);
        Matrix mat = new Matrix();
        if(!isFront) {
            mat.setRotate(180);
            Log.i("isFront", isFront + "");
        }else{
            mat.preScale(-1, 1);
        }
        photo = mirrorBitmap(photo,mat);

        canvas.drawBitmap(photo, 0, 0, null);
        canvas.drawBitmap(bmData, 0, 0, null);
        saveCurrentBitmap(overlaybitmap);
        mImageView.setImageBitmap(overlaybitmap);
        thumbnail.removeAllViews();
        thumbnail.addView(mImageView);
        mGraphicOverlay.destroyDrawingCache();

        Snackbar snackbar = Snackbar
                .make(parentLayout, "Photo Saved at Pictures/Mustache", Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private static void saveCurrentBitmap(Bitmap data) {
        File pictureFileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Mustache");
        if (!pictureFileDir.exists()){
            pictureFileDir.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoName = "Mustache_" + date + ".png";

        String filename = pictureFileDir.getPath() + File.separator + photoName;
        File pictureFile = new File(filename);
        Log.i("FileDir", "" + filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        data.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bitmapdata = baos.toByteArray();
        Log.i("FileData",""+(data!=null));
        Log.i("File", "" + (bitmapdata != null));
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(bitmapdata);
            Log.i("FileDir", filename + " Written");
            fos.close();
        }catch (Exception ex){
            Log.e("Mustache","Error saving bitmap cache\n"+ex.getMessage()+"\n");
        }finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void createCameraSource(int facing) {
        if(mDetector!=null){
            mDetector.release();
            mDetector=null;
        }
        Context context = getApplicationContext();
        mDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        mDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!mDetector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, mDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(facing)
                .setRequestedFps(30.0f)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    private void startCameraSource() {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, getApplicationContext());
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }
}
