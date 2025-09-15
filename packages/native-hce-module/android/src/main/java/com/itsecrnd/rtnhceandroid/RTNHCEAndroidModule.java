package com.itsecrnd.rtnhceandroid;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String TAG = "RTNHCEAndroidModule";
    public static final String NAME = "NativeHCEModule";

    private volatile boolean sessionRunning;
    private volatile boolean hceRunning;
    // TODO not implemented
    private volatile boolean hceBreakConnection;
    private volatile boolean hceDeselected;
    private HCEServiceInterface cb;

    private void sendEvent(final String type, final String arg) {
        WritableMap map = Arguments.createMap();
        map.putString("type", type);
        map.putString("arg", arg);
        emitOnEvent(map);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        this.cb = null;

        this.sessionRunning = false;
        this.hceRunning = false;
        this.hceBreakConnection = false;
        this.hceDeselected = true;
    }

    @Override
    public void invalidate() {

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

        Log.i(TAG, "respondAPDU(): Send to callback");
        cb.onRAPDU(rapdu);

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

    public void setHCEService(HCEServiceInterface cb) {
        this.cb = cb;
    }

    public void pSendEvent(final String type, final String arg) {
        sendEvent(type, arg);
    }
}
