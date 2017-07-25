package com.tau.application.Smarteyeglass;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.tau.application.R;

import static com.tau.application.Utils.Utils.log;

/**
 * Created by dan on 03/06/2017.
 */

public class SmarteyeglassUtils extends Application{

    private static SmarteyeglassUtils instance = new SmarteyeglassUtils();

    public static SmarteyeglassUtils getInstance(){
        return  instance;
    }

    public void updateLayout(Context ctx,String msg){
        try {
            Intent intent = new Intent(Control.Intents.CONTROL_SEND_TEXT_INTENT);
            intent.putExtra(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.btn_update_this);
            intent.putExtra(Control.Intents.EXTRA_TEXT, msg);
            ExtensionUtils.sendToHostApp(ctx, "com.sony.smarteyeglass", intent);
        }catch (Exception e){
            log("Cannot detect Smarteyeglasses");
        }
    }
}
