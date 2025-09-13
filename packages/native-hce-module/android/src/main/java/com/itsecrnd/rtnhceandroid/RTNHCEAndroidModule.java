package com.itsecrnd.rtnhceandroid;

import static android.content.Context.RECEIVER_EXPORTED;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_DETECT;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_LOST;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_SEND_R_APDU;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

import javax.crypto.SecretKey;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String TAG = "RTNHCEAndroidModule";
    public static final String NAME = "NativeHCEModule";

    private volatile boolean sessionRunning;
    private volatile boolean hceRunning;
    private volatile boolean hceBreakConnection;
    private final SecretKey encSecretKey;

    private void sendEvent(final String type, final String arg) {
        WritableMap map = Arguments.createMap();
        map.putString("type", type);
        map.putString("arg", arg);
        emitOnEvent(map);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                if (action != null) {
                    AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra("auth"));

                    if (action.equals(ACTION_RECEIVE_C_APDU)) {
                        String capdu = AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra("capdu"));

                        if (!hceBreakConnection) {
                            sendEvent("received", capdu);
                        } else {
                            Intent rintent = new Intent(ACTION_SEND_R_APDU);
                            rintent.setPackage(getReactApplicationContext().getPackageName());
                            rintent.putExtra("auth", AESGCMUtil.encryptData(encSecretKey, AESGCMUtil.randomString()));
                            rintent.putExtra("rapdu", "6999");
                            getReactApplicationContext().getApplicationContext().sendBroadcast(rintent);
                        }
                    } else if (action.equals(ACTION_READER_DETECT)) {
                        sendEvent("readerDetected", "");
                    } else if (action.equals(ACTION_READER_LOST)) {
                        if (!hceBreakConnection) {
                            // only if we haven't send a fake disconnect event already
                            sendEvent("readerDeselected", "");
                        }

                        hceBreakConnection = false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in RTNHCEAndroidModule.", e);
            }
        }
    };

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);
        String encKey;
        this.sessionRunning = false;
        this.hceRunning = false;
        this.hceBreakConnection = false;

        try {
            encSecretKey = AESGCMUtil.generateKey();
            encKey = BinaryUtils.ByteArrayToHexString(encSecretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key.", e);
        }

        String prefsName = getReactApplicationContext().getPackageName() + ".nativehcemodule";
        getReactApplicationContext()
                .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putString("encKey", encKey)
                .commit();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RECEIVE_C_APDU);
        filter.addAction(ACTION_READER_DETECT);
        filter.addAction(ACTION_READER_LOST);

        /*
         * FIXME It seems like those intents are not going through across this module and HostApduService.
         * This happens even though those things are within the same application and package.
         * It also seems like there is no way to bind HostApduService (or it's a skill issue).
         *
         * For now we will just encrypt&authenticate all messages using AES/GCM and use exported
         * receivers. This is not a perfect solution but it is going to be secure and sufficient for now.
         *
         * If somebody knows a better way then any pull requests would be greatly appreciated.
         */
        getReactApplicationContext().getApplicationContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED);
    }

    @Override
    public void invalidate() {
        getReactApplicationContext().getApplicationContext().unregisterReceiver(receiver);
    }

    @Override
    @NonNull
    public String getName() {
        return RTNHCEAndroidModule.NAME;
    }

    @Override
    public boolean isPlatformSupported() {
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
        if (this.hceRunning) {
            promise.reject("err_start_emulation", "Error trying to start emulation. Emulation already started.");
            return;
        }

        this.hceRunning = true;
        promise.resolve(null);
    }

    @Override
    public void stopHCE(String status, Promise promise) {
        if (this.hceRunning) {
            this.hceBreakConnection = true;
            this.hceRunning = false;

            sendEvent("readerDeselected", "");
        }

        promise.resolve(null);
    }

    @Override
    public void respondAPDU(String rapdu, Promise promise) {
        String encRapdu = AESGCMUtil.encryptData(encSecretKey, rapdu);

        Intent intent = new Intent(ACTION_SEND_R_APDU);
        intent.setPackage(getReactApplicationContext().getPackageName());
        intent.putExtra("auth", AESGCMUtil.encryptData(encSecretKey, AESGCMUtil.randomString()));
        intent.putExtra("rapdu", encRapdu);
        getReactApplicationContext().getApplicationContext().sendBroadcast(intent);

        promise.resolve(null);
    }

    @Override
    public void isHCERunning(Promise promise) {
        // unsupported on Android, always true
        promise.resolve(true);
    }
}
