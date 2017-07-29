package uk.ac.openlab.cryptocam.utility;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;

/**
 * Created by Kyle Montague on 31/05/2017.
 */

public class BLERx {

    final static String deviceName = "cc-";
    private static final long RESCAN_DELAY = 60*1000; // one scan every minute for new cammeras
    final UUID serviceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000010"); //key service
    final UUID characteristicUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000011"); //key char
    final UUID camServiceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000000");
    final UUID characteristicCamVersionUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");
    final UUID characteristicCamNameUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");
    final UUID characteristicCamModeUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000003");
    final UUID characteristicCamLocationUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000004");


    long reconnectInterval = 300000;

    boolean isScanning = false;

    private ArrayList<RxBleDevice> devices;


    public static final String TAG = "RxBLE";



    RxBleClient rxBleClient;

    Subscription scanSubscription;
    Context mContext;

    public BLERx(Context context){
        mContext = context;
        rxBleClient =  RxBleClient.create(mContext);
        devices = new ArrayList<>();


    }


    public void startScanning(){
        if(scanSubscription != null && !scanSubscription.isUnsubscribed())
            return;

        scanSubscription = rxBleClient.scanBleDevices(serviceUUID)
                .filter(rxBleScanResult -> shouldRequestKey(rxBleScanResult.getBleDevice().getBluetoothDevice()))
                .filter(rxBleScanResult -> !devices.contains(rxBleScanResult.getBleDevice()))
                .doOnError(error -> {
                    Log.e(TAG,error.getMessage());
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        rxBleScanResult -> {
                            // Process scan result here.
                            Log.d(TAG,rxBleScanResult.getBleDevice().getName());
                            synchronized (devices) {
                                Log.d(TAG,"Adding device: "+rxBleScanResult.getBleDevice().getMacAddress());
                                devices.add(rxBleScanResult.getBleDevice());
                            }

                            if(!Cam.exists(rxBleScanResult.getBleDevice().getMacAddress())){
                                firstConnectionToDevice(rxBleScanResult.getBleDevice());
                            }else {
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

        Cam camera = new Cam(device.getName(),device.getMacAddress());
        camera.saveAndNotify(mContext);

        Log.d(TAG,"Attempting first connection to:"+device.getMacAddress()+" "+device.getName());
        Observable<RxBleConnection> connectionObservable = establishConnection(device);

            readName(connectionObservable, camera);
            readVersion(connectionObservable, camera);
            readModel(connectionObservable, camera);
            readLocation(connectionObservable, camera);

            readKeys(connectionObservable, device);


    }

    public void connectToDevice(RxBleDevice device){
        Log.d(TAG,"Attempting connection to:"+device.getMacAddress()+" "+device.getName());
        Observable<RxBleConnection> connectionObservable = establishConnection(device);
        readKeys(connectionObservable,device);

    }


//    public void connectToDevice(RxBleDevice device){
//        Log.d(TAG,"Attempting connection to:"+device.getMacAddress()+" "+device.getName());
//        Handler handler = new Handler();
//        Runnable runnable = () -> connectToDevice(device);
//
//        device.establishConnection(false)
//                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
//                .first()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(characteristicValue -> {
//                    Log.d(TAG,"Got characteristic value from"+device.getName());
//                    String jString = new String(characteristicValue, StandardCharsets.UTF_8);
//                    try {
//
//                        CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(jString));
//                        Video video = new Video(packet,device.getMacAddress());
//                        video.saveAndNotify(mContext);
//
//                        handler.postDelayed(runnable,packet.reconnectIn);
//
//
//                    } catch (JSONException e) {
//                        Log.e(TAG,e.getMessage());
//                    }
//                }, throwable -> {
//                    if(throwable != null && throwable.getMessage()!=null)
//                        Log.e(TAG,throwable.getMessage());
//
//                    synchronized (devices) {
//                        devices.remove(device);
//                    }
//                });
//
//    }


    private boolean isConnected(RxBleDevice bleDevice) {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void readName(Observable<RxBleConnection> connectionObservable, Cam camera){
         connectionObservable
                 .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicCamNameUUID))
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(characteristicValue -> {
                     String value = new String(characteristicValue, StandardCharsets.UTF_8);
                     Log.d(TAG,"name: "+value);
                     camera.name = value;
                     camera.save();
                 }, throwable -> {
                     if(throwable != null && throwable.getMessage()!=null)
                         Log.e(TAG,throwable.getMessage());
                 });
    }

    private void readVersion(Observable<RxBleConnection> connectionObservable, Cam camera){
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicCamVersionUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    String value = new String(characteristicValue, StandardCharsets.UTF_8);
                    Log.d(TAG,"version: "+value);

                    camera.version = value;
                    camera.save();
                }, throwable -> {
                    if(throwable != null && throwable.getMessage()!=null)
                        Log.e(TAG,throwable.getMessage());
                });
    }


    private void readModel(Observable<RxBleConnection> connectionObservable, Cam camera){
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicCamModeUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    String value = new String(characteristicValue, StandardCharsets.UTF_8);
                    Log.d(TAG,"model: "+value);

                    camera.mode = value;
                    camera.save();
                }, throwable -> {
                    if(throwable != null && throwable.getMessage()!=null)
                        Log.e(TAG,throwable.getMessage());
                });
    }

    private void readLocation(Observable<RxBleConnection> connectionObservable, Cam camera){
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicCamLocationUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    String value = new String(characteristicValue, StandardCharsets.UTF_8);
                    Log.d(TAG,"location: "+value);
                    camera.location_type = value;
                    camera.save();
                }, throwable -> {
                    if(throwable != null && throwable.getMessage()!=null)
                        Log.e(TAG,throwable.getMessage());
                });
    }


    private void readKeys(Observable<RxBleConnection> connectionObservable, RxBleDevice device){


        Handler handler = new Handler();
        Runnable runnable = () -> connectToDevice(device);

        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    Log.d(TAG,"keys: "+characteristicValue.toString());
                    long reconnectIn = saveKeys(device.getMacAddress(),characteristicValue);
                    if(reconnectIn != Long.MIN_VALUE)
                        handler.postDelayed(runnable,reconnectIn);
                    triggerDisconnect();
                }, throwable -> {
                    if(throwable != null && throwable.getMessage()!=null)
                        Log.e(TAG,throwable.getMessage());

                    synchronized (devices) {
                        devices.remove(device);
                    }
                });
    }


    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();

    private Observable<RxBleConnection> establishConnection(RxBleDevice bleDevice) {
        return bleDevice
                .establishConnection(false);/*
                .doOnUnsubscribe(this::onUnsubscribe)
                .compose(new ConnectionSharingAdapter());*/
    }

    private void onUnsubscribe() {
        Log.d(TAG, "onUnsubscribe");
    }

    private void triggerDisconnect() {
        Log.d(TAG, "triggerDisconnect");
        disconnectTriggerSubject.onNext(null);
    }







    private long saveKeys(String macAddress, byte[] characteristicValue){
        String jString = new String(characteristicValue, StandardCharsets.UTF_8);
        try {
            CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(jString));
            Video video = new Video(packet,macAddress);
            video.saveAndNotify(mContext);
            return packet.reconnectIn;

        } catch (JSONException e) {
            Log.e(TAG,e.getMessage());
        }
        return Long.MIN_VALUE;
    }

    public boolean isActive(){
        return isScanning;
    }

    //todo change this
    boolean isCryptoCam(BluetoothDevice device){
        return (device.getName() != null && device.getName().toLowerCase().startsWith(deviceName));
    }

    boolean shouldRequestKey(BluetoothDevice device){
        //todo check the last time
        return true;
    }
}
