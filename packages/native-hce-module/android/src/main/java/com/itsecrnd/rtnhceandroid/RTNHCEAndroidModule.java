package com.itsecrnd.rtnhceandroid;

import static android.content.Context.RECEIVER_EXPORTED;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_DETECT;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_LOST;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_SEND_R_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.PERMISSION_HCE_BROADCAST;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String TAG = "RTNHCEAndroidModule";
    public static final String NAME = "NativeHCEModule";

    private volatile boolean sessionRunning;
    private volatile boolean hceRunning;
    private volatile boolean hceBreakConnection;
    private volatile boolean hceDeselected;
    private ReadinessCallback cb;

    private void sendEvent(final String type, final String arg) {
        WritableMap map = Arguments.createMap();
        map.putString("type", type);
        map.putString("arg", arg);
        emitOnEvent(map);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                if (action == null) {
                    Log.w(TAG, "Received intent with null action in module receiver.");
                    return;
                }

                if (action.equals(ACTION_RECEIVE_C_APDU)) {
                    String capdu = intent.getStringExtra("capdu");

                    if (!hceBreakConnection && hceRunning) {
                        sendEvent("received", capdu);
                    } else {
                        // HCE is already disabled or was just disabled while the HCEService was running
                        // don't forward C-APDUs to the application and just respond 6999 to everything
                        Intent rintent = new Intent(ACTION_SEND_R_APDU);
                        rintent.setPackage(getReactApplicationContext().getPackageName());
                        rintent.putExtra("rapdu", "6999");
                        getReactApplicationContext().getApplicationContext().sendBroadcast(rintent, PERMISSION_HCE_BROADCAST);
                    }
                } else if (action.equals(ACTION_READER_DETECT)) {
                    if (hceRunning) {
                        hceDeselected = false;
                        sendEvent("readerDetected", "");
                    }
                } else if (action.equals(ACTION_READER_LOST)) {
                    if (hceRunning && !hceBreakConnection && !hceDeselected) {
                        // only if we haven't send a fake disconnect event already
                        hceDeselected = true;
                        sendEvent("readerDeselected", "");
                    }

                    hceBreakConnection = false;
                } else {
                    Log.w(TAG, "Received unknown action intent in module receiver: " + action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in RTNHCEAndroidModule.", e);
            }
        }
    };

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        this.cb = null;

        this.sessionRunning = false;
        this.hceRunning = false;
        this.hceBreakConnection = false;
        this.hceDeselected = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_RECEIVE_C_APDU);
            filter.addAction(ACTION_READER_DETECT);
            filter.addAction(ACTION_READER_LOST);

            getReactApplicationContext().getApplicationContext().registerReceiver(
                    receiver,
                    filter,
                    PERMISSION_HCE_BROADCAST,
                    null,
                    RECEIVER_EXPORTED
            );
        }
    }

    @Override
    public void invalidate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getReactApplicationContext().getApplicationContext().unregisterReceiver(receiver);
        }
    }

    @Override
    @NonNull
    public String getName() {
        return RTNHCEAndroidModule.NAME;
    }

    @Override
    public boolean isPlatformSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }

        PackageManager pm = getReactApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
    }

    @Override
    public void acquireExclusiveNFC(Promise promise) {
        promise.reject("err_platform_unsupported", "acquireExclusiveNFC() is not supported on Android.");
    }

    @Override
    public boolean isExclusiveNFC() {
        // unsupported on Android, always false
        return false;
    }

    @Override
    public void beginSession(Promise promise) {
        if (!isPlatformSupported()) {
            promise.reject("err_platform_unsupported", "Unsupported Android version or missing NFC/HCE feature.");
            return;
        }

        if (!this.sessionRunning) {
            this.sessionRunning = true;
            sendEvent("sessionStarted", "");
            promise.resolve(null);

            if (cb != null) {
                cb.onSessionStarted();
            }
        } else {
            promise.reject("err_card_session_exists", "Session already exists.");
        }
    }

    @Override
    public void setSessionAlertMessage(String message) {
        // unsupported on Android, no-op
    }

    @Override
    public void invalidateSession() {
        if (this.sessionRunning) {
            this.sessionRunning = false;
            sendEvent("sessionInvalidated", "userInvalidated");
        }
    }

    @Override
    public boolean isSessionRunning() {
        return this.sessionRunning;
    }

    @Override
    public void startHCE(Promise promise) {
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        }

        if (this.hceRunning) {
            promise.reject("err_start_emulation", "Error trying to start emulation. Emulation already started.");
            return;
        }

        this.hceRunning = true;
        promise.resolve(null);
    }

    @Override
    public void stopHCE(String status, Promise promise) {
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        }

        if (this.hceRunning) {
            this.hceRunning = false;

            if (!this.hceDeselected) {
                this.hceDeselected = true;
                this.hceBreakConnection = true;
                sendEvent("readerDeselected", "");
            }
        }

        promise.resolve(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void respondAPDU(String rapdu, Promise promise) {
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        } else if (!this.hceRunning) {
            promise.reject("err_no_hce", "HCE is not running.");
            return;
        }

        if (this.cb != null) {
            cb.onRAPDU(rapdu);
        } else {
            Intent intent = new Intent(ACTION_SEND_R_APDU);
            intent.setPackage(getReactApplicationContext().getPackageName());
            intent.putExtra("rapdu", rapdu);
            getReactApplicationContext().getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
        }

        promise.resolve(null);
    }

    @Override
    public void isHCERunning(Promise promise) {
        promise.resolve(this.hceRunning);
    }

    public boolean checkEventEmitter() {
        Log.i(TAG, "BBB has event emitter? " + mEventEmitterCallback);
        return mEventEmitterCallback != null;
    }

    public void setSessionBeginCallback(ReadinessCallback cb) {
        this.cb = cb;
    }

    public void pSendEvent(final String type, final String arg) {
        sendEvent(type, arg);
    }
}
