package uk.ac.openlab.cryptocam.utility;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class BLE {

    //todo move these into a configuration file / class.
    final static String deviceName = "CryptoCam";
    final UUID serviceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");
    final UUID characteristicUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");

    //todo update this later to use the uuids.
    static boolean isCryptoCam(BluetoothDevice device){
         return (device.getName() != null && device.getName().equals(deviceName));
    }


    private static String TAG = "BLE";
    private BluetoothAdapter mBluetoothAdapter;
    private BLEBroadcastReceiver mReceiver;
    private IntentFilter discoveryIntents;

    private Context mContext;

    private ArrayList<BluetoothDevice> devices;

    public BLE(Context context){
        mContext = context;
    }


    public void init(){
        if(mContext == null) {
            throw new ExceptionInInitializerError("Context not initialised");
        }


        devices = new ArrayList<>();
        mReceiver = new BLEBroadcastReceiver();

        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        discoveryIntents = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoveryIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        discoveryIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        discoveryIntents.addAction(BluetoothDevice.ACTION_FOUND);


        checkBondedDevices();
    }

    private void checkBondedDevices() {
        if(mBluetoothAdapter!=null) {
            Set<BluetoothDevice> previousDevices = mBluetoothAdapter.getBondedDevices();
            if (previousDevices != null) {
                for (BluetoothDevice device : previousDevices) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED && isCryptoCam(device)) {
                        devices.add(device);
                    }
                }
            }
        }
    }

    public void start(){
        if(mBluetoothAdapter!=null) {
            mContext.registerReceiver(mReceiver, discoveryIntents);
            mBluetoothAdapter.startDiscovery();
        }
    }

    public void stop(){
        if(mBluetoothAdapter!=null) {
            mBluetoothAdapter.cancelDiscovery();
            mContext.unregisterReceiver(mReceiver);

            for(BluetoothDevice device:devices){
                device.connectGatt(mContext,true,characteristicCallback).discoverServices();
            }
        }



    }

    public boolean isScanning(){
        if(mBluetoothAdapter!=null){
            return mBluetoothAdapter.isDiscovering();
        }
        return false;
    }

    private android.bluetooth.BluetoothGattCallback characteristicCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG,"Discover services");

            BluetoothGattService service = gatt.getService(serviceUUID);
            gatt.readCharacteristic(service.getCharacteristic(characteristicUUID));

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if(characteristic.getUuid() == characteristicUUID){
                Log.d(TAG,"READ CHARACTERISTIC:"+ characteristic.toString());

                try {
                    CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(characteristic.toString()));
                    Log.d(TAG,String.format("key: %s, url: %s, reconnect: %d",packet.key,packet.url,packet.reconnectIn));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(characteristic.getUuid() == characteristicUUID){
                Log.d(TAG,"READ CHARACTERISTIC:"+ characteristic.toString());

                try {
                    CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(characteristic.toString()));
                    Log.d(TAG,String.format("key: %s, url: %s, reconnect: %d",packet.key,packet.url,packet.reconnectIn));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


    };


    public class BLEBroadcastReceiver extends BroadcastReceiver{



        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG,BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    Log.d(TAG,BluetoothAdapter.ACTION_STATE_CHANGED);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG,BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    mContext.unregisterReceiver(mReceiver);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Log.d(TAG,BluetoothDevice.ACTION_FOUND);
                    Bundle extras = intent.getExtras();
                    if(extras!=null) {
                        bondWithDevice((BluetoothDevice)extras.getParcelable(BluetoothDevice.EXTRA_DEVICE), deviceName);
                    }
                    break;
            }
        }


//        public void bondWithDevice(BluetoothDevice device, UUID targetUUID){
//            ParcelUuid[] uuids = device.getUuids();
//
//            if(uuids == null || uuids.length == 0)
//                return;
//
//            for(ParcelUuid uuid : uuids){
//                if (uuid.getUuid() == targetUUID){
//                    if(device.getBondState() == BluetoothDevice.BOND_NONE) {
//                        device.createBond();
//                        Log.d(TAG, "BONDING: " + device.getName());
//                    }else{
//                        Log.d(TAG, "BOND STATE: " + device.getBondState());
//                    }
//                }
//            }
//
//        }

        public void bondWithDevice(BluetoothDevice device, String targetDevice){
                 if (device == null)
                     return;

                if (isCryptoCam(device)){
                    if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                        device.createBond();
                        Log.d(TAG, "BONDING: " + device.getName());
                    }else{
                        Log.d(TAG, "BOND STATE: " + device.getBondState());
                    }

                    //todo revist this logic to allow multiple devices to be added.
                    devices.add(device); //currently pointless as i only identify the first cam.
                    stop(); //will stop after finding the first device.

                }
        }
    }



}
