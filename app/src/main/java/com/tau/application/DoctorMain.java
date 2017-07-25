package com.tau.application;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.tau.application.Query.QueryAdapter;
import com.tau.application.Query.QueryAlgorithm;
import com.tau.application.Smarteyeglass.SmarteyeglassUtils;
import com.tau.application.Utils.BarcodeScanner;
import com.tau.application.Utils.Constants;
import com.tau.application.Utils.Debug;
import com.tau.application.Utils.ManagePatients;
import com.tau.application.Utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;

import static com.tau.application.Utils.BarcodeScanner.mQRText;
import static com.tau.application.Utils.Utils.log;


public class DoctorMain extends AppCompatActivity {

    private static Activity mContext;

    boolean collectedGenes;
    static Spinner spinner;
    public String doctor_id;
    public String doctor_name;
    public int access_level;
    static SharedPreferences sharedPreferences;
    private static DoctorMain instance = new DoctorMain();
    public static DoctorMain getInstance(){
        return instance;
    }
    static String patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_main);
        sharedPreferences = this.getSharedPreferences(Constants.SHARED_PREF, 0);

        Intent intent = getIntent();

        String id = intent.getStringExtra("id");
        String doctorName = intent.getStringExtra("name");
        String last_patient_id = intent.getStringExtra("patient");
        access_level = intent.getIntExtra("access_level", 100); //emptystate value

        if(id==null || doctorName==null || last_patient_id==null || access_level==100){
            doctorName =sharedPreferences.getString("name", null);
            id = sharedPreferences.getString("id", null);
            last_patient_id = sharedPreferences.getString("last_patient_id", null);
            access_level = sharedPreferences.getInt("access_level", 3);
        }else{
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("name", doctorName);
            editor.putString("id", id);
            editor.putString("last_patient_id", last_patient_id);
            editor.putInt("access_level", access_level);
            editor.commit();
        }

        doctor_id = id;
        doctor_name = doctorName;
        patientId = last_patient_id;

        mContext = this;
        collectedGenes = false;
        Utils.stopLoading();

        TextView tv= (TextView) findViewById(R.id.textView21);
        tv.setText("Manage patient: "+patientId);
        //spinner code
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.queries, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(mQRText!=null){
//            executeLambda();
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        if(collectedGenes){
            setContentView(R.layout.activity_doctor_main);
            collectedGenes = false;
        }else{
            startActivity(new Intent(DoctorMain.this, MainActivity.class));
        }
    }

    public void verifyQR(View v) {

        if (Debug.genes_debug_mode) {
            String result = "[APC, ASPM, BDNF, CFTR, CREBBP,CRH ,CXCR4, ART4, DHFR, HFE, KR14, RHO, SDHD]";
            createGenesList(result);
            return;
        }
        if (Debug.drugs_debug_mode) {
            decodeAndProcess("7290000857329", Long.parseLong(patientId));
            return;
        }

        if(access_level==0 && mQRText==null){
            runGeneQuery(true);
        }else if(mQRText==null){
            Utils.showBlankDialog(mContext, DoctorMain.this, "No QR Code scanned", "Please scan it before retrieving values", "OK");
            Utils.stopLoading();
            return;
        }else{
            decodeAndProcess(mQRText, Long.parseLong(patientId));
        }
    }

    /**
     *
     * @param code QR / Drug code
     * @param id Patient's id
     */
    public void decodeAndProcess(final String code, final long id){
        try {
            //if it's a QR we'll get an exception here
            long ignore = Long.parseLong(code);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        /**
                         * sample - Agispor 7290000857329
                         * https://try.jsoup.org/
                         */
                        Document doc = Jsoup.connect("http://pharmanet.co.il/Product.aspx?Barcode=" + code).get();
                        Elements headline = doc.select("title");
                        Elements divs = doc.select("div:not([id],[class],[style])");
                        org.jsoup.nodes.Element head = headline.first();
                        String header = head.text().toUpperCase();
                        String activeSubstance = null;
                        for (Element element : divs) {
                            if(!element.toString().contains("input type") && !element.toString().contains("href") && element.toString().contains("%")){
                                activeSubstance = element.text().toUpperCase();
                            }
                        }
                        //TODO show alertdialog with drug name and active substance
                        Doctor doctor;
                        if(activeSubstance == null){
                            doctor = new Doctor(header, id);
                        }else{
                            doctor = new Doctor(activeSubstance, id);
                        }
                        runDrugQuery(doctor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };thread.start();
        }
        //GENES
        catch (NumberFormatException e) {
            runGeneQuery(false);
        }
    }

    public void runDrugQuery(final Doctor doctor) {
        Utils.showLoading(DoctorMain.this);
        new AsyncTask<Doctor, Void, String>() {
            @Override
            protected String doInBackground(Doctor... params) {
                try {
                    return MainActivity.lambdaInterface.find_vip_genes(params[0]);
                } catch (LambdaFunctionException e) {
                    log("Failed to invoke lambda :" + e);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Error retrieving data", "Invalid ID or bad Code format",
                            "OK");
                } catch
                        (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Error retrieving data", e.toString(),
                            "OK");
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                Utils.stopLoading();
                if (result == null) {
                    return;
                }
                if (result.contains("false")) {
                    log("Data is :" + result);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "No genes associated with this drug in patient's VCF", "Out of danger",
                            "OK");
                } else {
                    log("Data is :" + result);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "WARNING! \nVery Important Pharmacogene found", result,
                            "OK");
                }
            }
        }.execute(doctor);
    }

    public void runGeneQuery(Boolean isQrNull){
        Utils.showLoading(this);
        String localQR;
        String spinnerChoice = spinner.getSelectedItem().toString();
        if(spinnerChoice.contains("Clear")){
            mQRText = null;
            isQrNull = true;
        }
        if(isQrNull){
            localQR = "id=" + patientId + "&query=" + spinnerChoice + "&doctor=" + doctor_id;
        }else{
            localQR = mQRText + "query=" + spinnerChoice  + "&doctor=" + doctor_id;
        }
        Patient patient = new Patient(localQR);
        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {
                try {
                    return MainActivity.lambdaInterface.verify_qr(params[0]);
                } catch (LambdaFunctionException e) {
                    //We'll fail if the QR Code has returned anything other than a string
                    log("Failed to invoke lambda :" + e);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Error retrieving data", "Invalid ID or bad QR Code format",
                            "OK");
                    mQRText = null;
                } catch (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Error retrieving data", e.toString(),
                            "OK");
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                Utils.stopLoading();
                if (result == null) {
                    return;
                }
                if (result.contains("No")) {
                    log("Data is :" + result);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Error retrieving data", result,
                            "OK");
                } else {
                    log("Data is :" + result);
                    Utils.showBlankDialog(mContext, DoctorMain.this, "Successfully retrieved data", result,
                            "OK");
                    createGenesList(result);
                }
            }
        }.execute(patient);
    }


    public void scanqR(final View v){
        Intent intent = new Intent(getApplicationContext(), BarcodeScanner.class);
        intent.putExtra("requester", "Doctor");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void managePatients(final View v){
        Intent intent = new Intent(getApplicationContext(), ManagePatients.class);
        startActivity(intent);
    }


    public void createGenesList(String result){
        setContentView(R.layout.query_results);
        collectedGenes = true;
        String res = result;
        res = res.replace("[", "");
        res = res.replace("]", "");
        res = res.replace("\"", "");
        String[] genes = res.split(",");

        /**
         * Define the limit here
         */
        ListAdapter queryAdapter = new QueryAdapter(mContext, genes, 50);

        ListView tauListView = (ListView) findViewById(R.id.tauListView);
        tauListView.setAdapter(queryAdapter);
    }

    public void runAlgorithm(Context ctx, String gene, int limit){
        Intent intent = new Intent(ctx, QueryAlgorithm.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("gene", gene);
        intent.putExtra("limit", limit);
        ctx.startActivity(intent);
    }

//    Change widgets on UI test
//    public void changeImageAfterSeconds(){
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                ImageView img= (ImageView) findViewById(R.id.imageView9);
//                img.setImageResource(R.mipmap.vv);
//            }
//        }, 5000);
//
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                ImageView img= (ImageView) findViewById(R.id.imageView13);
//                img.setImageResource(R.mipmap.vv);
//            }
//        }, 7500);
//
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                ImageView img= (ImageView) findViewById(R.id.imageView14);
//                img.setImageResource(R.mipmap.vv);
//                Button bt = (Button) findViewById(R.id.masterButton);
//                bt.setVisibility(View.VISIBLE);
//                TextView txv = (TextView) findViewById(R.id.masterOrTextView);
//                txv.setVisibility(View.VISIBLE);
//            }
//        }, 10000);
//    }

}
