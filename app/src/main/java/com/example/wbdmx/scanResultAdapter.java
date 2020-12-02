package com.example.wbdmx;


import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class scanResultAdapter extends RecyclerView.Adapter<scanResultAdapter.ViewHolder> {
    private List<ScanResult> scanResult;

    private CustomItemClickListner listner;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            deviceName = view.findViewById(R.id.device_name);
            deviceAddress = view.findViewById(R.id.device_address);
            deviceRssi = view.findViewById(R.id.device_rssi);
        }

    }

    public scanResultAdapter(List<ScanResult> dataSet, CustomItemClickListner listner) {
        scanResult = dataSet;
        this.listner = listner;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.listitem_device, viewGroup, false);
        final ViewHolder mViewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listner.onItemClick(v, mViewHolder.getAdapterPosition());
            }
        });

        return mViewHolder;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.deviceName.setText(scanResult.get(position).getDevice().getName() == null ? "Unknown": scanResult.get(position).getDevice().getName());
        viewHolder.deviceRssi.setText(String.valueOf(scanResult.get(position).getRssi()));
        viewHolder.deviceAddress.setText(scanResult.get(position).getDevice().getAddress());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (scanResult == null) return 0;

        return scanResult.size();
    }
}
