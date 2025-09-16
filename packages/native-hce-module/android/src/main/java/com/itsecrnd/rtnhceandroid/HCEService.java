/*
 * Some of the code inspired by:
 * https://github.com/appidea/react-native-hce
 * https://github.com/transistorsoft/react-native-background-fetch
 */

package com.itsecrnd.rtnhceandroid;

import static com.facebook.react.jstasks.HeadlessJsTaskContext.Companion;

import android.app.ActivityManager;
import android.content.Context;
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

import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class HCEService extends HostApduService implements ReactInstanceEventListener, HCEServiceCallback {
    private static final String TAG = "HCEService";

    private boolean isForeground;
    private RTNHCEAndroidModule hceModule;
    private byte[] pendingCAPDU;

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

    @Override
    public void onBackgroundHCEInit() {
        Log.d(TAG, "HCEService:onBackgroundHCEInit");

        if (pendingCAPDU != null) {
            Log.d(TAG, "HCEService:onBackgroundHCEInit send pendingCAPDU");
            hceModule.sendBackgroundEvent("received", BinaryUtils.ByteArrayToHexString(pendingCAPDU));
            pendingCAPDU = null;
        }
    }

    @Override
    public void onRespondAPDU(String rapdu) {
        Log.d(TAG, "HCEService:onRespondAPDU");
        sendResponseApdu(BinaryUtils.HexStringToByteArray(rapdu));
    }

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        Log.d(TAG, "HCEService:processCommandApdu");
        String capdu = BinaryUtils.ByteArrayToHexString(command).toUpperCase(Locale.ROOT);

        if (isForeground && hceModule._isHCERunning() && !hceModule.isHCEBrokenConnection()) {
            Log.d(TAG, "HCEService:processCommandApdu foreground sendEvent received");
            hceModule.sendEvent("received", capdu);
        } else {
            if (hceModule != null && hceModule.isHCEBackgroundHandlerReady()) {
                Log.d(TAG, "HCEService:processCommandApdu background sendBackgroundEvent received");
                hceModule.sendBackgroundEvent("received", capdu);
            } else {
                Log.d(TAG, "HCEService:processCommandApdu background pendingCAPDU");
                pendingCAPDU = command;
            }
        }

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "HCEService:onCreate");

        ReactHost reactHost = ((ReactApplication) getApplication()).getReactHost();

        if (reactHost == null) {
            throw new RuntimeException("BUG! getReactHost() returned null.");
        }

        isForeground = isAppOnForeground(getApplicationContext());
        pendingCAPDU = null;
        hceModule = null;

        if (isForeground) {
            Log.d(TAG, "HCEService:onCreate foreground");

            ReactContext reactContext = reactHost.getCurrentReactContext();

            if (reactContext == null) {
                throw new RuntimeException("BUG! getCurrentReactContext() returned null in foreground.");
            }

            this.hceModule = ((RTNHCEAndroidModule) reactContext.getNativeModule("NativeHCEModule"));

            if (this.hceModule == null) {
                throw new RuntimeException("BUG! getNativeModule() returned null in foreground.");
            }

            this.hceModule.setHCEService(this);

            if (this.hceModule._isHCERunning()) {
                this.hceModule.sendEvent("readerDetected", "");
            } else {
                this.hceModule.setHCEBrokenConnection();
            }
        } else {
            ReactContext reactContext = reactHost.getCurrentReactContext();
            Log.d(TAG, "HCEService:onCreate background");

            if (reactContext == null) {
                reactHost.addReactInstanceEventListener(this);
                reactHost.start();
            } else {
                setupRunJSTask(reactContext);
            }
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCEService:onDeactivated: " + reason);

        if (isForeground) {
            if (this.hceModule != null && !this.hceModule.isHCEBrokenConnection()) {
                this.hceModule.sendEvent("readerDeselected", "");
            }
        } else {
            this.hceModule.sendBackgroundEvent("readerDeselected", "");
        }

        this.hceModule.setHCEService(null);
    }

    @Override
    public void onReactContextInitialized(@NonNull ReactContext reactContext) {
        HeadlessJsTaskContext headlessJsTaskContext = Companion.getInstance(reactContext);
        headlessJsTaskContext.addTaskEventListener(new HeadlessJsTaskEventListener() {
            @Override
            public void onHeadlessJsTaskStart(int i) {
                Log.d(TAG, "HCEService:HeadlessJsTaskEventListener:onHeadlessJsTaskStart: " + i);
            }

            @Override
            public void onHeadlessJsTaskFinish(int i) {
                Log.d(TAG, "HCEService:HeadlessJsTaskEventListener:onHeadlessJsTaskFinish: " + i);
            }
        });

        setupRunJSTask(reactContext);
    }

    public void setupRunJSTask(@NonNull ReactContext reactContext) {
        hceModule = (RTNHCEAndroidModule) reactContext.getNativeModule("NativeHCEModule");

        if (hceModule == null) {
            throw new RuntimeException("BUG! getNativeModule() returned null in background.");
        }

        hceModule.setHCEService(this);

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "HCEService:setupRunJSTask:runOnUiThread startTask");
                HeadlessJsTaskContext headlessJsTaskContext = Companion.getInstance(reactContext);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "HCEService:onDestroy");
    }
}
