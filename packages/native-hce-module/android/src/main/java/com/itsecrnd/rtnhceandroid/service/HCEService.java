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
import com.itsecrnd.rtnhceandroid.util.BinaryUtils;

import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class HCEService extends HostApduService {
    private static final String TAG = "HCEService";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

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

        Intent intent = new Intent(ACTION_RECEIVE_C_APDU);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra(KEY_CAPDU, capdu);
        getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);

        return null;
    }

    private boolean isAppOnForeground(Context context) {
        /**
         We need to check if app is in foreground otherwise the app will crash.
         https://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         **/
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

    @Override
    public void onCreate() {
        if (isAppOnForeground(getApplicationContext())) {
            Log.d(TAG, "HCEService onCreate foreground");

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SEND_R_APDU);
            getApplicationContext().registerReceiver(
                    receiver, filter, PERMISSION_HCE_BROADCAST, null, RECEIVER_EXPORTED);

            Intent intent = new Intent(ACTION_READER_DETECT);
            intent.setPackage(getApplicationContext().getPackageName());
            getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
        } else {
            Log.d(TAG, "HCEService onCreate background");

            ReactHost reactHost = ((ReactApplication) getApplication()).getReactHost();
            reactHost.addReactInstanceEventListener(new ReactInstanceEventListener() {
                @Override
                public void onReactContextInitialized(@NonNull ReactContext reactContext) {
                    HeadlessJsTaskContext headlessJsTaskContext = Companion.getInstance(reactContext);

                    headlessJsTaskContext.addTaskEventListener(new HeadlessJsTaskEventListener() {
                        @Override
                        public void onHeadlessJsTaskStart(int i) {
                            Log.i(TAG, "onHeadlessJsTaskStart: " + i);
                        }

                        @Override
                        public void onHeadlessJsTaskFinish(int i) {
                            Log.i(TAG, "onHeadlessJsTaskFinish: " + i);
                        }
                    });

                    UiThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // TODO check if we need to stop task somehow
                            headlessJsTaskContext.startTask(
                                    new HeadlessJsTaskConfig(
                                            "handleBackgroundHCECall",
                                            Arguments.fromBundle(new Bundle()),
                                            5000, // timeout in milliseconds for the task
                                            false // optional: defines whether or not the task is allowed in foreground. Default is false
                                    ));
                        }
                    });

                    reactHost.removeReactInstanceEventListener(this);
                }
            });
            reactHost.start();
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCEService onDeactivated: " + reason);

        // TODO remove event listeners for headlessJsTaskContext

        getApplicationContext().unregisterReceiver(receiver);

        Intent intent = new Intent(ACTION_READER_LOST);
        intent.setPackage(getApplicationContext().getPackageName());
        getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
    }
}
