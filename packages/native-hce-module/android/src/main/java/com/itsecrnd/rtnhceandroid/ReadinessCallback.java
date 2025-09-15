package com.itsecrnd.rtnhceandroid;

public interface ReadinessCallback {
    public void onSessionStarted();
    public void onRAPDU(String rapdu);
}
