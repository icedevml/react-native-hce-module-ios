package com.itsecrnd.rtnhceandroid;

import static com.itsecrnd.rtnhceandroid.IntentKeys.INTENT_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.INTENT_SEND_R_APDU;

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
    private static final String TAG = "CardService";

    private SecretKey encSecretKey;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received broadcast in HCEService.");
            String action = intent.getAction();

            if (action != null && action.equals(INTENT_SEND_R_APDU)) {
                String rapdu = AESGCMUtil.decryptData(encSecretKey, intent.getStringExtra("rapdu"));
                byte[] dec = BinaryUtils.HexStringToByteArray(rapdu.toUpperCase(Locale.ROOT));
                Log.i(TAG, "Sending rapdu: " + BinaryUtils.ByteArrayToHexString(dec));
                sendResponseApdu(dec);
                //sendResponseApdu(new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0x90, (byte) 0x00 });
            }
        }
    };

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        String capdu = AESGCMUtil.encryptData(encSecretKey, BinaryUtils.ByteArrayToHexString(command));

        Intent intent = new Intent(INTENT_RECEIVE_C_APDU);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra("capdu", capdu);
        getApplicationContext().sendBroadcast(intent);

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting service");

        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_SEND_R_APDU);

        String encKey = getApplicationContext()
                .getSharedPreferences("RTNHCEAndroidModuleBroadcastEnc", Context.MODE_PRIVATE)
                .getString("encKey", "");

        if (encKey.isEmpty()) {
            throw new RuntimeException("Failed to load encKey.");
        }

        byte[] bArr = BinaryUtils.HexStringToByteArray(encKey);
        encSecretKey = new SecretKeySpec(bArr, 0, bArr.length, "AES");

        /*
         * See a comment about registerReceiver() in RTNHCEAndroidModule.
         */
        getApplicationContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED);
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Finishing service: " + reason);

        getApplicationContext().unregisterReceiver(receiver);
    }
}
