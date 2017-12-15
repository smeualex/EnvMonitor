package com.smeualex.envmonitor;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.Manifest;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    NavigationView navigationView;
    MenuItem blueTooth_NAV;
    BluetoothAdapter mBluetoothAdapter; // alex: the BT adapter

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public BT_DeviceListaAdapter mDevicesListAdapter;
    ListView lvNewDevices;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver_BtChange = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        blueTooth_NAV.setIcon(R.drawable.ic_bluetooth_black_24dp);
                        blueTooth_NAV.setTitle(R.string.navBT_OFF);
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        blueTooth_NAV.setIcon(R.drawable.ic_bluetooth_disabled_black_24dp);
                        blueTooth_NAV.setTitle(R.string.navBT_ON);
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURNING OB");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: STATE TURNING OFF");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver_BT_ScanMode = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver_BT_ScanMode: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver_BT_ScanMode: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver_BT_ScanMode: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver_BT_ScanMode: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver_BT_ScanMode: Connected.");
                        break;
                }

            }
        }
    };

    /**
     * Broadcast Receiver for BT Device found
     */
    private final BroadcastReceiver mBroadcastReceiver_BT_DeviceFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "BT_DeviceFound - onReceive() - ACTION_FOUND");

            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                // get the device from the intent extra
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the device to the list
                mBTDevices.add(device);

                Log.d(TAG, "BT_DeviceFound - onReceive() - "
                        + device.getName() + ": " + device.getAddress());

                mDevicesListAdapter = new BT_DeviceListaAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDevicesListAdapter);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, " >>>> onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        blueTooth_NAV = navigationView.getMenu().findItem(R.id.nav_bluetoothOn);

        /* SET THE CORRECT ICON FROM THE START */
        blueTooth_NAV.setIcon(
                (mBluetoothAdapter.isEnabled())             ?
                        R.drawable.ic_bluetooth_black_24dp  :
                        R.drawable.ic_bluetooth_disabled_black_24dp
        );

        blueTooth_NAV.setTitle(
                (mBluetoothAdapter.isEnabled()) ?
                        R.string.navBT_OFF       :
                        R.string.navBT_ON
        );

        lvNewDevices = findViewById(R.id.lvNewDevices);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, " >>>> onDestroy()");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver_BtChange);
        unregisterReceiver(mBroadcastReceiver_BT_ScanMode);
        unregisterReceiver(mBroadcastReceiver_BT_DeviceFound);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d(TAG, " >>>> onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, " >>>> onOptionsItemSelected()");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_bluetoothOn) {
            Log.d(TAG, "onNavigationItemSelected() - BT Enable/Disable clicked");
            enableDisableBluetooth(item);
        } else if (id == R.id.nav_btDiscoverable) {
            Log.d(TAG, "onNavigationItemSelected() - BT Enable/Disable discoverable");
            enableDisableDiscoverable();
        } else if (id == R.id.nav_btDiscover) {
            Log.d(TAG, "onNavigationItemSelected() - BT Discover Devices...");
            discoverBTDevices();
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void discoverBTDevices() {
        Log.d(TAG, "dicoverBTDevices() - looking for unpaired devices");

        // if BT is discovering already... cancel first
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "discoverBTDevices() - Cancelling BT discovery;");
        }
        // for Android > LOLLIPOP => Check BT discover permission is required
        checkBTPermissions();

        // start discovery and setup the BroadcastReceiver
        mBluetoothAdapter.startDiscovery();
        IntentFilter discoverBTDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver_BT_DeviceFound, discoverBTDevicesIntent);
    }

    @TargetApi(23)
    private void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                        1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    public void enableDisableBluetooth(MenuItem item) {
        // NO BLUETOOTH
        if (mBluetoothAdapter == null) {
            Log.e(TAG, getString(R.string.errorNoBT));
        }

        // BLUETOOTH IS DISABLED    ==>     enable it
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            /* use broadcast received to intercept the BT state changes to log them */
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver_BtChange, BTIntent);
        }

        // BLUETOOTH IS ENABLED     ==>     disable id
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver_BtChange, BTIntent);
        }

    }

    public void enableDisableDiscoverable(){
        Log.d(TAG, "enableDisableDiscoverable: Making device discoverable for 300 seconds.");

        /** REQUEST DISCOVERABLE for 300s           */
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        /** Broadcast receiver for the SCAN MODE    */
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver_BT_ScanMode,intentFilter);
    }
}
