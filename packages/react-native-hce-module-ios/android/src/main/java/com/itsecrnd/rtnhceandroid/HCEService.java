package com.itsecrnd.rtnhceandroid;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class HCEService extends HostApduService {
    private static final String TAG = "CardService";

    public static final byte[] R_APDU_OK = BinaryUtils.HexStringToByteArray("0A9000");

    @Override
    public byte[] processCommandApdu(byte[] command, Bundle extras) {
        return R_APDU_OK;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting service");
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Finishing service: " + reason);
    }
}