package com.tau.application.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tau.application.Amazon.LambdaInterface;
import com.tau.application.DoctorMain;
import com.tau.application.MainActivity;
import com.tau.application.MainInterface;
import com.tau.application.Patient;
import com.tau.application.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.tau.application.MainActivity.access_level;
import static com.tau.application.MainActivity.mapper;
import static com.tau.application.Utils.Utils.log;
import static com.tau.application.Utils.Utils.showBlankDialog;
import static com.tau.application.Utils.Utils.stopLoading;

public class ManagePatients extends ListActivity implements MainInterface {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems = new ArrayList<String>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;

    static Activity mCtx;
    public String patientId;
    public String doctor_id;
    public String doctor_name;
    public int access_level;

    static String userType;
    public static String userName;
    public static String password;
    public static String name;
    static SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.manage_patients);
        sharedPreferences = this.getSharedPreferences(Constants.SHARED_PREF, 0);
        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String name = intent.getStringExtra("name");
        access_level = intent.getIntExtra("access_level", 100);
        if(id==null || name==null || access_level==100){
            name =sharedPreferences.getString("name", null);
            id = sharedPreferences.getString("id", null);
            access_level = sharedPreferences.getInt("access_level", 100);
        }else{
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("patient_name", name);
            editor.putString("patient_id", id);
            editor.putInt("access_level", access_level);
            editor.commit();
        }
        doctor_id = id;
        doctor_name = name;
        mCtx = this;
        adapter = new ArrayAdapter<String>(this, R.layout.custom_row_patient, R.id.patientID, listItems);
        setListAdapter(adapter);
        dialog();

        Set<String> fetch = new HashSet<>();
        fetch = sharedPreferences.getStringSet("patients", fetch);
        boolean isEmpty = fetch.isEmpty();
        if(isEmpty){
            dialog().show();
        }else{
            addPatientStart();
        }
    }

    public void onTaskCompleted(String key, HashMap<String,String> value){
        stopLoading();
        if(key.equals("is_vcf_exist")){
            String id = value.get("id");
            String name = value.get("name");
            SavePatient(id, name);

            String hasVCF;
            if(value.get("res").equals("true")){
                hasVCF = "Has VCF";
            }else {
                hasVCF = "No VCF";
            }
            String result_str = String.format("Patient: %s %s %s",  id, name, hasVCF);
            listItems.add(result_str);
            adapter.notifyDataSetChanged();
            return;
        }

        if(key.equals("patient")){
        }

    }

    public void plusPatient(View view){
        dialog().show();
    }

    public AlertDialog.Builder dialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(ManagePatients.this);
        edittext.setHint("Insert ID");
        edittext.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setMaxLines(1);
        edittext.setTextSize(20);
        edittext.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(9)
        });
        edittext.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alert.setTitle("Add a new patient");
        alert.setMessage("To add new patient, insert his/her ID");
        alert.setView(edittext);

        alert.setPositiveButton("Append", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String id = edittext.getText().toString();
                if (checkIfEsistLocally(id)){
                    Toast.makeText(getApplicationContext(), "Patient already exists", Toast.LENGTH_SHORT).show();
                } else {
                    verifyPatient(id);
                }
            }
        });

        alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Toast.makeText(getApplicationContext(), "No patient added", Toast.LENGTH_SHORT).show();
            }
        });
        return alert;
    }

    public boolean checkIfEsistLocally(String st){
        Set<String> fetch = new HashSet<>();
        fetch = sharedPreferences.getStringSet("patients", fetch);
        for(String patient: fetch){
            String[] separated = patient.split("&");
            String id = separated[0];
            if (st.equals(id)){
                return true;
            }
        }
        return false;
    }

    public void deleteRow(View v) {
        ListView lv = getListView();

        TextView pid = (TextView) findViewById(R.id.patientID);
        Set<String> fetch = new HashSet<>();
        fetch = sharedPreferences.getStringSet("patients", fetch);
        String fetchid = pid.getText().toString().split(" ")[1];
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        for(String patient: fetch){
            String[] separated = patient.split("&");
            String id = separated[0];
            if (fetchid.equals(id)){
                fetch.remove(patient);
                break;
            }
        }
        editor.putStringSet("patients", fetch);
        editor.commit();

        int position = lv.getPositionForView(v);
        listItems.remove(position);
        adapter.notifyDataSetChanged();
    }

    public void pickPatient(View v) {
        ListView lv = getListView();
        patientId  = lv.getAdapter().getItem(lv.getPositionForView(v)).toString().split(" ")[1];
        Toast.makeText(ManagePatients.this, patientId, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), DoctorMain.class);
        intent.putExtra("patient", patientId);
        intent.putExtra("id", doctor_id);
        intent.putExtra("name", doctor_name);
        intent.putExtra("access_level", access_level);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void verifyPatient(String idInput) {
        final String id = idInput;
        try {
            Long.parseLong(id);
        }catch(NumberFormatException e){
            showBlankDialog(ManagePatients.this, mCtx, "Invalid input", "Please enter a valid ID", "OK");
            return;
        }

        final long idLong = Long.parseLong(id);
        Utils.showLoading(this);

        try {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    userType = null;

                    Patient patient = mapper.load(Patient.class, idLong);
                    if (patient != null) {
                        userType = "Patient";
                        name = patient.getName();
                    }
                    return userType;
                }

                @Override
                protected void onPostExecute(String result) {
                    try {
                        if (result.equals("Patient")) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("id", id);
                            map.put("name", name);
                            checkVCF(map);
                        }
                    } catch (Exception e) {
                        log("Patient not found " + result);
                        stopLoading();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(ManagePatients.this, "Patient not found", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }.execute();
        } catch (com.amazonaws.AmazonClientException e) {
            log("Amazon client exception " + e);
            stopLoading();
        }
    }

    private void checkVCF(HashMap<String,String> map){

        final String id = map.get("id");
        final String name = map.get("name");

        final LambdaInterface myInterface = MainActivity.factory.build(LambdaInterface.class);

        Patient patient = new Patient(id);

        new AsyncTask<Patient, Void, String>() {
            @Override
            protected String doInBackground(Patient... params) {

                try {
                    return myInterface.is_vcf_exist(params[0]);
                } catch (Exception e) {
                    log("Failed to invoke lambda :" + e);
                    stopLoading();
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String result) {

                if(result!=null) {
                    HashMap<String,String> map = new HashMap<String, String>();
                    map.put("res", result);
                    map.put("id", id);
                    map.put("name", name);
                    onTaskCompleted("is_vcf_exist", map);
                }
            }
        }.execute(patient);
    }

    public void addPatientStart(){
        Set<String> fetch = new HashSet<>();
        fetch = sharedPreferences.getStringSet("patients", fetch);
        for(String patient: fetch){
            String[] separated = patient.split("&");
            String id = separated[0];
            String name = separated[1];
            listItems.add("Patient: " +id+" "+name);
            adapter.notifyDataSetChanged();
        }
    }
    static void SavePatient(String id, String name){
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> fetch = new HashSet<>();
        fetch = sharedPreferences.getStringSet("patients", fetch);
        fetch.add(id+"&"+name);
        editor.putStringSet("patients", fetch);
        editor.commit();
    }


}