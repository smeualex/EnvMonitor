package com.smeualex.envmonitor.util;

import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

/**
 * Created by alex on 2017-12-22.
 */

public class Util {
    public static void msgSnack(View v, String msg){
        Log.v("SNACKBAR", " >> Creating a snackbar: " + msg);
        Snackbar.make(v, msg, Snackbar.LENGTH_LONG).show();
    }
}
