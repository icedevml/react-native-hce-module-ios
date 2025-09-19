package com.itsecrnd.rtnhceandroid;

public interface HCEServiceCallback {
    void onBackgroundHCEInit(String handle);
    void onBackgroundHCEFinish(String handle);
    void onRespondAPDU(String handle, String rapdu);
}
