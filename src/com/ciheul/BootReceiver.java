package com.ciheul;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    // when booting up the android phone, turn updater service on automatically
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d("BootReceiver", "onReceived");
        context.startService(new Intent(context, UpdaterService.class));
    }

}
