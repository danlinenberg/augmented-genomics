package com.tau.application.Utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.tau.application.Amazon.LambdaInterface;
import com.tau.application.MainActivity;
import com.tau.application.Patient;
import com.tau.application.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.R.id.text1;
import static com.tau.application.MainActivity.userName;
import static java.lang.Character.FORMAT;

public class BarcodeGenerator extends Activity {
    ImageView qrCodeImageview;
    String QRcode;
    public final static int WIDTH=500;
    static List<String> preQRdata = new ArrayList<>();
    static Activity mContext;

    //TODO check permissions for camera and phone

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_generator);
        mContext = this;

        Utils.showLoading(this);
        getID();

        Long tsLong = System.currentTimeMillis()/1000;
        final String ts = tsLong.toString();

        final TextView mTextField = (TextView)findViewById(R.id.coundown);

        /**
         * Passing ID and timestamp to add to Patient's table
         */
        preQRdata.add("id="+userName.toString());
        preQRdata.add("timestamp="+ts);
        String QRData = Utils.listToString(preQRdata);
        Patient patient = new Patient(QRData);
        final LambdaInterface myInterface = MainActivity.factory.build(LambdaInterface.class);
        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {
                try {
                    return myInterface.device_whitelist(params[0]);
                } catch (Exception e) {
                    Utils.log("Failed to invoke lambda :" + e);
                    Utils.stopLoading();
                    Utils.showBlankDialog(mContext, BarcodeGenerator.this, "Oops...", e.toString(),
                            "OK");
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String result) {
                //
            }
        }.execute(patient);


        Thread t = new Thread(new Runnable() {
            public void run() {
                List<String> lst = MainActivity.device_params;
                lst.add("id="+MainActivity.userName);
                lst.add("timestamp="+ts);
                QRcode= Utils.listToString(lst);
                try {
                    synchronized (this) {
                        wait(5000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Bitmap bitmap = null;
                                    bitmap = encodeAsBitmap(QRcode);
                                    qrCodeImageview.setImageBitmap(bitmap);
                                    new CountDownTimer(300000, 1000) {

                                        public void onTick(long millisUntilFinished) {
                                            mTextField.setText("Time Remaining: " + millisUntilFinished / 1000);
                                            //here you can have your logic to set text to edittext
                                        }

                                        public void onFinish() {
                                            mTextField.setText("Session expired. Please recreate QR");
                                        }

                                    }.start();
                                } catch (WriterException e) {
                                    e.printStackTrace();
                                } // end of catch block
                            } // end of run method
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void getID() {
        qrCodeImageview=(ImageView) findViewById(R.id.img_qr_code_image);
    }
    // this is method call from on create and return bitmap image of QRCode.
    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 500, 0, 0, w, h);
        Utils.stopLoading();
        return bitmap;

    } /// end of this method

//Closes barcode activity
    public void close(View v) {

        finish();
    }
}
