package com.tau.application.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.tau.application.DoctorMain;
import com.tau.application.PatientMain;
import com.tau.application.R;


/**
 * Created by dan on 24/12/2016.
 */
public class BarcodeScanner extends Activity {

    static Activity mContext;
    public static String mQRText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        mContext = this;

        Intent intent = getIntent();
        final String requester = intent.getStringExtra("requester");

        final TextView name_header=(TextView) findViewById(R.id.barcode_header);
        if(requester.contains("Doctor")){
            name_header.setText(getResources().getString(R.string.barcode_header_doctor));
        }else{
            name_header.setText(getResources().getString(R.string.barcode_header_patient));
        }

        final SurfaceView cameraView = (SurfaceView)findViewById(R.id.camera_view);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();


        final CameraSource cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(cameraView.getHolder());
                    }
                } catch (Exception e) {
                    Utils.log("Can't instantiate camera, "+  e.getMessage());
                    Utils.showBlankDialog(mContext, BarcodeScanner.this, "Error loading camera", e.getMessage(), "OK");
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                try {
//                    cameraSource.start(cameraView.getHolder());
//                } catch (Exception e) {
//                    SmarteyeglassUtils.log("Can't Instantiate camera, "+e.getMessage());
//                }
                try{
                    cameraSource.release();
                }catch (NullPointerException e){
                    //
                }
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Utils.log("Releasing Camera");
                runOnUiThread(new Runnable() {
                    public void run() {
                        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(BarcodeScanner.this).create();
                        alertDialog.setTitle("Retrieved Code:");
                        alertDialog.setMessage(mQRText);
                        alertDialog.setButton("Return", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final Intent intent;
                                if(requester.contains("Doctor")){
                                    intent = new Intent(getApplicationContext(), DoctorMain.class);
                                }else{
                                    intent = new Intent(getApplicationContext(), PatientMain.class);
                                    intent.putExtra("barcode", mQRText);
                                }
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                alertDialog.dismiss();
                                finish();
                            }
                        });
                        alertDialog.show();}
                });
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                //Received QR
                if (barcodes.size() != 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mQRText = barcodes.valueAt(0).displayValue;
                            Utils.log(mQRText);
                            try{
                                cameraSource.release();
                            }catch (NullPointerException e){
                                //
                            }
                        }
                    });
                }
            }
        });
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        finish();
    }

}
