package com.tau.application.Query;

import android.os.AsyncTask;

import com.tau.application.Utils.Utils;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import static com.tau.application.Utils.Utils.log;

/**
 * Created by dan on 06/05/2017.
 */

public class HttpHandler {

    private static HttpHandler instance = new HttpHandler();

    public static HttpHandler getInstance(){
        return instance;
    }

    public void ReqeustGET(final QueryInterface queryInterface, final String input_url, final String endpoint, final String gene){
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                URL url;
                HttpURLConnection client = null;
                StringBuffer response;

                try {
                    url = new URL(input_url);
                    client = (HttpURLConnection) url.openConnection();
                    client.setRequestMethod("GET");
                    client.setRequestProperty("Accept", "application/json");
                    client.setRequestProperty("Content-Type", "application/json");
                    client.setDoOutput(true);
                    int timeoutConnection = 20000; //20 sec timeout
                    client.setReadTimeout(timeoutConnection);
                    int responseCode = client.getResponseCode();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream()));
                    String inputLine;
                    response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    JSONObject json = new JSONObject(response.toString());
                    queryInterface.onTaskCompleted(json, endpoint, gene);
                }
                catch(Exception e){
                    e.printStackTrace();
                    if(e.toString().contains("SocketTimeoutException") && endpoint.contains("DISGENET")){
                        queryInterface.onAlgCompleted("Timeout");
                    }
                }
                finally {
                    if(client != null)
                        client.disconnect();
                }
                return null;
            }
        }.execute();
    }

    public void ReqeustGET_Multiple(final QueryInterface queryInterface, final String input_url, final String disease_id, final String endpoint, final String gene) {
        final String url_formatted = String.format(input_url, disease_id);
        URL url;
        HttpURLConnection client = null;
        StringBuffer response;
        try {
            final String url_encoded = URLEncoder.encode(url_formatted, "UTF-8");
            String url_complete = String.format(QueryAlgorithm.getInstance().URL_DISGENET, url_encoded);
            url = new URL(url_complete);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("GET");
            client.setRequestProperty("Accept", "application/json");
            client.setRequestProperty("Content-Type", "application/json");
            client.setDoOutput(true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject json = new JSONObject(response.toString());

            log("Request Output : " +json);

            queryInterface.onSubTaskCompleted(json, endpoint, gene, disease_id);

        }
        catch(Exception e){
            queryInterface.onSubTaskCompleted(null, endpoint, gene, disease_id);
            e.printStackTrace();

        }
        finally {
            if(client != null)
                client.disconnect();
        }
    }
}
