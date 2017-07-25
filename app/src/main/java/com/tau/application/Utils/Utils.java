package com.tau.application.Utils;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.tau.application.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by dan on 10/12/2016.
 */

public class Utils extends Application{

    private static Utils instance = new Utils();

    public static Utils getInstance(){
        return  instance;
    }

    public static void log(String str){
        Log.d(Constants.LOG_TAG, str);
    }
    static private ProgressDialog progressDialog;

    public static String getTimeinday(){
        String timeinday=null;
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if(timeOfDay >= 0 && timeOfDay < 12){
            timeinday="Good Morning, ";
        }else if(timeOfDay >= 12 && timeOfDay < 16){
            timeinday="Good Afternoon, ";
        }else if(timeOfDay >= 16 && timeOfDay < 21){
            timeinday="Good Evening, ";
        }else if(timeOfDay >= 21 && timeOfDay < 24){
            timeinday="Good Night, ";
        }
        return timeinday;
    }

    public static String listToString(List<String> lst){
        StringBuilder sb = new StringBuilder();

        for (String s : lst)
        {
            sb.append(s);
            sb.append("&");
        }
        return sb.toString();
    }

    // TODO: fix reflection
//    public static void showBlankDialog(final Context ctx, final String title, final String message,
//                                       final String button, final String c, final String obj, final Class<?> param1,
//                                       final Class<?> param2, final Class<?> param3){
//        runOnUiThread(new Runnable() {
//            public void run() {
//                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(ctx).create();
//                alertDialog.setTitle(title);
//                alertDialog.setMessage(message);
//                alertDialog.setButton(button, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        try {
//                            if(obj != null) {
//                                Method m = Class.forName(c).getDeclaredMethod(obj, null);
//                                m.invoke(Class.forName(c), param1, param2, param3);
//                            }
//                        }catch(Exception e){
//                            log(e.toString());
//                        }
//                    }
//                });
//                alertDialog.show();
//            }
//        });
//    }

    public static void showBlankDialog(final Activity activity, final Context ctx, final String title, final String message,
                                       final String button){
        try {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(ctx).create();
                    alertDialog.setTitle(title);
                    alertDialog.setMessage(message);
                    alertDialog.setButton(button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //
                        }
                    });
                    alertDialog.show();
                }
            });
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    public static void showLoading(Context ctx){
        try{
            stopLoading();
        }catch (Exception e){
            //
        }
        try {
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setMessage("Augmenting Genomics...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }catch(Exception e){
            //
        }
    }
    public static void stopLoading(){
        try {
            progressDialog.dismiss();
        }catch (Exception e){}
    }

    public static List sortHashByValues(HashMap<String, Object> h){
        HashMap<Object, String> rev = reverse(h);
        List keys = new ArrayList(rev.keySet());
        Collections.sort(keys);
        List<Pair<Object, String>> tuples= new ArrayList<Pair<Object, String>>();
        for(Iterator i = keys.iterator(); i.hasNext();){
            Object k = i.next();
            Pair<Object,String> p=  new Pair<Object, String>(k, rev.get(k));
            tuples.add(0,p)
            ;}

        return tuples;}


    public static <K,V> HashMap<V,K> reverse(Map<K,V> map) {
        HashMap<V,K> rev = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            rev.put(entry.getValue(), entry.getKey());
        return rev;
    }

}
