package com.itsecrnd.rtnhceandroid;

public interface HCEServiceCallback {
    void onBackgroundHCEInit();
    void onRespondAPDU(String rapdu);
}
