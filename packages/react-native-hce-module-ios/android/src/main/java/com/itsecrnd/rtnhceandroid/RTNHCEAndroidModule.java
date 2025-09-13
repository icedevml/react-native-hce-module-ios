package com.itsecrnd.rtnhceandroid;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.util.Map;
import java.util.HashMap;
import com.itsecrnd.rtnhceandroid.NativeHCEModuleSpec;

public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String NAME = "NativeHCEModule";

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);
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
