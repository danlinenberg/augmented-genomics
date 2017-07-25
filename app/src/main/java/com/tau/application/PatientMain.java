package com.tau.application;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.tau.application.Amazon.LambdaInterface;
import com.tau.application.Utils.BarcodeGenerator;
import com.tau.application.Utils.BarcodeScanner;
import com.tau.application.Utils.Constants;
import com.tau.application.Utils.FileDialog;
import com.tau.application.Utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.tau.application.MainActivity.lambdaInterface;
import static com.tau.application.MainActivity.userName;
import static com.tau.application.Utils.Utils.log;

public class PatientMain extends AppCompatActivity {
    static View view;
    private boolean isInSettings;
    private Context ctx;
    static Activity mContext;
    public String patient_id;
    public String patient_name;
    static SharedPreferences sharedPreferences;
    private static PatientMain instance = new PatientMain();
    public static PatientMain getInstance(){
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_main);
        sharedPreferences = this.getSharedPreferences(Constants.SHARED_PREF, 0);

        Intent intent = getIntent();


        String id = intent.getStringExtra("id");
        String name = intent.getStringExtra("name");
        if(id==null || name==null){
            name =sharedPreferences.getString("patient_name", null);
            id = sharedPreferences.getString("patient_id", null);
        }else{
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("patient_name", name);
            editor.putString("patient_id", id);
            editor.commit();
        }

        patient_id = id;
        patient_name = name;

        if(intent.getStringExtra("barcode")!=null){
            Utils.showLoading(PatientMain.this);
            runDrugQuery(intent.getStringExtra("barcode"));
        }

        isInSettings = false;
        ctx = getApplicationContext();
        Utils.stopLoading();
        view = this.findViewById(android.R.id.content).getRootView();

        final TextView name_header=(TextView) findViewById(R.id.patient_name_header);
        name_header.setText("Hello " + patient_name);

