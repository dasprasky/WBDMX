package com.example.wbdmx;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SuggestedUiActivity extends AppCompatActivity {
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private List<BluetoothGattService> services;
    private static final int GATT_MAX_MTU_SIZE = 517;
    private boolean enable = false;
    Date dNow;
    SimpleDateFormat timeStamp =
            new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
    private String hr_csv;
    private String tx_csv;
    private long startTime;
    private long startTime_nano;
    private static final UUID TX_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_MEASURE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suggested_ui);

        Intent intent = getIntent();
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device == null) Toast.makeText(getApplicationContext(), "Bluetooth device missing. Disconnect and try again! " , Toast.LENGTH_SHORT).show();
        else Toast.makeText(getApplicationContext(), "Connecting to device " +device.getAddress(), Toast.LENGTH_SHORT).show();
        gatt = device.connectGatt(this, false, gattCallback);


        Button disconnect_button = (Button)findViewById(R.id.connection);
        disconnect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gatt.disconnect();
                finish(); //finishes current activity and falls back to MainActivity
            }
        });
        Button record_button = (Button)findViewById(R.id.recording);
        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = getCharPosition(TX_CHARACTERISTIC);
                if(i!=-1)
                    record(characteristics.get(i));
            }
        });
    }
    public int getCharPosition(UUID uuid){
        int i = -1;
        for(int j = 0; j<characteristics.size();j++){
            if(characteristics.get(j).getUuid().equals(uuid)){
                i=j;
                break;
            }
        }
        return i;
    }
    public void record(BluetoothGattCharacteristic characteristic){
        if(isCharacteristicNotifiable(characteristic)&&!enable){
            if(characteristic.getUuid()!=HEART_RATE_MEASURE) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setNotifications(characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                    }
                }, 1500);
            }
            enable = true;

            try{
                dNow= new Date( );
                String folder =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/hrm";
                File directory = new File(folder);
                if(!directory.exists()){
                    directory.mkdir();
                }
                String hr_filename = "/hrm/hr_"+timeStamp.format(dNow.getTime())+".csv";
                hr_csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + hr_filename);
                new File(hr_csv);

                folder =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/tx";
                directory = new File(folder);
                if(!directory.exists()){
                    directory.mkdir();
                }
                String tx_filename = "/tx/tx_"+timeStamp.format(dNow.getTime())+".csv";
                tx_csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + tx_filename);
                new File(tx_csv);

                Log.w("onInfo","new files created");
            }catch (Exception e){
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Starting Log",Toast.LENGTH_LONG).show();
                    Button record_button = (Button)findViewById(R.id.recording);
                    record_button.setText("STOP RECORDING");
                }
            });
            startTime = System.currentTimeMillis();
            startTime_nano = System.nanoTime();

        }
        else if(isCharacteristicNotifiable(characteristic) && enable){
            setNotifications(characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, false);
            enable = false;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Ending Log",Toast.LENGTH_LONG).show();
                    Button record_button = (Button)findViewById(R.id.recording);
                    record_button.setText("START RECORDING");
                }
            });
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
                    Log.w("BluetoothGattCallback", "Successfully disconnected form device "+ gatt.getDevice().getAddress());
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
            displayUI(characteristics);

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
                if(characteristic.getUuid().equals(DEVICE_NAME)) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            TextView device_name = (TextView) findViewById(R.id.device_value);
                            device_name.setText(new String(characteristic.getValue(), StandardCharsets.UTF_8));
                        }
                    });
                }
            }
            else if(status == BluetoothGatt.GATT_READ_NOT_PERMITTED){
                Log.i("BluetoothGattCallback", "Read not permitted for  "+ characteristic.getUuid().toString());
            }
            else{
                Log.i("BluetoothGattCallback", "Characteristic read failed for  "+ characteristic.getUuid().toString());
                gatt.disconnect();
                finish();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic){
            byte[] value = characteristic.getValue();
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(characteristic, value);

        }
    };
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic, byte[] value){
        if(characteristic.getUuid().equals(HEART_RATE_MEASURE)){
            String value_str =  bytesToHex(value);
            String[] line = value_str.split(" ");
            int heart_rate =Integer.parseInt(line[1], 16);
            TextView hr = (TextView)findViewById(R.id.heart_rate_value);
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    hr.setText(Integer.toString(heart_rate));
                }
            });
            TextView rri = (TextView) findViewById(R.id.rri_value);
            if(line.length>2) {
                float rri_value = (float)Integer.parseInt(line[3] + line[2], 16) * 1000 / 1024;
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        rri.setText(String.format("%.2f", rri_value));
                    }
                });
            } else{
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        rri.setText("-");
                    }
                });}
        }
        if(enable==true){
            SimpleDateFormat format = new SimpleDateFormat ("HH.mm.ss.SSS");
            Date d = new Date();

            if(characteristic.getUuid().equals(TX_CHARACTERISTIC)){
                byte[] char_value = characteristic.getValue();
                boolean check_sum = checksum(char_value);
                Log.i("broadcastUpdate/checksm", Boolean.toString(check_sum));
                List<String> processed_line = new ArrayList<String>();
                if(!check_sum) processed_line.add("***Checksum mismatch**");
                String value_str =  bytestoformat(char_value);

                processed_line.add(format.format(d.getTime()));
                processed_line.add(Long.toString(System.currentTimeMillis() - startTime));
                processed_line.add(Long.toString(System.nanoTime() - startTime_nano));
                processed_line.addAll(Arrays.asList(value_str.split(" ")));
                String[] final_line = new String[processed_line.size()];
                processed_line.toArray(final_line);
                csv_writer(final_line, tx_csv);
                Log.i("broadcastUpdate/tx_char",  value_str);
            }
            else{
                String value_str =  bytesToHex(value);
                List<String> processed_line = new ArrayList<String>();
                processed_line.add(format.format(d.getTime()));
                processed_line.add(Long.toString(System.currentTimeMillis() - startTime));
                processed_line.add(Long.toString(System.nanoTime() - startTime_nano));
                String[] line = value_str.split(" ");
                int heart_rate =Integer.parseInt(line[1], 16);
                float rri_value = 0.0f;
                if(line.length>2) {
                    rri_value = (float)Integer.parseInt(line[3] + line[2], 16) * 1000 / 1024;
                }
                processed_line.add(Integer.toString(heart_rate));
                processed_line.add(Float.toString(rri_value));

                String[] final_line = new String[processed_line.size()];
                processed_line.toArray(final_line);
                csv_writer(final_line, hr_csv);
                Log.i("broadcastUpdate/hr",  value_str);
            }
        }
    }
    public void csv_writer(String[] line, String csv){
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
            writer.writeNext(line, false);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean checksum(byte[] value){
        int checksum;
        String all_values = bytesToHex(value);
        String[] all_values_arr = all_values.split(" ");
        checksum = Integer.parseInt(all_values_arr[1], 16);
        String[] sensor_values = bytestoformat(value).split(" ");
        int xor = 0;
        for (int i = 0; i<all_values_arr.length;i++){
            if(i<2) continue;
            xor ^= Integer.parseInt(all_values_arr[i], 16);
        }
        if(xor == checksum) return true;

        return false;
    }

    private void displayUI(List<BluetoothGattCharacteristic> characteristics){
        if(characteristics.isEmpty()){
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
            return;
        }
        Iterator<BluetoothGattCharacteristic> it = characteristics.iterator();
        while(it.hasNext()){
            BluetoothGattCharacteristic character = it.next();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(character.getUuid().equals(DEVICE_NAME)){
                        gatt.readCharacteristic(character);
                    }
                }
            }, 1000);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(character.getUuid().equals(HEART_RATE_MEASURE)){
                        setNotifications(character, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                    }
                }
            }, 500);
        }
    }
    public void setNotifications(BluetoothGattCharacteristic characteristic, byte[] payload, boolean enable){
        String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
        UUID cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
        if(descriptor == null){
            Log.e("setNotification", "Could not get CCC descriptor for characteristic "+ characteristic.getUuid());
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

    public boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

}