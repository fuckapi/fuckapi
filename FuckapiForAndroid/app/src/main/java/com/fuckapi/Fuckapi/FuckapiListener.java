package com.fuckapi.Fuckapi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.UUID;

public class FuckapiListener implements BluetoothAdapter.LeScanCallback {

    public static final String TAG = "FuckapiListener";

    public static final String DEVICE_PREFIX = "AP-3x";
    public static final String DEVICE_ADDRESS = "EF:66:97:E0:7B:F3";
    public static final String UUID_ZEAL_SERVICE = "EBAD3530-226C-4ebb-A153-A3BC9567057D";
    public static final String UUID_ZEAL_NOTIFICATION_CHARACTERISTIC = "EBAD3531-226C-4ebb-A153-A3BC9567057D";
    public static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";

    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private BluetoothGatt mConnGatt;
    private int bleStatus;

    private Context mContext;

    public FuckapiListener(Context context) {
        mContext = context;

        Log.e(TAG, "initialize");
        // BLE check
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG, "BLE not supported");
            return;
        }

        // BT check
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Log.i(TAG, "BT unavalable");
            return;
        }

        // check BluetoothDevice
        if (mDevice == null) {
            mBTAdapter.startLeScan(this);
            mDevice = mBTAdapter.getRemoteDevice(DEVICE_ADDRESS);
            if (mDevice == null) {
                Log.i(TAG, "Device not found");
                return;
            }
        }
        mConnGatt = mDevice.connectGatt(context, false, mGattcallback);
        Log.i(TAG, "ready");
    }

    @Override
    public void onLeScan(final BluetoothDevice device, final int newRssi,
                         final byte[] newScanRecord) {
        Log.i(TAG, "name = " + device.getName() + ", address = " + device.getAddress());
        if (device.getName() == null) {
            return;
        }

        if (!device.getName().startsWith(DEVICE_PREFIX)) {
            return;
        }

        mDevice = device;
    }

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        public double beforeAccelZ = -1;
        private double lowPassX;
        private double lowPassY;
        private double lowPassZ;
        private double k = 0.5;

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite:" + descriptor.getValue()[0] + ", deviceName:" + gatt.getDevice().getName() + ", deviceAddress:" + gatt.getDevice().getAddress());
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleStatus = newState;
                mConnGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleStatus = newState;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = mConnGatt.getService(UUID.fromString(UUID_ZEAL_SERVICE));
            if (service == null) {
                Log.i(TAG, "not found service");
            } else {
                BluetoothGattCharacteristic c = service.getCharacteristic(UUID.fromString(UUID_ZEAL_NOTIFICATION_CHARACTERISTIC));
                boolean registered = mConnGatt.setCharacteristicNotification(c, true);

                BluetoothGattDescriptor descriptor = c.getDescriptor(
                        UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_CHARACTERISTIC));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                if (registered) {
                    Log.i(TAG, "ready");
                } else {
                    Log.i(TAG, "notice error");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] accelData = characteristic.getValue();
            double accelX = (accelData[0] << 8) + accelData[1];
            double accelY = (accelData[2] << 8) + accelData[3];
            double accelZ = (accelData[4] << 8) + accelData[5];

            if (beforeAccelZ == -1) {
                beforeAccelZ = accelZ;
                return;
            }
            
            lowPassX += (accelX - lowPassX) * k;
            lowPassY += (accelY - lowPassY) * k;
            lowPassZ += (accelZ - lowPassZ) * k;
            
            // High Pass Filter
            double rawAx = accelX - lowPassX;
            double rawAy = accelY - lowPassY;
            double rawAz = accelZ - lowPassZ;
            
            beforeAccelZ = accelZ;
            (( FuckapiHandler)mContext).received(rawAx, rawAy, rawAz);
        }
    };

    interface FuckapiHandler {
        void received(double x, double y, double z);
    }

}