        mContext = this;
        //Automatically whitelist the patient's device
        whiteList();
    }

    public void generateBarcode(View V){
        Intent intent = new Intent(this,BarcodeGenerator.class);
        startActivity(intent);
    }

    public void goToSettinngs(View v){
        setContentView(R.layout.activity_patient_settings);
        isInSettings = true;
        final SeekBar sk11=(SeekBar) findViewById(R.id.seekBar11);
        final SeekBar sk12=(SeekBar) findViewById(R.id.seekBar12);
        final SeekBar sk13=(SeekBar) findViewById(R.id.seekBar13);
        final SeekBar sk14=(SeekBar) findViewById(R.id.seekBar14);
        final SeekBar sk15=(SeekBar) findViewById(R.id.seekBar15);
        final SeekBar sk21=(SeekBar) findViewById(R.id.seekBar21);
        final SeekBar sk22=(SeekBar) findViewById(R.id.seekBar22);
        final SeekBar sk23=(SeekBar) findViewById(R.id.seekBar23);
        final SeekBar sk24=(SeekBar) findViewById(R.id.seekBar24);
        final SeekBar sk25=(SeekBar) findViewById(R.id.seekBar25);
        sk11.setProgress(sharedPreferences.getInt("seekBar11", 1));
        sk12.setProgress(sharedPreferences.getInt("seekBar12", 1));
        sk13.setProgress(sharedPreferences.getInt("seekBar13", 1));
        sk14.setProgress(sharedPreferences.getInt("seekBar14", 1));
        sk15.setProgress(sharedPreferences.getInt("seekBar15", 1));
        sk21.setProgress(sharedPreferences.getInt("seekBar21", 1));
        sk22.setProgress(sharedPreferences.getInt("seekBar22", 1));
        sk23.setProgress(sharedPreferences.getInt("seekBar23", 1));
        sk24.setProgress(sharedPreferences.getInt("seekBar24", 1));
        sk25.setProgress(sharedPreferences.getInt("seekBar25", 1));

        SeekBar.OnSeekBarChangeListener sklistneer =  (new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                int id = seekBar.getId();
                String name = getResources().getResourceEntryName(id);
                String[] split = name.split("seekBar");
                final String level = split[1];
                String payload = "id=" + patient_id + "&level=" + level + "&setting=" + progress;

                Patient patient = new Patient(payload);
                final LambdaInterface myInterface = MainActivity.factory.build(LambdaInterface.class);
                new AsyncTask<Patient, Void, String>() {
                    @Override
                    protected String doInBackground(Patient... params) {

                        try {
                            return myInterface.set_access_control(params[0]);
                        } catch (Exception e) {
                            log("Failed to invoke lambda :" + e);
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        log("Changed access control level "+level+" : " +progress);
                    }
                }.execute(patient);
                android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(name, progress);
                editor.commit();
            }
        });

        sk11.setOnSeekBarChangeListener(sklistneer);
        sk12.setOnSeekBarChangeListener(sklistneer);
        sk13.setOnSeekBarChangeListener(sklistneer);
        sk14.setOnSeekBarChangeListener(sklistneer);
        sk15.setOnSeekBarChangeListener(sklistneer);
        sk21.setOnSeekBarChangeListener(sklistneer);
        sk22.setOnSeekBarChangeListener(sklistneer);
        sk23.setOnSeekBarChangeListener(sklistneer);
        sk24.setOnSeekBarChangeListener(sklistneer);
        sk25.setOnSeekBarChangeListener(sklistneer);
    }

    public void scanqR(final View v){
        Intent intent = new Intent(getApplicationContext(), BarcodeScanner.class);
        intent.putExtra("requester", "Patient");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed(){
        if(isInSettings){
            setContentView(R.layout.activity_patient_main);
            isInSettings = false;
        }else{
            startActivity(new Intent(PatientMain.this, MainActivity.class));
        }
    }


    public void deleteData(View v) {
        Utils.showLoading(PatientMain.this);

        final LambdaInterface myInterface = MainActivity.factory.build(LambdaInterface.class);

        Patient patient = new Patient(userName);

        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {
                try {
                    return myInterface.data_delete(params[0]);
                } catch (AmazonClientException e){
                    Utils.stopLoading();
                    Utils.showBlankDialog(mContext, PatientMain.this, "Error deleting data", "Request timed out, try again later",
                            "OK");
                    //deleteData(view);
                    return null;
                } catch (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    Utils.stopLoading();
                    Utils.showBlankDialog(mContext, PatientMain.this, "Error deleting data", e.toString(),
                            "OK");
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    if (result.contains("Success")) {
                        Utils.showBlankDialog(mContext, PatientMain.this, "Success!", "All your data has been deleted",
                                "OK");
                        log("Successfuly deleted data");
                        Utils.stopLoading();
                    } else {
                        Utils.showBlankDialog(mContext, PatientMain.this, "Error deleting data", result,
                                "OK");
                        Utils.stopLoading();
                        log("Error deleting data");

                    }
                }
            }
        }.execute(patient);
    }

    public void whiteList(){

        final LambdaInterface myInterface = MainActivity.factory.build(LambdaInterface.class);

        List<String> lst = MainActivity.device_params;
        lst.add("id="+MainActivity.userName);
        String whitelistData = Utils.listToString(lst);
        Patient patient = new Patient(whitelistData);

        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {

                try {
                    return myInterface.device_whitelist(params[0]);
                } catch (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String result) {
                if(result!=null) {
                    if(result.contains("Success")){
                          log("Successfuly Whitelisted device");
                    }else{
                        log("Error Whitelisting device");

                    }
                }
            }
        }.execute(patient);
    }
    public void browseFiles(View v){
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        //We're only accepting ".csv" files
        FileDialog fileDialog = new FileDialog(this, mPath, ".csv");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    uploadToS3(file);
                }
            });
        fileDialog.showDialog();
    }

    public void uploadToS3(final File file){
        Utils.showLoading(PatientMain.this);

        log(file.getName());
        AmazonS3 s3 = new AmazonS3Client(MainActivity.credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        TransferObserver observer = transferUtility.upload(
                Constants.BUCKET,
                userName.toString()+".csv",
                file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if(state.toString().contains("COMPLETED")){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(PatientMain.this).create();
                            alertDialog.setTitle("Finished uploading file");
                            alertDialog.setMessage("Your data needs to be updated with the new file");
                            alertDialog.setButton("UPDATE", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    alertDialog.dismiss();
                                    executeLambda();
                                }
                            });
                            alertDialog.show();
                            Utils.stopLoading();
                        }
                    });
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
//                int percentage = (int) (bytesCurrent/bytesTotal*100);
            }

            @Override
            public void onError(int id, Exception ex) {
                Utils.showBlankDialog(mContext, getApplicationContext(), "Error uploading file", ex.toString(),
                        "OK");
            }
        });
    }

    public void executeLambda(){
        Utils.showLoading(PatientMain.this);

        Patient patient = new Patient(userName);

        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {
                try {
                    return lambdaInterface.table_prepare(params[0]);
                } catch (AmazonClientException e) {
                    executeLambda();
                    return null;
                } catch (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    Utils.showBlankDialog(mContext, PatientMain.this, "Error uploading data", e.toString(),
                            "OK");
                    Utils.stopLoading();
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String result) {
                if(result!=null) {
                    if (result.contains("Success")) {
                        Utils.stopLoading();
                        Utils.showBlankDialog(mContext, PatientMain.this, "Update Successful!", null,
                                "OK");
                    } else if (result.contains("Requested resource")) {
                        //recursive
                        executeLambda();
                    } else if (result.toString().contains("Data Exists")) {
                        Utils.showBlankDialog(mContext, PatientMain.this, "Previous data exists for this ID", "Delete all data before updating",
                                "OK");
                        Utils.stopLoading();
                    } else if(result.toString().contains("No Patient Found")){
                        Utils.showBlankDialog(mContext, PatientMain.this, "No patient found with this ID", "Make sure you are registered in the system",
                                "OK");
                        Utils.stopLoading();
                    } else{
                        Utils.showBlankDialog(mContext, PatientMain.this, "Oops...", result,
                                "OK");
                        Utils.stopLoading();
                    }
                }
                }

        }.execute(patient);
    }

    public void runDrugQuery(final String code) {
        try {
            long ignore = Long.parseLong(code);
            final long patient_id_long = Long.parseLong(patient_id);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        /**
                         * sample - Agispor 7290000857329
                         * https://try.jsoup.org/
                         */
                        Document doc = Jsoup.connect("http://pharmanet.co.il/Product.aspx?Barcode=" + code).get();
                        if(doc==null){
                            Utils.showBlankDialog(mContext, PatientMain.this, "Error retrieving data", "Invalid Drug",
                                    "OK");
                        }
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
                            doctor = new Doctor(header, patient_id_long);
                        }else{
                            doctor = new Doctor(activeSubstance, patient_id_long);
                        }

                        new AsyncTask<Doctor, Void, String>() {
                            @Override
                            protected String doInBackground(Doctor... params) {
                                try {
                                    return MainActivity.lambdaInterface.find_vip_genes(params[0]);
                                } catch (LambdaFunctionException e) {
                                    log("Failed to invoke lambda :" + e);
                                    Utils.showBlankDialog(mContext, PatientMain.this, "Error retrieving data", "Invalid ID or bad Code format",
                                            "OK");
                                } catch
                                        (Exception e) {
                                    log("Failed to invoke lambda :" + e);
                                    Utils.showBlankDialog(mContext, PatientMain.this, "Error retrieving data", e.toString(),
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
                                    Utils.showBlankDialog(mContext, PatientMain.this, "No genes associated with this drug in patient's VCF", "Out of danger",
                                            "OK");
                                } else {
                                    log("Data is :" + result);
                                    Utils.showBlankDialog(mContext, PatientMain.this, "WARNING! \nVery Important Pharmacogene found", result,
                                            "OK");
                                }
                            }
                        }.execute(doctor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };thread.start();
        }catch(NumberFormatException e){
            e.printStackTrace();
        };
    }

}