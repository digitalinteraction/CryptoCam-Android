package uk.ac.openlab.cryptocam;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Kyle Montague on 31/05/2017.
 */


@RunWith(AndroidJUnit4.class)
public class GBInstrumentedTest {

    Context appContext;
    RxBleClient rxBleClient;




    final String bandMAC = "88:0f:10:e5:52:48".toUpperCase();


    final String TAG = "RX_BLE";
    Object syncObject;

    @Before
    public void setup(){
        Log.d(TAG,"setup");
        appContext = InstrumentationRegistry.getTargetContext();
        rxBleClient = RxBleClient.create(appContext);
        syncObject = new Object();
    }


    @Test
    public void connect() throws InterruptedException {
        Log.d(TAG,"starting");
        rxBleClient.scanBleDevices()
                .filter(rxBleScanResult -> isDevice(rxBleScanResult.getBleDevice()))
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(
                        throwable -> {
                            fail(throwable.getMessage());
                        }
                )
                .subscribe(
                        rxBleScanResult -> {
                            Log.d(TAG,(rxBleScanResult.getBleDevice().toString()));

                            rxBleScanResult.getBleDevice().establishConnection(false)
                                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(UUID.fromString("")))
                                    .subscribe(
                                            characteristicValue -> {
                                                Log.d(TAG,characteristicValue.toString());
                                                syncObject.notify();
                                            },
                                            throwable -> {
                                                Log.e(TAG,throwable.getMessage());
                                                syncObject.notify();
                                            });

                        }
                );
        assertTrue(true);


        synchronized(syncObject){
            syncObject.wait();
        }
    }


    private boolean isDevice(RxBleDevice device){
        return (device.getMacAddress().toUpperCase().equals(bandMAC));
    }
}
