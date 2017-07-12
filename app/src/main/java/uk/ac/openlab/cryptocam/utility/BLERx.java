package uk.ac.openlab.cryptocam.utility;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

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
    private static final long RESCAN_DELAY = 60*1000; // one scan every minute for new cammeras
    final UUID serviceUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001"); //will soon be 0000
    final UUID characteristicUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000002");
    final UUID camDetailUUID = UUID.fromString("cc92cc92-ca19-0000-0000-000000000001");

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

        scanSubscription = rxBleClient.scanBleDevices()
                .filter(rxBleScanResult -> isCryptoCam(rxBleScanResult.getBleDevice().getBluetoothDevice()))
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

                                Cam cam = new Cam(rxBleScanResult.getBleDevice().getName(),rxBleScanResult.getBleDevice().getMacAddress()); //TODO ADD GPS LOCATION
                                Loc.shared(mContext).getCurrentLocation(new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        if(location!=null) {
                                            cam.location = location;
                                        }
                                        cam.saveAndNotify(mContext);
                                    }

                                    @Override
                                    public void onStatusChanged(String s, int i, Bundle bundle) {

                                    }

                                    @Override
                                    public void onProviderEnabled(String s) {

                                    }

                                    @Override
                                    public void onProviderDisabled(String s) {

                                    }
                                });
                            }

                            connectToDevice(rxBleScanResult.getBleDevice());

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


    public void connectToDevice(RxBleDevice device){
        Log.d(TAG,"Attempting connection to:"+device.getMacAddress()+" "+device.getName());
        Handler handler = new Handler();
        Runnable runnable = () -> connectToDevice(device);

        device.establishConnection(false)
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
                .first()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    Log.d(TAG,"Got characteristic value from"+device.getName());
                    String jString = new String(characteristicValue, StandardCharsets.UTF_8);
                    try {

                        CryptoCamPacket packet = CryptoCamPacket.fromJson(new JSONObject(jString));
                        Video video = new Video(packet,device.getMacAddress());
                        video.saveAndNotify(mContext);

                        handler.postDelayed(runnable,packet.reconnectIn);


                    } catch (JSONException e) {
                        Log.e(TAG,e.getMessage());
                    }
                }, throwable -> {
                    if(throwable != null && throwable.getMessage()!=null)
                        Log.e(TAG,throwable.getMessage());

                    synchronized (devices) {
                        devices.remove(device);
                    }
                });

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
