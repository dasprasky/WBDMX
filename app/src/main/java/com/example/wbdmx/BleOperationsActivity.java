package com.example.wbdmx;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BleOperationsActivity extends AppCompatActivity {
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private List<BluetoothGattService> services;
    private static final int GATT_MAX_MTU_SIZE = 517;
    ListView listView;
    private characteristicAdapter charAdapter;
    private boolean enable = false;
    Date dNow;
    SimpleDateFormat timeStamp =
            new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
    private String csv;
    private static final UUID TX_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device == null) Toast.makeText(getApplicationContext(), "Bluetooth device missing. Disconnect and try again! " , Toast.LENGTH_SHORT).show();
        else Toast.makeText(getApplicationContext(), "Connecting to device " +device.getAddress(), Toast.LENGTH_SHORT).show();
        gatt = device.connectGatt(this, false, gattCallback);
        setContentView(R.layout.activity_ble_operations);

        if(gatt==null)Toast.makeText(getApplicationContext(), "Could not connect to device. Disconnect and try again! " +device.getAddress(), Toast.LENGTH_LONG).show();

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothGattCharacteristic characteristic = characteristics.get(position);
                if(isCharacteristicReadable(characteristic)){
                    gatt.readCharacteristic(characteristic);

                }
                if(isCharacteristicWritable(characteristic)){

                }
                if(isCharacteristicWritableWithoutResponse(characteristic)){

                }
                if(isCharacteristicNotifiable(characteristic)){
                    //setNotifications(characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                }
            }
        });

        Button disconnect_button = (Button)findViewById(R.id.disconnect_button);
        disconnect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gatt.disconnect();
                finish(); //finishes current activity and falls back to MainActivity
            }
        });
    }

    private onInfoButtonClickedListner listener = new onInfoButtonClickedListner() {
        @Override
        public void onInfoButtonClicked(BluetoothGattCharacteristic characteristic) {

            if(isCharacteristicNotifiable(characteristic) && !enable){
                setNotifications(characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                enable = true;
                try{
                     dNow= new Date( );
                     String filename = "/data_"+timeStamp.format(dNow.getTime())+".csv";
                    csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + filename);
                    File res = new File(csv);

                    Log.w("onInfo","new file created");
                }catch (Exception e){
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Starting Log",Toast.LENGTH_LONG).show();
                    }
                });

            }
            else if(isCharacteristicNotifiable(characteristic) && enable){
                setNotifications(characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, false);
                enable = false;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Ending Log",Toast.LENGTH_LONG).show();
                    }
                });

            }
        }
    };

    public void setNotifications(BluetoothGattCharacteristic characteristic, byte[] payload, boolean enable){
        String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
        UUID cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
        if(descriptor == null){
            Log.e("setNotificaiton", "Could not get CCC descriptor for characteristic "+ characteristic.getUuid());
        }
        if(!gatt.setCharacteristicNotification(descriptor.getCharacteristic(), enable)){
            Log.e("setNotification", "setCharacteristicNotification failed");
        }
        descriptor.setValue(payload);
        boolean result = gatt.writeDescriptor(descriptor);
        if(!result){
            Log.e("setNotification", "writeDescriptor failed for descriptor");

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Descriptor failed! Device may not be connected. Try again!",Toast.LENGTH_LONG).show();
                }
            });
            gatt.disconnect();
            finish();
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(status == gatt.GATT_SUCCESS){
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    Log.w("BluetoothGattCallback", "Successfully connected to device " + gatt.getDevice().getAddress());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            boolean ans = gatt.discoverServices();
                            Log.d("onConnectionStateChange", "Discover Services started: " + ans);
                            gatt.requestMtu(GATT_MAX_MTU_SIZE);
                        }
                    });
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    Log.w("BluetoothGattCallback", "Succesfully disconnected form device "+ gatt.getDevice().getAddress());
                    gatt.close();
                }
                else{
                    Log.w("BluetoothGattCallback", "Error "+status+" encountered for "+gatt.getDevice().getAddress()+ "\nDisconnecting...");
                    gatt.close();
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            super.onServicesDiscovered(gatt, status);
            services = gatt.getServices();
            Log.w("BluetoothGattCallback", "Discovered "+ services.size()+" for "+gatt.getDevice().getAddress());

            characteristics.addAll(services.stream().flatMap(s -> s.getCharacteristics().stream())
                    .collect(Collectors.toList()));

            Log.w("onServicesDiscovered", "size = "+characteristics.size());
            printGattTable(services);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    charAdapter = new characteristicAdapter(characteristics, getApplicationContext(), listener);
                    listView.setAdapter(charAdapter);
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){
            boolean event = status ==BluetoothGatt.GATT_SUCCESS;
            Log.w("onMtuChanged", "ATT MTU changed to "+mtu+" "+ event);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.i("BluetoothGattCallback", "Read characteristic success for "+ characteristic.getUuid().toString() +" value: "+ new String(characteristic.getValue(), StandardCharsets.UTF_8));
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Read success: "+ new String(characteristic.getValue(), StandardCharsets.UTF_8),Toast.LENGTH_LONG).show();
                    }
                });

            }
            else if(status == BluetoothGatt.GATT_READ_NOT_PERMITTED){
                Log.i("BluetoothGattCallback", "Read not permitted for  "+ characteristic.getUuid().toString());

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Read not permitted!",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                Log.i("BluetoothGattCallback", "Characteristic read failed for  "+ characteristic.getUuid().toString());

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Read failed! Device may not be connected. Try again!",Toast.LENGTH_LONG).show();
                    }
                });
                gatt.disconnect();
                finish();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic){
            byte[] value = characteristic.getValue();
            //Log.i("onCharacteristicChanged",  value.toString()+ '\n' +Integer.toString(value.length));
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(characteristic, value);

        }
    };
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic, byte[] value){
       if(isCharacteristicNotifiable(characteristic)){
           String value_str =  bytesToHex(value);
           if(characteristic.getUuid().equals(TX_CHARACTERISTIC)){
              value_str =  bytestoformat(characteristic.getValue());
           }
           String[] line = value_str.split(" ");
           Log.i("broadcastUpdate",  value_str);
           try {
               CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
               writer.writeNext(line);
               writer.close();

           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }
    /**
     * Convert bytes into hex string.
     */
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if ((bytes == null) || (bytes.length <= 0)) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 3 - 1];

        for (int j=0; j<bytes.length; ++j) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j < bytes.length - 1) {
                hexChars[j * 3 + 2] = 0x20;           // hard coded space
            }
        }

        return new String(hexChars);
    }

    public static String bytestoformat(byte[] bytes){
        String number="";
        for(int j = 2; j<bytes.length-1; j++){
            int result=0;
            if(j%2!=0 ) continue;
            result = (bytes[j] & 0xff) |
                    ((bytes[j+1] & 0xff) << 8);
            if(j<bytes.length-7) // bytes 2 to 14 are unsigned
                number = number + Integer.toString(result)+' ';
            else{ // bytes 15 onwards carries signed value
                short signed = (short) result;
                number = number +Short.toString(signed)+' ';
            }
        }
        return number;
    }

    private void printGattTable(List<BluetoothGattService> services){
        if(services.isEmpty()){
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
            return;
        }
        Iterator<BluetoothGattService> it = services.iterator();
        while(it.hasNext()){
            BluetoothGattService service = it.next();
            Log.i("printGattTable", "Service "+ service.getUuid());
            for(int i = 0; i<service.getCharacteristics().size(); i++){
                Log.i("printGattTable","Characteristic UUID: "+ service.getCharacteristics().get(i).getUuid());
            }
        }
    }

    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE )) != 0;
    }

    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }


    public boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    public boolean isCharacteristicWritableWithoutResponse(BluetoothGattCharacteristic pChar){
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    public boolean isCharacteristicIndicatable(BluetoothGattCharacteristic pChar){
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0;
    }

}
