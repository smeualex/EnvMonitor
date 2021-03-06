package com.smeualex.envmonitor;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
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
import android.widget.AdapterView;
import android.widget.ListView;

import com.smeualex.envmonitor.util.Util;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener {

    // activity codes for onActivityResult()
    private static final int ACTIVITY_CODE_TURN_BT_ON = 1;

    private static final String TAG = "MainActivity";
    private static final int DISCOVERABLE_TIME = 120;

    CountDownTimer discoverableTimer;
    Boolean isDiscoverable;

    NavigationView navigationView;
    MenuItem blueTooth_NAV;
    MenuItem blueTooth_Discovery_NAV;
    BluetoothAdapter mBluetoothAdapter; // alex: the BT adapter

    DrawerLayout drawer;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public BT_DeviceListAdapter mDevicesListAdapter;
    ListView lvNewDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, " >>>> onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        /* BROADCAST RECEIVERS */
        // Intent Filter to get the BOND BT notifications
        IntentFilter btBondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver_BT_Bonded, btBondFilter);
        //
        IntentFilter discoverBTDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver_BT_DeviceFound, discoverBTDevicesIntent);
        /* use broadcast received to intercept the BT state changes to log them */
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver_BtChange, BTIntent);
        /* Broadcast receiver for the SCAN MODE    */
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver_BT_ScanMode,intentFilter);

        // BLUETOOTH ADAPTER
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null){
            //Show a mensag. that the device has no bluetooth adapter
            Util.msgSnack(drawer, "Bluetooth Device Not Available");
            //finish apk
            finish();
        }
        else if(!mBluetoothAdapter.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Util.msgSnack(drawer, "Turning on Bluetooth");
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, ACTIVITY_CODE_TURN_BT_ON);
        }

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        blueTooth_NAV = navigationView.getMenu().findItem(R.id.nav_bluetoothOn);
        blueTooth_Discovery_NAV = navigationView.getMenu().findItem(R.id.nav_btDiscoverable);

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
        // SET THE ON CLICK LISTENER on the new devices list //
        lvNewDevices.setOnItemClickListener(MainActivity.this);


        isDiscoverable = false;
        discoverableTimer = new CountDownTimer(DISCOVERABLE_TIME * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, " >>> TIMER: Device discoverable for " + (millisUntilFinished / 1000) + "s");
                blueTooth_Discovery_NAV.setTitle("Discoverable for "
                        + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                isDiscoverable = false;
                Log.d(TAG, " >>> TIMER: finished");
                blueTooth_Discovery_NAV.setTitle("Make Discoverable...");
            }
        };
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, " >>>> onDestroy()");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver_BtChange);
        unregisterReceiver(mBroadcastReceiver_BT_ScanMode);
        unregisterReceiver(mBroadcastReceiver_BT_DeviceFound);
        unregisterReceiver(mBroadcastReceiver_BT_Bonded);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(TAG, "onActivityResult(): requestCode = " + requestCode + "; " +
                                             "resultCode = " + resultCode + ";");
        switch(requestCode){
            case ACTIVITY_CODE_TURN_BT_ON:
                if(resultCode == RESULT_OK){
                    Log.d(TAG, "onActivityResult():         BT Turned on - OK!");
                }
                break;
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver_BtChange = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null &&
                    action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

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

            if (action != null &&
                    action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

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
     * Broadcast Receiver for changes to the BONDED state
     */
    private final BroadcastReceiver mBroadcastReceiver_BT_Bonded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null &&
                    action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch(device.getBondState()){
                    // already bonded
                    case BluetoothDevice.BOND_BONDED:
                        Log.d(TAG, "mBroadcastReceiver_BT_Bonded: BOND_BOND");
                        break;

                    // creating a bond
                    case BluetoothDevice.BOND_BONDING:
                        Log.d(TAG, "mBroadcastReceiver_BT_Bonded: BOND_BONDING");
                        break;

                    // breaking a bond
                    case BluetoothDevice.BOND_NONE:
                        Log.d(TAG, "mBroadcastReceiver_BT_Bonded: BOND_NONE");
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

            if (action != null &&
                    action.equals(BluetoothDevice.ACTION_FOUND)) {
                // get the device from the intent extra
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the device to the list
                mBTDevices.add(device);

                Log.d(TAG, "BT_DeviceFound - onReceive() - "
                        + device.getName() + ": " + device.getAddress());

                mDevicesListAdapter = new BT_DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDevicesListAdapter);
            }
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_bluetoothOn) {
            Log.d(TAG, "onNavigationItemSelected() - BT Enable/Disable clicked");
            enableDisableBluetooth(item);
        } else if (id == R.id.nav_btDiscoverable) {
            Log.d(TAG, " >>>>>>> onNavigationItemSelected() - BT Enable/Disable discoverable");
            enableDisableDiscoverable();
        } else if (id == R.id.nav_btDiscover) {
            Log.d(TAG, "onNavigationItemSelected() - BT Discover Devices...");
            discoverBTDevices();
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
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
        }

        // BLUETOOTH IS ENABLED     ==>     disable id
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }

    }

    public void enableDisableDiscoverable(){
        Log.d(TAG, "enableDisableDiscoverable: Making device discoverable for 300 seconds.");
        /* REQUEST DISCOVERABLE for 120s           */
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_TIME);
            startActivity(discoverableIntent);
        }

        if (!isDiscoverable) {
            isDiscoverable = true;
            discoverableTimer.start();
        }
    }

    /// on click for the device list
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // first cancel BT discovery => very memory intensive
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick(): You clicked a Bluetooth device!");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddr = mBTDevices.get(i).getAddress();
        Log.d(TAG, "onItemClick():      >>>>>> " + deviceName);
        Log.d(TAG, "onItemClick():      >>>>>> " + deviceAddr);

        // create the bond
        // NOTE: requires API lvl > 18
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Pairing with device: " + deviceName + "[" + deviceAddr + "]");
            mBTDevices.get(i).createBond();
        }
    }
}
