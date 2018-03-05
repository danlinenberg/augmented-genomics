package com.tau.application;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.tau.application.Amazon.LambdaInterface;
import com.tau.application.Smarteyeglass.SmarteyeglassUtils;
import com.tau.application.Utils.Constants;
import com.tau.application.Utils.ManagePatients;
import com.tau.application.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.parseLong;

public class  MainActivity extends Activity {

    private String tid = Utils.getTimeinday();
    EditText mEdit_id;
    EditText mEdit_password;
    public static DynamoDBMapper mapper;
    static String userType;
    public static String userName;
    public static String password;
    public static String name;
    public static int access_level;
    public static CognitoCachingCredentialsProvider credentialsProvider;
    public static LambdaInterface lambdaInterface;
    public static LambdaInvokerFactory factory;
    public static List<String> device_params;

    static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        device_params = new ArrayList<>();
        /**
         * Smarteyeglass start
         */
        Intent intent = new Intent(Control.Intents
                .CONTROL_START_REQUEST_INTENT);
        ExtensionUtils.sendToHostApp(getApplicationContext(),
                "com.sony.smarteyeglass", intent);

        //Device params for Barcode Generator & Whitelist
        new Thread(new Runnable() {
            public void run() {
                AdvertisingIdClient.Info adInfo;
                String gaid;
                String imei;
                String android_id;
                try {
                    //GAID
                    adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    gaid = adInfo.getId();
                    device_params.add("gaid="+gaid);
                }catch(Exception e) {
                    e.printStackTrace();
                }
                try {
                    //IMEI
                    TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    if(tm.getDeviceId()!=null){
                        imei = tm.getDeviceId();
                        device_params.add("imei="+imei);
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }
                try {
                    //Android ID
                    if(Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID)!=null) {
                        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                        device_params.add("android_id="+android_id);
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

        sharedPreferences = this.getSharedPreferences(Constants.SHARED_PREF, 0);
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                Constants.TABLE_ID, // Federated Identity Pool ID
                Regions.EU_WEST_1 // Region
        );
        factory = new LambdaInvokerFactory(
                getApplicationContext(),
                Regions.EU_WEST_1,
                credentialsProvider);
        lambdaInterface = factory.build(LambdaInterface.class);

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        ddbClient.setRegion(Region.getRegion(Regions.EU_WEST_1));

        mapper = new DynamoDBMapper(ddbClient);
        mEdit_id = (EditText) findViewById(R.id.edit_id);
        mEdit_password = (EditText) findViewById(R.id.edit_pass);

        if (sharedPreferences.getString("username", null) != null) {
            mEdit_id.setText(sharedPreferences.getString("username", null).toString());
        }
        if (sharedPreferences.getString("password", null) != null) {
            mEdit_password.setText(sharedPreferences.getString("password", null).toString());
        }


        /**
         * asking for permissions
         * we don't handle a scenario where user declines. this will be requested as long as the app doesn't have all permissions
         */
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Utils.stopLoading();
    }

    public void Login(View v) {
        Utils.showLoading(MainActivity.this);

        AmazonCognitoIdentityProvider identityProvider = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(), new ClientConfiguration());
        identityProvider.setRegion(Region.getRegion(Regions.EU_WEST_1));
        CognitoUserPool userPool = new CognitoUserPool(getApplicationContext(),
                Constants.USER_POOL_ID,
                Constants.CLIENT_ID,
                Constants.CLIENT_SECRET,
                identityProvider);
        // Callback handler for the sign-in process
        password = mEdit_password.getText().toString();
        userName = mEdit_id.getText().toString();


        AuthenticationHandler authenticationHandler = new AuthenticationHandler() {

            @Override
            public void onSuccess(CognitoUserSession cognitoUserSession) {
                // Sign-in was successful, cognitoUserSession will contain tokens for the user
                Utils.log("Signed in");
                android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("username", userName);
                editor.putString("password", password);
                editor.commit();

                getIdentity(parseLong(userName));
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                // The API needs user sign-in credentials to continue
                AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, password, null);
                // Pass the user sign-in credentials to the continuation
                authenticationContinuation.setAuthenticationDetails(authenticationDetails);
                // Allow the sign-in to continue
                authenticationContinuation.continueTask();
            }

            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
                // Multi-factor authentication is required; get the verification code from user
                //multiFactorAuthenticationContinuation.setMfaCode("123");
                // Allow the sign-in process to continue
                //multiFactorAuthenticationContinuation.continueTask();
            }

            @Override
            public void onFailure(Exception exception) {
                Utils.log("Cannot sign in, " + exception);
                if(exception.toString().contains("AmazonServiceException")) {
                    showToast("No internet connection");
                }else{
                    showToast("Wrong Username/Password");
                }
                Utils.stopLoading();
                // Sign-in failed, check exception for the cause
            }
        };

        final CognitoUser cognitoUser = userPool.getUser(userName);
        cognitoUser.getSessionInBackground(authenticationHandler);
    }

    public void getIdentity(final long id) {

        try {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    userType = null;
                    Doctor doctor = mapper.load(Doctor.class, id);
                    Patient patient = mapper.load(Patient.class, id);
                    if (patient != null) {
                        userType = "Patient";
                        name = patient.getName();
                    }
                    if (doctor != null) {
                        userType = "Doctor";
                        name = doctor.getName();
                        access_level = doctor.getAccessLevel();
                    }
                    return userType;

                }

                @Override
                protected void onPostExecute(String result) {
                    try {

                        String fullClassName = getPackageName() + "." + result + "Main";
                        Class c = Class.forName(fullClassName);
                        Intent intent;
                        if (result.equals("Doctor")){
                            intent = new Intent(getApplicationContext(), ManagePatients.class);
                        }
                        else {intent = new Intent(getApplicationContext(), c);}
                        intent.putExtra("id", Long.toString(id));
                        intent.putExtra("name", name);
                        intent.putExtra("access_level", access_level);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        showToast(Utils.getTimeinday() + result);

                        //TODO Update layout on any new information (eg. display patient name and ID when the doctor picks a new patient from the list)
                        /**
                         * Smarteyeglass
                         */
                        SmarteyeglassUtils.getInstance().updateLayout(getApplicationContext(), "Welcome " + name.split(" ",2)[0]);

                    } catch (Exception e) {
                        Utils.log("Class not found " + result);
                        showToast("No user with this ID");
                    }
                }
            }.execute();
        }catch (com.amazonaws.AmazonClientException e){
            Utils.log("Amazon client exception " +e);
        }
    }

    public void showToast(final String txt)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getApplicationContext(),txt, Toast.LENGTH_SHORT).show();
            }
        });
    }
}