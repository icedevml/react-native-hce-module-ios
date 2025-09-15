/*
 * Some of the code inspired by:
 * https://github.com/appidea/react-native-hce
 * https://github.com/transistorsoft/react-native-background-fetch
 */

package com.itsecrnd.rtnhceandroid.service;

import static com.facebook.react.jstasks.HeadlessJsTaskContext.Companion;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_DETECT;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_LOST;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_SEND_R_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_CAPDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_RAPDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.PERMISSION_HCE_BROADCAST;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactHost;
import com.facebook.react.ReactInstanceEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.jstasks.HeadlessJsTaskContext;
import com.facebook.react.jstasks.HeadlessJsTaskEventListener;
import com.itsecrnd.rtnhceandroid.RTNHCEAndroidModule;
import com.itsecrnd.rtnhceandroid.ReadinessCallback;
import com.itsecrnd.rtnhceandroid.util.BinaryUtils;

import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class HCEService extends HostApduService implements ReactInstanceEventListener {
    private static final String TAG = "HCEService";

    private boolean isForeground;
    private RTNHCEAndroidModule mhceModule;
    private byte[] cachedCAPDU;

    private boolean isAppOnForeground(Context context) {
        /*
         We need to check if app is in foreground otherwise the app will crash.
         https://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         */
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                Log.i(TAG, "onReceive()");
                if (action != null && action.equals(ACTION_SEND_R_APDU)) {
                    String rapdu = intent.getStringExtra(KEY_RAPDU);

                    if (rapdu == null) {
                        throw new RuntimeException("Bug! ACTION_SEND_RAPDU intent KEY_RAPDU is null.");
                    }

                    byte[] dec = BinaryUtils.HexStringToByteArray(rapdu.toUpperCase(Locale.ROOT));
                    sendResponseApdu(dec);
                } else {
                    Log.w(TAG, "Received unknown action: " + action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in HCEService.", e);
            }
        }
    };

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        String capdu = BinaryUtils.ByteArrayToHexString(command).toUpperCase(Locale.ROOT);

        if (!isForeground) {
            if (mhceModule != null && mhceModule.checkEventEmitter()) {
                mhceModule.pSendEvent("received", capdu);
            } else {
                cachedCAPDU = command;
            }
        } else {
            Intent intent = new Intent(ACTION_RECEIVE_C_APDU);
            intent.setPackage(getApplicationContext().getPackageName());
            intent.putExtra(KEY_CAPDU, capdu);
            getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
        }

        return null;
    }

    @Override
    public void onCreate() {
        ReactHost reactHost = ((ReactApplication) getApplication()).getReactHost();
        Log.i(TAG, "ReactHost: " + reactHost);

        isForeground = isAppOnForeground(getApplicationContext());
        cachedCAPDU = null;
        mhceModule = null;

        if (isForeground) {
            Log.d(TAG, "HCEService onCreate foreground");

            ReactContext reactContext = reactHost.getCurrentReactContext();

            if (reactContext != null) {
                ((RTNHCEAndroidModule) reactContext.getNativeModule("NativeHCEModule")).setSessionBeginCallback(null);
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SEND_R_APDU);
            getApplicationContext().registerReceiver(
                    receiver, filter, PERMISSION_HCE_BROADCAST, null, RECEIVER_EXPORTED);

            Intent intent = new Intent(ACTION_READER_DETECT);
            intent.setPackage(getApplicationContext().getPackageName());
            getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
        } else {
            ReactContext reactContext = reactHost.getCurrentReactContext();
            Log.d(TAG, "HCEService onCreate background");
            Log.i(TAG, "Current react context: " + reactContext);

            if (reactContext == null) {
                reactHost.addReactInstanceEventListener(this);
                reactHost.start();
            } else {
                onReactContextInitialized(reactContext);
            }
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCEService onDeactivated: " + reason);

        if (isForeground) {
            getApplicationContext().unregisterReceiver(receiver);

            Intent intent = new Intent(ACTION_READER_LOST);
            intent.setPackage(getApplicationContext().getPackageName());
            getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
        } else {
            this.mhceModule.pSendEvent("readerDeselected", "");
        }
    }

    @Override
    public void onReactContextInitialized(@NonNull ReactContext reactContext) {
        mhceModule = (RTNHCEAndroidModule) reactContext.getNativeModule("NativeHCEModule");
        mhceModule.setSessionBeginCallback(new ReadinessCallback() {
            @Override
            public void onSessionStarted() {
                Log.i(TAG, "BBB Received callback onSessionStarted");

                if (cachedCAPDU != null) {
                    mhceModule.pSendEvent("received", BinaryUtils.ByteArrayToHexString(cachedCAPDU));
                    cachedCAPDU = null;
                }
            }

            @Override
            public void onRAPDU(String rapdu) {
                sendResponseApdu(BinaryUtils.HexStringToByteArray(rapdu));
            }
        });

        HeadlessJsTaskContext headlessJsTaskContext = Companion.getInstance(reactContext);
        headlessJsTaskContext.addTaskEventListener(new HeadlessJsTaskEventListener() {
            @Override
            public void onHeadlessJsTaskStart(int i) {
                Log.i(TAG, "onHeadlessJsTaskStart: " + i);
            }

            @Override
            public void onHeadlessJsTaskFinish(int i) {
                // TODO task doesn't terminate right away but only on timeout
                Log.i(TAG, "onHeadlessJsTaskFinish: " + i);
            }
        });

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start task");
                headlessJsTaskContext.startTask(
                        new HeadlessJsTaskConfig(
                                "handleBackgroundHCECall",
                                Arguments.fromBundle(new Bundle()),
                                15000,
                                false // not allowed in foreground
                        ));
            }
        });
    }
}
