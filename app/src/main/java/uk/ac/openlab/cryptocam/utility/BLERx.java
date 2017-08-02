package uk.ac.openlab.cryptocam.utility;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;

/**
 * Created by Kyle Montague on 31/05/2017.
 */

public class BLERx {

    final static String deviceName = "cc-";
    final UUID keyServiceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000010"); //key service
    final UUID characteristicCamKeyUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000011"); //key char
    final UUID camServiceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000000");
    final UUID characteristicCamVersionUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");
    final UUID characteristicCamNameUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");
    final UUID characteristicCamModeUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000003");
    final UUID characteristicCamLocationUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000004");




    boolean isScanning = false;

    long reconnectIn = 30 * 1000;


    public static final String TAG = "BLERx";



    RxBleClient rxBleClient;
    RxBleAdapterStateObservable rxBleAdapterStateObservable;

    Subscription scanSubscription;
    Subscription stateSubscription;
    Context mContext;
    boolean hasBLE = true;

    public BLERx(Context context){
        mContext = context;
        rxBleClient =  RxBleClient.create(mContext);
        rxBleAdapterStateObservable = new RxBleAdapterStateObservable(mContext);




    }



    public void terminate(){
        if(stateSubscription!=null && !stateSubscription.isUnsubscribed())
            stateSubscription.unsubscribe();

    }

    public void startScanning(){

        if(stateSubscription == null || stateSubscription.isUnsubscribed()){
            stateSubscription = rxBleAdapterStateObservable
                    .subscribe(bleAdapterState -> {
                        if(bleAdapterState.equals(RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF) || bleAdapterState.equals(RxBleAdapterStateObservable.BleAdapterState.STATE_OFF)){
                            //unable to scan, should stop
                            hasBLE = false;
                            if(isActive() && !scanSubscription.isUnsubscribed()){
                                scanSubscription.unsubscribe();
                            }

                        }else if(bleAdapterState.equals(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)){
                            // have ble can scan
                            hasBLE = true;
                            if(isActive())
                            {
                                startScanning();
                            }
                        }
                    });
        }

        if(scanSubscription != null && !scanSubscription.isUnsubscribed())
            return;


        scanSubscription = rxBleClient.scanBleDevices(keyServiceUUID)
                .filter(rxBleScanResult -> shouldRequestKey(rxBleScanResult.getBleDevice().getBluetoothDevice()))
                .doOnError(error -> {
                    Log.e(TAG,error.getMessage());
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        rxBleScanResult -> {
                            // Process scan result here.
                            Log.d(TAG,rxBleScanResult.getBleDevice().getName());

                            if(!Cam.exists(Realm.getDefaultInstance(),rxBleScanResult.getBleDevice().getMacAddress().toLowerCase())){
                                firstConnectionToDevice(rxBleScanResult.getBleDevice());
                            }else {
                                if(rxBleScanResult.getBleDevice().getConnectionState() != RxBleConnection.RxBleConnectionState.CONNECTED)
                                    connectToDevice(rxBleScanResult.getBleDevice());
                            }



                        },
                        throwable -> {
                            // Handle an error here.
                            Log.d(TAG,throwable.getMessage());
                        }
                );

        isScanning = true;
    }



    public void stopScanning(){
        if(scanSubscription != null && !scanSubscription.isUnsubscribed()){
            scanSubscription.unsubscribe();
        }
        isScanning = false;
    }

    public void firstConnectionToDevice(RxBleDevice device){
        Log.d(TAG,"Attempting first connection to:"+device.getMacAddress()+" "+device.getName());
        readEverything(device);
    }


    private void readEverything(RxBleDevice device){
        Observable<RxBleConnection> connectionObservable = device.establishConnection(false);
        connectionObservable
                .flatMap( // get the characteristics from the service you're interested in
                        rxBleConnection -> Observable.combineLatest(
                                rxBleConnection.readCharacteristic(characteristicCamLocationUUID),
                                rxBleConnection.readCharacteristic(characteristicCamModeUUID),
                                rxBleConnection.readCharacteristic(characteristicCamVersionUUID),
                                rxBleConnection.readCharacteristic(characteristicCamNameUUID),
                                rxBleConnection.readCharacteristic(characteristicCamKeyUUID),
                                CombinedObject::new))
                .subscribeOn(Schedulers.newThread())
                .take(5)
                .subscribe(combinedObject ->
                        {
                            Realm.getDefaultInstance().executeTransaction(realm -> {
                                Cam camera = new Cam(combinedObject.name,device.getMacAddress());
                                camera.lastseen = System.currentTimeMillis();
                                camera.mode = combinedObject.model;
                                camera.location = combinedObject.location;
                                camera.version = combinedObject.version;
                            });
                            long interval = saveKeys(device.getMacAddress(),combinedObject.key);
                            if(interval != Long.MIN_VALUE)
                                reconnectIn = interval;

                        },
                        throwable -> { /* handle errors */
                            Log.e(TAG,throwable.toString());
                        }
                );
    }

    public void connectToDevice(RxBleDevice device){
        if(hasBLE) {
            Log.d(TAG, "Attempting connection to:" + device.getMacAddress() + " " + device.getName());
            Observable<RxBleConnection> connectionObservable = device.establishConnection(false).timeout(20, TimeUnit.SECONDS);
            readKeys(connectionObservable, device);
        }
    }



    private boolean isConnected(RxBleDevice bleDevice) {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void readKeys(Observable<RxBleConnection> connectionObservable, RxBleDevice device){
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicCamKeyUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .first()
                .subscribe(characteristicValue -> {
                    long interval = saveKeys(device.getMacAddress(),characteristicValue);
                    if(interval != Long.MIN_VALUE)
                        reconnectIn = interval;
                }, throwable -> {
                    if(throwable != null)
                        Log.e(TAG,throwable.toString());
                });
    }


    private void onUnsubscribe() {
        Log.d(TAG, "onUnsubscribe");
    }

    private void onCompleted() {
        Log.d(TAG, "onCompleted");
    }

    private void triggerDisconnect() {
        Log.d(TAG, "triggerDisconnect");
    }



    private long saveKeys(String macAddress, String characteristicString){
        try {
            CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(characteristicString));
            Realm.getDefaultInstance().executeTransaction(realm -> {
                Cam cam = Cam.fromMacaddress(realm,macAddress);
                if(cam!=null) {
                    cam.videos.add(new Video(packet));
                    cam.lastseen = System.currentTimeMillis();
                }
            });
            return packet.reconnectIn;

        } catch (JSONException e) {
            Log.e(TAG,e.getMessage());
        }
        return Long.MIN_VALUE;
    }

    private long saveKeys(String macAddress, byte[] characteristicValue){
        return saveKeys(macAddress,new String(characteristicValue, StandardCharsets.UTF_8));
    }

    public boolean isActive(){
        return isScanning;
    }


    boolean shouldRequestKey(BluetoothDevice device){
        Cam cam = Cam.fromMacaddress(Realm.getDefaultInstance(),device.getAddress());
        return (cam != null && (System.currentTimeMillis() - cam.lastseen) < reconnectIn);
    }

}
