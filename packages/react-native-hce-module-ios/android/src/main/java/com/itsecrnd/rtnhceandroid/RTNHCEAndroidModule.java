package com.itsecrnd.rtnhceandroid;

import static android.content.Context.RECEIVER_EXPORTED;

import static com.itsecrnd.rtnhceandroid.IntentKeys.INTENT_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.INTENT_SEND_R_APDU;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(INTENT_RECEIVE_C_APDU)) {
                String capdu = AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra("capdu"));

                WritableMap map = Arguments.createMap();
                map.putString("type", "received");
                map.putString("arg", capdu);
                emitOnEvent(map);
            }
        }
    };

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);
        String encKey;

        try {
            encSecretKey = AESGCMUtil.generateKey();
            encKey = BinaryUtils.ByteArrayToHexString(encSecretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key.", e);
        }

        getReactApplicationContext()
                .getSharedPreferences("RTNHCEAndroidModuleBroadcastEnc", Context.MODE_PRIVATE)
                .edit()
                .putString("encKey", encKey)
                .commit();

        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_RECEIVE_C_APDU);

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
        return true;
    }

    @Override
    public void acquireExclusiveNFC(Promise promise) {
        promise.resolve(null);
    }

    @Override
    public boolean isExclusiveNFC() {
        return false;
    }

    @Override
    public void beginSession(Promise promise) {
        promise.resolve(null);
    }

    @Override
    public void setSessionAlertMessage(String message) {

    }

    @Override
    public void invalidateSession() {

    }

    @Override
    public boolean isSessionRunning() {
        return true;
    }

    @Override
    public void startHCE(Promise promise) {
        promise.resolve(null);
    }

    @Override
    public void stopHCE(String status, Promise promise) {
        promise.resolve(null);
    }

    @Override
    public void respondAPDU(String rapdu, Promise promise) {
        String encRapdu = AESGCMUtil.encryptData(encSecretKey, rapdu);

        Intent intent = new Intent(INTENT_SEND_R_APDU);
        intent.setPackage(getReactApplicationContext().getPackageName());
        intent.putExtra("rapdu", encRapdu);
        getReactApplicationContext().getApplicationContext().sendBroadcast(intent);

        promise.resolve(null);
    }

    @Override
    public void isHCERunning(Promise promise) {
        promise.resolve(true);
    }
}
