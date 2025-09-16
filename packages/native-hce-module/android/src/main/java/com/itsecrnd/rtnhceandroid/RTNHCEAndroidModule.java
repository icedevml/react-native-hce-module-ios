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
    private volatile boolean hceDeselected;

    private volatile boolean hceBackgroundReady;

    private HCEServiceInterface cb;

    public void sendEvent(final String type, final String arg) {
        Log.i(TAG, "RTNHCEAndroidModule:sendEvent");
        WritableMap map = Arguments.createMap();
        map.putString("type", type);
        map.putString("arg", arg);
        emitOnEvent(map);
    }

    public void sendBackgroundEvent(final String type, final String arg) {
        Log.i(TAG, "RTNHCEAndroidModule:sendBackgroundEvent");
        WritableMap map = Arguments.createMap();
        map.putString("type", type);
        map.putString("arg", arg);
        emitOnBackgroundEvent(map);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        this.cb = null;

        this.sessionRunning = false;
        this.hceRunning = false;
        this.hceDeselected = true;
        this.hceBackgroundReady = false;
    }

    @Override
    public void invalidate() {
        Log.i(TAG, "RTNHCEAndroidModule:invalidate");
    }

    @Override
    @NonNull
    public String getName() {
        Log.i(TAG, "RTNHCEAndroidModule:getName");
        return RTNHCEAndroidModule.NAME;
    }

    @Override
    public boolean isPlatformSupported() {
        Log.i(TAG, "RTNHCEAndroidModule:isPlatformSupported");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }

        PackageManager pm = getReactApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
    }

    @Override
    public void acquireExclusiveNFC(Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:acquireExclusiveNFC");
        promise.reject("err_platform_unsupported", "acquireExclusiveNFC() is not supported on Android.");
    }

    @Override
    public boolean isExclusiveNFC() {
        Log.i(TAG, "RTNHCEAndroidModule:isExclusiveNFC");
        // unsupported on Android, always false
        return false;
    }

    @Override
    public void beginSession(Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:beginSession");

        if (!isPlatformSupported()) {
            promise.reject("err_platform_unsupported", "Unsupported Android version or missing NFC/HCE feature.");
            return;
        }

        if (!this.sessionRunning) {
            this.sessionRunning = true;
            sendEvent("sessionStarted", "");
            promise.resolve(null);
        } else {
            promise.reject("err_card_session_exists", "Session already exists.");
        }
    }

    @Override
    public void setSessionAlertMessage(String message) {
        Log.i(TAG, "RTNHCEAndroidModule:setSessionAlertMessage");
        // unsupported on Android, no-op
    }

    @Override
    public void invalidateSession() {
        Log.i(TAG, "RTNHCEAndroidModule:invalidateSession");
        if (this.sessionRunning) {
            this.hceRunning = false; // FIXME
            this.sessionRunning = false;
            sendEvent("sessionInvalidated", "userInvalidated");
        }
    }

    @Override
    public boolean isSessionRunning() {
        Log.i(TAG, "RTNHCEAndroidModule:isSessionRunning");
        return this.sessionRunning;
    }

    @Override
    public void initBackgroundHCE(Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:initBackgroundHCE");

        this.hceBackgroundReady = true;

        promise.resolve(null);

        this.cb.onBackgroundHCEStarted();
    }

    @Override
    public void startHCE(Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:startHCE");
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
        Log.i(TAG, "RTNHCEAndroidModule:stopHCE");
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        }

        if (this.hceRunning) {
            this.hceRunning = false;

            if (!this.hceDeselected) {
                this.hceDeselected = true;
                sendEvent("readerDeselected", "");
            }
        }

        promise.resolve(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void respondAPDU(String rapdu, Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:respondAPDU");

        if (!this.hceBackgroundReady) {
            if (!this.sessionRunning) {
                promise.reject("err_no_session", "No session is active.");
                return;
            } else if (!this.hceRunning) {
                promise.reject("err_no_hce", "HCE is not running.");
                return;
            }
        }

        Log.i(TAG, "respondAPDU(): Send to callback");
        cb.onRAPDU(rapdu);

        promise.resolve(null);
    }

    @Override
    public void isHCERunning(Promise promise) {
        Log.i(TAG, "RTNHCEAndroidModule:isHCERunning");
        promise.resolve(this.hceRunning);
    }

    public boolean isHCEBackgroundHandlerReady() {
        return hceBackgroundReady;
    }

    public void setHCEService(HCEServiceInterface cb) {
        Log.i(TAG, "RTNHCEAndroidModule:setHCEService");

        this.cb = cb;
        this.hceDeselected = true;
        this.hceBackgroundReady = false;
    }
}
