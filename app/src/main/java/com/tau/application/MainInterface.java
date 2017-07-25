package com.tau.application;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by dan on 21/06/2017.
 */

public interface MainInterface {
    void onTaskCompleted(String key, HashMap<String, String> value);
}
