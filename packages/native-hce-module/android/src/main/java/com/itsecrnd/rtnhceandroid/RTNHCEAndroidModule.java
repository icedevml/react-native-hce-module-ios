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

    private final SecretKey encSecretKey;
    private boolean sessionOpen;

    private void toggleHceService(Boolean enabled) {
        getReactApplicationContext()
                .getPackageManager()
                .setComponentEnabledSetting(
                        new ComponentName(getReactApplicationContext(), HCEService.class),
                        enabled
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
    }

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
                        sendEvent("received", capdu);
                    } else if (action.equals(ACTION_READER_DETECT)) {
                        sendEvent("readerDetected", "");
                    } else if (action.equals(ACTION_READER_LOST)) {
                        sendEvent("readerDeselected", "");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in RTNHCEAndroidModule.", e);
            }
        }
    };

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        this.sessionOpen = false;

        String encKey;

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
        // unsupported on Android
        return false;
    }

    @Override
    public void beginSession(Promise promise) {
        if (this.sessionOpen) {
            promise.reject("err_card_session_exists", "Session already exists.");
        } else {
            this.sessionOpen = true;
            promise.resolve(null);
            sendEvent("sessionStarted", "");
        }
    }

    @Override
    public void setSessionAlertMessage(String message) {
        // unsupported on Android, no-op
    }

    @Override
    public void invalidateSession() {
        toggleHceService(false);
        this.sessionOpen = false;
        sendEvent("sessionInvalidated", "");
    }

    @Override
    public boolean isSessionRunning() {
        return this.sessionOpen;
    }

    @Override
    public void startHCE(Promise promise) {
        toggleHceService(true);
        promise.resolve(null);
    }

    @Override
    public void stopHCE(String status, Promise promise) {
        toggleHceService(false);
        promise.resolve(null);
    }

    @Override
    public void respondAPDU(String rapdu, Promise promise) {
        if (!this.sessionOpen) {
            return;
        }

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
        int enabledSetting = getReactApplicationContext()
                .getPackageManager()
                .getComponentEnabledSetting(new ComponentName(getReactApplicationContext(), HCEService.class));

        promise.resolve(enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }
}
