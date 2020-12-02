package com.example.wbdmx;

import android.bluetooth.BluetoothGattCharacteristic;
import android.view.View;

public interface onInfoButtonClickedListner {
    public void onInfoButtonClicked(BluetoothGattCharacteristic characteristic);
}
