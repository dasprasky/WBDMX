package com.example.wbdmx;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;

import java.util.ArrayList;

import java.util.List;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner bleScanner=null;
    private ScanSettings scanSettings;
    private ScanCallback scanCallback;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private boolean isScanning = false;
    private ArrayList<ScanResult> scanResult = new ArrayList<>();
    private scanResultAdapter scanAdapter;
    private RecyclerView recyclerView;
    private Button scan_btn;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        scanAdapter = new scanResultAdapter(scanResult, new CustomItemClickListner() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onItemClick(View v, int position) {
                if(isScanning){
                    stopScan();
                }
                final BluetoothDevice device = scanResult.get(position).getDevice();
                Operations(v, device);
            }
        });
        recyclerView.setAdapter(scanAdapter);

        //button so that we can change its text when clicked
        scan_btn = (Button) findViewById(R.id.search_button);
    }
    public void Operations(View view, BluetoothDevice device){
        Intent intent = new Intent(this, SuggestedUiActivity.class);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSIONS);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startScan(){
        //response to the scan button
        bleScanner = mBluetoothAdapter.getBluetoothLeScanner();
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanResult.clear();
        scanAdapter.notifyDataSetChanged();
        scanCallback = new ScanCallback(){
            @Override
            public void onScanResult(int callbackType, ScanResult result){
                ListIterator<ScanResult> item_iterator = scanResult.listIterator();
                boolean flag = true;
                while(item_iterator.hasNext()){
                    ScanResult item = item_iterator.next();
                    if(item.getDevice().getAddress().equals(result.getDevice().getAddress())){
                        flag = false;
                   }
                }
                if(flag) {
                    Log.i("ScanCallback", "Found unique BLE device! Name : " + result.getDevice().getName() + " address: " +result.getDevice().getAddress());
                    scanResult.add(result);
                    scanAdapter.notifyItemInserted(scanResult.size()-1); }
            }

            @Override
            public void onScanFailed(int errorCode){
                Log.e("ScanCallback", "onScanFailed: code " + errorCode);
            }
        };
        scanResult.clear();
        bleScanner.startScan(null, scanSettings, scanCallback);
        isScanning = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScan(){
        bleScanner.stopScan(scanCallback);
        isScanning = false;
        scan_btn.setText("SEARCH FOR DEVICES");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void button_function(View v) {
        if(isScanning){
            stopScan();

        }
        else{
            startScan();
            scan_btn.setText("STOP SEARCH");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();

        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();

        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
    }

}