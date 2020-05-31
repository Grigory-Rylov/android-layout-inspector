package com.android.ddmlib;

public class ClientsListenerSetter {
    public static void setClientsListener(IDevice device, ClientsChangedListener listener) {
        if (device instanceof Device) {
            ((Device) device).setClientsChangedListener(listener);
        }
    }
}
