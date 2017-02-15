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
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import uk.ac.openlab.cryptocam.data.Cam;
import uk.ac.openlab.cryptocam.data.Video;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class BLE {

    //todo move these into a configuration file / class.
    final static String deviceName = "gw-";
    final UUID serviceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");
    final UUID characteristicUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");
    long reconnectInterval = 300000;

    //todo update this later to use the uuids.
    static boolean isCryptoCam(BluetoothDevice device){
         return (device.getName() != null && device.getName().toLowerCase().startsWith(deviceName));
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
                        addDevice(device);
                    }
                }
            }
        }
    }


    private void addDevice(BluetoothDevice device){
        devices.add(device);
        if(!Cam.exists(device.getAddress()))
        {
            long id = new Cam(device.getName().toLowerCase(),device.getAddress().toLowerCase()).saveAndNotify(mContext);
            Log.d(TAG, "Added to database - id:"+id);
        }
    }

    boolean reconnectDispatched = false;
    Handler reconnectHandler = new Handler();
    Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            reconnectDispatched = false;
            connectToBondedDevices();
        }
    };

    public void reconnectIn(long delay){

        if(!reconnectDispatched) {
            reconnectDispatched = true;
            reconnectHandler.postDelayed(reconnectRunnable, delay);
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

            connectToBondedDevices();
        }



    }

    private void connectToBondedDevices() {
        for(BluetoothDevice device:devices){
            Log.d(TAG,"GATT - Connect:"+device.getAddress());
            device.connectGatt(mContext,false,characteristicCallback);
        }
    }

    public boolean isScanning(){
        if(mBluetoothAdapter!=null){
            return mBluetoothAdapter.isDiscovering();
        }
        return false;
    }

    private boolean isDiscovering = false;

    private BluetoothGattCallback characteristicCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    Log.d("BLE", String.format("STATE_CONNECTED status:%d newstate:%d",status,newState));
                    if(!isDiscovering && gatt != null) {
                        gatt.discoverServices();
                        isDiscovering = true;
                    }
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.d("BLE", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.d("BLE", "STATE_OTHER");
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(isDiscovering) {
                isDiscovering = false;
                BluetoothGattService service = gatt.getService(serviceUUID);
                gatt.readCharacteristic(service.getCharacteristic(characteristicUUID));
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if(characteristic.getUuid().equals(characteristicUUID)) {
                try {
                    if (characteristic.getValue() == null)
                        return;
                    String jString = new String(characteristic.getValue(),"UTF-8");
                    CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(jString));
                    BluetoothDevice device = gatt.getDevice();
                    String macaddress= "";
                    if(device != null)
                        macaddress = device.getAddress().toLowerCase();
                    Video v = new Video(packet,macaddress);
                    if(v!=null)
                        Log.d(TAG,"ID: "+v.saveAndNotify(mContext));
                    reconnectInterval = packet.reconnectIn;

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
            gatt.disconnect();
            gatt.close();
            reconnectIn(reconnectInterval);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(characteristic.getUuid().equals(characteristicUUID)){


                try {
                    if(characteristic.getValue() == null)
                        return;
                    String jString = new String(characteristic.getValue(),"UTF-8");
                    CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(jString));
                    Video v = new Video(packet,gatt.getDevice().getAddress());
                    if(v!=null)
                        v.saveAndNotify(mContext);
                    reconnectInterval = packet.reconnectIn;

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            gatt.disconnect();

            gatt.close();
            reconnectIn(reconnectInterval);


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
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG,BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    mContext.unregisterReceiver(mReceiver);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Bundle extras = intent.getExtras();
                    if(extras!=null) {
                        bondWithDevice((BluetoothDevice)extras.getParcelable(BluetoothDevice.EXTRA_DEVICE));
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

        public void bondWithDevice(BluetoothDevice device){
                 if (device == null)
                     return;

                if (isCryptoCam(device)){
                    if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                        device.createBond();
                        Log.d(TAG, "BONDING: " + device.getName());
                    }else{
                        Log.d(TAG, "BOND STATE: " + device.getBondState());
                    }

                    //todo revisit this logic to allow multiple devices to be added.
                    addDevice(device); //currently pointless as i only identify the first cam.
                    stop(); //will stop after finding the first device.

                }
        }
    }



}
