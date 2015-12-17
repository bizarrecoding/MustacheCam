package com.bizarrecoding.mustachecam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

/**
 * Created by Herik on 14/12/2015.
 */
public class PhotoHandler implements CameraSource.PictureCallback {

    private final Context context;
    private CameraSource mCameraSource;
    private final int rotation = 270;

    public PhotoHandler(Context ctx,CameraSource mCamera){
        this.context = ctx;
        mCameraSource = mCamera;
    }

    @Override
    public void onPictureTaken(byte[] data) {
        //SAVED IMAGE ROTATION
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,opt);

        Matrix matrix = new Matrix();
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();

        Log.i("Orientation info", "" + info.orientation);
        if (bm.getWidth() > bm.getHeight()) {
            matrix.setRotate(rotation, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
            Log.i("Rotation","Camera output rotated 270 degrees.");
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

        Long start = System.currentTimeMillis();
        Log.i("FUSE","start");
        FaceTrackerActivity.fuseBitmap(rotatedBitmap);
        Log.i("FUSE", "end, duration = " + (System.currentTimeMillis() - start));
    }
}
