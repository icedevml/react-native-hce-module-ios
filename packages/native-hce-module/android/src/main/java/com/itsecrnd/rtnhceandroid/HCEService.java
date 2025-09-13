package com.itsecrnd.rtnhceandroid;

import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_DETECT;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_LOST;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_SEND_R_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_AUTH;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_CAPDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_RAPDU;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Locale;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class HCEService extends HostApduService {
    private static final String TAG = "HCEService";

    private SecretKey encSecretKey;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                if (action != null) {
                    AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra(KEY_AUTH));

                    if (action.equals(ACTION_SEND_R_APDU)) {
                        String rapdu = AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra(KEY_RAPDU));
                        byte[] dec = BinaryUtils.HexStringToByteArray(rapdu.toUpperCase(Locale.ROOT));
                        sendResponseApdu(dec);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in HCEService.", e);
            }
        }
    };

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        String capdu = AESGCMUtil.encryptData(encSecretKey, BinaryUtils.ByteArrayToHexString(command));

        Intent intent = new Intent(ACTION_RECEIVE_C_APDU);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra(KEY_AUTH, AESGCMUtil.encryptData(encSecretKey, AESGCMUtil.randomString()));
        intent.putExtra(KEY_CAPDU, capdu);
        getApplicationContext().sendBroadcast(intent);

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "HCEService onCreate");

        String prefsName = getApplicationContext().getPackageName() + ".nativehcemodule";
        String encKey = getApplicationContext()
                .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .getString("encKey", "");

        if (encKey.isEmpty()) {
            throw new RuntimeException("Failed to load encKey.");
        }

        byte[] bArr = BinaryUtils.HexStringToByteArray(encKey);
        encSecretKey = new SecretKeySpec(bArr, 0, bArr.length, "AES");

        /*
         * See a comment about registerReceiver() in RTNHCEAndroidModule.
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_R_APDU);
        getApplicationContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED);

        Intent intent = new Intent(ACTION_READER_DETECT);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra(KEY_AUTH, AESGCMUtil.encryptData(encSecretKey, AESGCMUtil.randomString()));
        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCEService onDeactivated: " + reason);

        getApplicationContext().unregisterReceiver(receiver);

        Intent intent = new Intent(ACTION_READER_LOST);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra(KEY_AUTH, AESGCMUtil.encryptData(encSecretKey, AESGCMUtil.randomString()));
        getApplicationContext().sendBroadcast(intent);
    }
}
