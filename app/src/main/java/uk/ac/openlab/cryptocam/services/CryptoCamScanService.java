package uk.ac.openlab.cryptocam.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import uk.ac.openlab.cryptocam.utility.BLE;

public class CryptoCamScanService extends Service {

    BLE mBle;
    CryptoCamBinder mBinder = new CryptoCamBinder();

    public CryptoCamScanService() {
        mBle = new BLE(this);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBle.init();
        mBle.start();
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public boolean stopService(Intent name) {
        mBle.disable();
        return super.stopService(name);
    }


    public boolean isActive(){
        return mBle.isActive();
    }

    public class CryptoCamBinder extends Binder{

        public CryptoCamScanService getService(){
            return CryptoCamScanService.this;
        }
    }
}
