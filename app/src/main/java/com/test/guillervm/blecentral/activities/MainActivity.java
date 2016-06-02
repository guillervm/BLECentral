package com.test.guillervm.blecentral.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.test.guillervm.blecentral.core.HexStringGenerator;
import com.test.guillervm.blecentral.core.Peripheral;
import com.test.guillervm.blecentral.recycler_aux.MyAdapter;
import com.test.guillervm.blecentral.R;
import com.test.guillervm.blecentral.recycler_aux.RecyclerItemClickListener;

import org.apache.http.util.ByteArrayBuffer;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    private static final int REQUEST_ENABLE_BT = 123;
    private static final UUID UUID_SERVICE = java.util.UUID.fromString("0000181C-0000-1000-8000-00805F9B34FB");
    private static final UUID UUID_CHARACTERISTIC = java.util.UUID.fromString("00002a19-0000-1000-8000-00805F9B34FB"); //BatteryLevel

    private static final long SCAN_PERIOD = 300000;
    private static final String TAG = "BluetoothInfo";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCallback mGattCallback;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private boolean mScanning = false;
    private Handler mHandler;
    private Runnable runnable;
    private int connected = -1;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private ArrayList<Peripheral> devices;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private MenuItem refreshButton;
    private MenuItem cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set interface.
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        // Improve performance if you know that changes in content do not change the layout size.
        recyclerView.setHasFixedSize(true);
        // Use a linear layout manager.
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // Necessary initializations.
        devices = new ArrayList<>();
        // Specify an adapter.
        adapter = new MyAdapter(devices, getApplicationContext());
        recyclerView.setAdapter(adapter);
        // Register interface events.
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(MainActivity.this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        if (connected == -1) {
                            if (connectToDevice(devices.get(position))) {
                                devices.get(position).setAvailable(-1);
                                adapter.notifyDataSetChanged();
                                connected = position;
                            }
                        }
                    }
                })
        );

        // Initialize Bluetooth adapter.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Initialization of the callback for ending scan.
        runnable = new Runnable() {
            @Override
            public void run() {
                stopScan(leScanCallback);
            }
        };

        mGattCallback = new BluetoothGattCallback() {
            @Override
            // Connection state changes.
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectionState = BluetoothProfile.STATE_CONNECTED;

                    Log.i(TAG, "Connected to GATT server.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.connected_to) + " " + devices.get(connected).getDevice().getAddress(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Discover available services.
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

                    int deviceState = 0;
                    if (mScanning) {
                        if (bluetoothManager.getConnectionState(devices.get(connected).getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED) {
                            deviceState = 1;
                        } else if (bluetoothManager.getConnectionState(devices.get(connected).getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
                                || bluetoothManager.getConnectionState(devices.get(connected).getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTING
                                || bluetoothManager.getConnectionState(devices.get(connected).getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTING) {
                            deviceState = -1;
                        }
                    }

                    // Close the connection to the GATT server.
                    mBluetoothGatt.close();

                    Log.i(TAG, "Disconnected from GATT server.");

                    final int assign = deviceState;
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Delayed update of the interface and connection variable.
                            devices.get(connected).setAvailable(assign);
                            connected = -1;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }, 500);
                }
            }

            @Override
            // New services discovered.
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "onServicesDiscovered received: " + status);

                    BluetoothGattService service = gatt.getService(UUID_SERVICE);
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC);
                        if (characteristic != null) {
                            // Write 32 random bytes to the peripheral device.
                            characteristic.setValue(HexStringGenerator.getInstance().generateString(32));
                            gatt.writeCharacteristic(characteristic);
                        }
                    }

                    // Disconnect from GATT server
                    gatt.disconnect();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume scanning.
        checkStateBluetooth();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop scanning if the application is not in the front.
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }

        if (mScanning) {
            stopScan(leScanCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        refreshButton = menu.findItem(R.id.action_refresh);
        cancelButton = menu.findItem(R.id.action_cancel);

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            checkStateBluetooth();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            checkStateBluetooth();

            return true;
        } else if (id == R.id.action_cancel) {
            stopScan(leScanCallback);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkStateBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (leScanCallback == null) {
                leScanCallback = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        Peripheral temp = new Peripheral(device, 1, scanRecord);
                        temp.setRssi(rssi);

                        if (bluetoothManager.getConnectionState(temp.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED) {
                            temp.setAvailable(1);
                        } else if (bluetoothManager.getConnectionState(temp.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
                                || bluetoothManager.getConnectionState(temp.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTING
                                || bluetoothManager.getConnectionState(temp.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTING) {
                            temp.setAvailable(-1);
                        } else {
                            temp.setAvailable(0);
                        }

                        if (!devices.contains(temp)) {
                            devices.add(temp);
                        } else {
                            int index = devices.indexOf(temp);
                            devices.get(index).setRssi(temp.getRssi());
                            devices.get(index).setAvailable(temp.isAvailable());
                        }

                        Collections.sort(devices);
                        adapter.notifyDataSetChanged();
                    }
                };
            }

            startScan(leScanCallback);
        }
    }

    private void startScan(BluetoothAdapter.LeScanCallback callback) {
        // Stops scanning after a pre-defined scan period.
        mHandler = new Handler();
        mHandler.postDelayed(runnable, SCAN_PERIOD);

        mScanning = true;
        if (refreshButton != null && cancelButton != null) {
            // Update interface.
            refreshButton.setVisible(false);
            cancelButton.setVisible(true);
        }

        // Start scanning.
        bluetoothAdapter.startLeScan(callback);
        Toast.makeText(this, R.string.start_scan, Toast.LENGTH_SHORT).show();
    }

    private void stopScan(BluetoothAdapter.LeScanCallback callback) {
        // Cancel callbacks if scheduled.
        mHandler.removeCallbacksAndMessages(null);

        mScanning = false;
        if (refreshButton != null && cancelButton != null) {
            // Update interface.
            cancelButton.setVisible(false);
            refreshButton.setVisible(true);
        }

        // Stop scanning.
        bluetoothAdapter.stopLeScan(callback);

        // Update peripherals state.
        for (Peripheral peripheral:devices) {
            peripheral.setAvailable(0);
        }
        adapter.notifyDataSetChanged();

        Toast.makeText(this, R.string.stop_scan, Toast.LENGTH_SHORT).show();
    }

    // Start a connection to a certain device.
    public boolean connectToDevice(Peripheral peripheral) {
        if (peripheral.isAvailable() > 0 && mConnectionState == BluetoothProfile.STATE_DISCONNECTED) {
            BluetoothDevice currentDevice = peripheral.getDevice();
            if (currentDevice != null
                    && bluetoothManager.getConnectionState(currentDevice, BluetoothProfile.GATT) != BluetoothProfile.STATE_CONNECTED
                    && bluetoothManager.getConnectionState(currentDevice, BluetoothProfile.GATT) != BluetoothProfile.STATE_CONNECTING
                    ) {
                // Connect to the GATT server.
                mBluetoothGatt = currentDevice.connectGatt(MainActivity.this.getApplicationContext(), false, mGattCallback);
            } else {
                return false;
            }

            return mBluetoothGatt != null;
        } else if (peripheral.isAvailable() < 0) {
            Toast.makeText(this, R.string.device_not_available, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.device_state_unknown, Toast.LENGTH_SHORT).show();
        }

        return false;
    }
}
