package com.itsecrnd.rtnhceandroid;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

@SuppressLint("NewApi") // TODO remove
public class HCEService extends HostApduService {
    private static final String TAG = "CardService";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received broadcast in HCEService.");
            String action = intent.getAction();

            if (action != null && action.equals("com.itsecrnd.rtnhceandroid.sendresponseapdu")) {
                sendResponseApdu(new byte[] { (byte) 0xCC, (byte) 0x90, (byte) 0x00 });
            }
        }
    };

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        Intent intent = new Intent("com.itsecrnd.rtnhceandroid.sendcommandapdu");
        intent.putExtra("capdu", BinaryUtils.ByteArrayToHexString(command));
        getApplicationContext().sendBroadcast(intent);

        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting service");

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.itsecrnd.rtnhceandroid.sendresponseapdu");
        registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        // TODO check security around RECEIVER_EXPORTED - we only need to communicate inside app
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Finishing service: " + reason);

        unregisterReceiver(receiver);
    }
}
