package com.tau.application.Query;

import org.json.JSONObject;

/**
 * Created by dan on 06/05/2017.
 */

public interface QueryInterface {
    void onTaskCompleted(JSONObject json, String endpoint, String gene);
    void onSubTaskCompleted(JSONObject json, String endpoint, String gene, String disease);
    void onAlgCompleted(String gene);
}
