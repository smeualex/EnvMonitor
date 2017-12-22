package com.smeualex.envmonitor;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by asm on 12/15/2017.
 *
 * Bluetooth Devices List Adapter
 */

public class BT_DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    static private class ViewHolderItem {
        private TextView deviceName;
        private TextView deviceAddr;
        private TextView deviceUUID;
    }

    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int mViewResourceId;

    BT_DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);

        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolderItem viewHolder;

        if(convertView == null) {
            convertView = mLayoutInflater.inflate(mViewResourceId, null);

            viewHolder = new ViewHolderItem();
            viewHolder.deviceName = convertView.findViewById(R.id.tvDeviceName);
            viewHolder.deviceAddr = convertView.findViewById(R.id.tvDeviceAddr);
            viewHolder.deviceUUID = convertView.findViewById(R.id.tvDeviceUUID);

            // save the view holder in the tag
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem)convertView.getTag();
        }

        BluetoothDevice device = mDevices.get(position);
        if(device != null){
            // update the text in the view holder
            if(viewHolder.deviceName != null)
                viewHolder.deviceName.setText(device.getName());

            if(viewHolder.deviceAddr != null)
                viewHolder.deviceAddr.setText(device.getAddress());

            if(viewHolder.deviceUUID != null) {
                // get the device UUIDs
                ParcelUuid[] deviceUUIDs = device.getUuids();
                int idx = 1;
                for (ParcelUuid uuid: deviceUUIDs) {
                    Log.d("BT_DEVICE_IST_ADAPTER", " >>> " + uuid.toString());
                    viewHolder.deviceUUID.append((idx++) + ". " + uuid.toString() + '\n');
                }
            }
        }

        return convertView;
    }
}
