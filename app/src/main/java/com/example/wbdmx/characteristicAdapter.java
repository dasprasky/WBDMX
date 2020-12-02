package com.example.wbdmx;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class characteristicAdapter  extends ArrayAdapter<BluetoothGattCharacteristic> implements View.OnClickListener {
    private List<BluetoothGattCharacteristic> characteristics;
    private Context mContext;
    private Hashtable<UUID, String> uuid_names = new Hashtable<UUID, String>();
    private Hashtable<Integer, String> property_names = new Hashtable<Integer, String>();
    private onInfoButtonClickedListner infoListenerInterface;

    private static class ViewHolder{
        TextView characteristic_uuid;
        TextView characteristic_property;
        ImageView info;
    }
    public characteristicAdapter(List<BluetoothGattCharacteristic> characteristics,  Context m, onInfoButtonClickedListner listener) {
        super(m, R.layout.row_characteristic, characteristics);
        this.characteristics = characteristics;
        this.mContext = m;
        this.infoListenerInterface = listener;

        uuid_names.put(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"), "Device Name");
        uuid_names.put(UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"), "Appearance");
        uuid_names.put(UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"), "Peripheral Preferred Connection Parameter");
        uuid_names.put(UUID.fromString("00002aa6-0000-1000-8000-00805f9b34fb"), "Central Address Resolution");
        uuid_names.put(UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb"), "Service Changed");
        uuid_names.put(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), "Heart Rate Measurement");
        uuid_names.put(UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"), "Body Sensor Location");
        uuid_names.put(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), "Manufacturer Name String");
        uuid_names.put(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), "RX Characteristic");
        uuid_names.put(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"), "TX Characteristic");
        uuid_names.put(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"), "Firmware Revision String");
        uuid_names.put(UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb"), "RSC Measurement");
        uuid_names.put(UUID.fromString("00002a54-0000-1000-8000-00805f9b34fb"), "RSC Feature");
        uuid_names.put(UUID.fromString("00002a5d-0000-1000-8000-00805f9b34fb"), "Sensor Location");
        uuid_names.put(UUID.fromString("8ec90003-f315-4f60-9fb8-838830daea50"), "Buttonless DFU");

        property_names.put(2,"Read");
        property_names.put(8,"Write");
        property_names.put(4,"Write No Response");
        property_names.put(32,"Indicate");
        property_names.put(16,"Notify");

    }
    @Override
    public void onClick(View v){
        int position = (Integer)v.getTag();
        BluetoothGattCharacteristic characteristic = characteristics.get(position);
        switch (v.getId())
        {
            case R.id.create_csv:
               infoListenerInterface.onInfoButtonClicked(characteristic);
               break;
        }
    }


    public View getView(int position, View convertView, ViewGroup parent){
        BluetoothGattCharacteristic character = getItem(position);

        ViewHolder viewHolder;

        final View result;

        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_characteristic, parent, false);
            viewHolder.characteristic_uuid = (TextView) convertView.findViewById(R.id.characteristic_uuid);
            viewHolder.characteristic_property= (TextView) convertView.findViewById(R.id.characteristic_properties);
            viewHolder.info=(ImageView) convertView.findViewById(R.id.create_csv);

            result = convertView;
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)convertView.getTag();
            result = convertView;
        }
        String name = uuid_names.get(character.getUuid());
        viewHolder.characteristic_uuid.setText(name == null ? character.getUuid().toString(): name);

        String prop = getProperties(character.getProperties());
        viewHolder.characteristic_property.setText(prop);

        viewHolder.info.setOnClickListener(this);
        viewHolder.info.setTag(position);


        return convertView;
    }
    public String getProperties(Integer sum){

        HashSet<Integer> s = new HashSet<Integer>();
        Integer[] keys = property_names.keySet().toArray(new Integer[property_names.keySet().size()]);
        String prop = property_names.get(sum);
        for(int i =0 ; i<keys.length; i++){

            int temp = sum - keys[i];
            if(s.contains(temp)){
                prop = property_names.get(temp)+", "+ property_names.get(keys[i]);
            }
            s.add(keys[i]);
        }
        return prop;
    }
}
