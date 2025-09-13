package com.itsecrnd.rtnhceandroid;

import static android.content.Context.RECEIVER_EXPORTED;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;

public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String TAG = "RTNHCEAndroidModule";
    public static final String NAME = "NativeHCEModule";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received broadcast in RTNHCEAndroidModule.");
            String action = intent.getAction();

            if (action != null && action.equals("com.itsecrnd.rtnhceandroid.sendcommandapdu")) {
                Log.i(TAG, "Received C-APDU: " + intent.getStringExtra("capdu"));
            }
        }
    };

    @SuppressLint("NewApi")  // TODO remove
    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.itsecrnd.rtnhceandroid.sendcommandapdu");
        getReactApplicationContext().getApplicationContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED);

        // TODO unregister receiver
    }

    @Override
    @NonNull
    public String getName() {
        return RTNHCEAndroidModule.NAME;
    }

    @Override
    public boolean isPlatformSupported() {
        return false;
    }

    @Override
    public void acquireExclusiveNFC(Promise promise) {
        Log.i(TAG, "Send broadcast.");
        Intent intent = new Intent("com.itsecrnd.rtnhceandroid.sendresponseapdu");
        intent.putExtra("rapdu", "EE9000");
        this.getReactApplicationContext().getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public boolean isExclusiveNFC() {
        return false;
    }

    @Override
    public void beginSession(Promise promise) {

    }

    @Override
    public void setSessionAlertMessage(String message) {

    }

    @Override
    public void invalidateSession() {

    }

    @Override
    public boolean isSessionRunning() {
        return false;
    }

    @Override
    public void startHCE(Promise promise) {

    }

    @Override
    public void stopHCE(String status, Promise promise) {

    }

    @Override
    public void respondAPDU(String rapdu, Promise promise) {

    }

    @Override
    public void isHCERunning(Promise promise) {

    }
}
