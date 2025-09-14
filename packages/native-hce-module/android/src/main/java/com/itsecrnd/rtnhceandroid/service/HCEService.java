package com.itsecrnd.rtnhceandroid.service;

import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_DETECT;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_READER_LOST;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_RECEIVE_C_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.ACTION_SEND_R_APDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_CAPDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.KEY_RAPDU;
import static com.itsecrnd.rtnhceandroid.IntentKeys.PERMISSION_HCE_BROADCAST;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.itsecrnd.rtnhceandroid.util.BinaryUtils;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class HCEService extends HostApduService {
    private static final String TAG = "HCEService";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                if (action != null && action.equals(ACTION_SEND_R_APDU)) {
                    String rapdu = intent.getStringExtra(KEY_RAPDU);

                    if (rapdu == null) {
                        throw new RuntimeException("Bug! ACTION_SEND_RAPDU intent KEY_RAPDU is null.");
                    }

                    byte[] dec = BinaryUtils.HexStringToByteArray(rapdu.toUpperCase(Locale.ROOT));
                    sendResponseApdu(dec);
                } else {
                    Log.w(TAG, "Received unknown action: " + action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle broadcast in HCEService.", e);
            }
        }
    };

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        String capdu = BinaryUtils.ByteArrayToHexString(command).toUpperCase(Locale.ROOT);

        Intent intent = new Intent(ACTION_RECEIVE_C_APDU);
        intent.setPackage(getApplicationContext().getPackageName());
        intent.putExtra(KEY_CAPDU, capdu);
        getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "HCEService onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_R_APDU);
        getApplicationContext().registerReceiver(
                receiver, filter, PERMISSION_HCE_BROADCAST, null, RECEIVER_EXPORTED);

        Intent intent = new Intent(ACTION_READER_DETECT);
        intent.setPackage(getApplicationContext().getPackageName());
        getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCEService onDeactivated: " + reason);

        getApplicationContext().unregisterReceiver(receiver);

        Intent intent = new Intent(ACTION_READER_LOST);
        intent.setPackage(getApplicationContext().getPackageName());
        getApplicationContext().sendBroadcast(intent, PERMISSION_HCE_BROADCAST);
    }
}
