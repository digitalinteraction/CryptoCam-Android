package uk.ac.openlab.cryptocam.utility;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;

/**
 * Created by Kyle Montague on 31/05/2017.
 */

public class BLERx {

    final static String deviceName = "cc-";
    final UUID serviceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000010"); //key service
    final UUID characteristicUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000011"); //key char
    final UUID camServiceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000000");
    final UUID characteristicCamVersionUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");
    final UUID characteristicCamNameUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");
    final UUID characteristicCamModeUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000003");
    final UUID characteristicCamLocationUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000004");




    boolean isScanning = false;

    private ArrayList<RxBleDevice> devices;


    public static final String TAG = "BLERx";



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
                                if(!devices.contains(rxBleScanResult.getBleDevice()))
                                    devices.add(rxBleScanResult.getBleDevice());
                            }


                            if(!Cam.exists(rxBleScanResult.getBleDevice().getMacAddress().toLowerCase())){
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
                                rxBleConnection.readCharacteristic(characteristicUUID),
                                CombinedObject::new))
                .subscribeOn(Schedulers.newThread())
                .take(5)
                .subscribe(combinedObject ->
                        {
                            Log.d(TAG,combinedObject.toString());

                            Cam camera = new Cam(combinedObject.name,device.getMacAddress());
                            camera.mode = combinedObject.model;
                            camera.location_type = combinedObject.location;
                            camera.version = combinedObject.version;
                            camera.saveAndNotify(mContext);

                            Handler handler = new Handler(Looper.getMainLooper());
                            Runnable runnable = () -> connectToDevice(device);

                            long reconnectIn = saveKeys(device.getMacAddress(),combinedObject.key);
                            if(reconnectIn != Long.MIN_VALUE)
                                handler.postDelayed(runnable,reconnectIn);
                            else{
                                synchronized (devices) {
                                    devices.remove(device);
                                }
                            }
                        },
                        throwable -> { /* handle errors */
                            Log.e(TAG,throwable.toString());
                        }
                );
    }




//
//    private void readEverything(RxBleDevice device, Cam camera){
//        Observable<RxBleConnection> connectionObservable = establishConnection(device);
//
//        connectionObservable
//                .flatMap( // get the characteristics from the service you're interested in
//                        connection -> connection
//                                .discoverServices()
//                                .flatMap(services -> services
//                                        .getService(camServiceUUID)
//                                        .map(BluetoothGattService::getCharacteristics)
//                                ),
//                        Pair::new
//                )
//                .flatMap(connectionAndCharacteristics -> {
//                    final RxBleConnection connection = connectionAndCharacteristics.first;
//                    final List<BluetoothGattCharacteristic> characteristics = connectionAndCharacteristics.second;
//                    return readInitialValues(connection, characteristics);
//                })
//                .subscribe(
//                        pair -> {
//                            if(pair.first.getUuid().equals(characteristicCamLocationUUID)){
//                                camera.location_type = new String(pair.second, StandardCharsets.UTF_8);
//                            }else if(pair.first.getUuid().equals(characteristicCamModeUUID)){
//                                camera.mode = new String(pair.second, StandardCharsets.UTF_8);
//                            }else if(pair.first.getUuid().equals(characteristicCamNameUUID)){
//                                camera.name = new String(pair.second, StandardCharsets.UTF_8);
//                            }else if(pair.first.getUuid().equals(characteristicCamVersionUUID)){
//                                camera.version = new String(pair.second, StandardCharsets.UTF_8);
//                            }
//                            camera.save();
//
//                            if(camera.hasDetails()){
//                               connectionObservable.unsubscribeOn(Schedulers.immediate());
//                            }
//                        },
//                        throwable -> { /* handle errors */}
//                );
//    }



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
                     if(throwable != null)
                         Log.e(TAG,throwable.toString());
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
                    if(throwable != null)
                        Log.e(TAG,throwable.toString());
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
                    if(throwable != null)
                        Log.e(TAG,throwable.toString());
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
                    if(throwable != null)
                        Log.e(TAG,throwable.toString());
                });
    }


    private void readKeys(Observable<RxBleConnection> connectionObservable, RxBleDevice device){


        Handler handler = new Handler();
        Runnable runnable = () -> connectToDevice(device);

        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
                .observeOn(AndroidSchedulers.mainThread())
                .first()
                .subscribe(characteristicValue -> {
                    long reconnectIn = saveKeys(device.getMacAddress(),characteristicValue);
                    if(reconnectIn != Long.MIN_VALUE)
                        handler.postDelayed(runnable,reconnectIn);
                    else{
                        synchronized (devices) {
                            devices.remove(device);
                        }
                    }
                }, throwable -> {
                    if(throwable != null)
                        Log.e(TAG,throwable.toString());
                });
    }



    private Observable<RxBleConnection> establishConnection(RxBleDevice bleDevice) {
        return bleDevice
                .establishConnection(false);//               .compose(new ConnectionSharingAdapter());
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
        Log.d("Keys",characteristicString);
        try {
            CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(characteristicString));
            Video video = new Video(packet,macAddress);
            video.saveAndNotify(mContext);
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
        //todo check the last time
        return true;
    }

}
