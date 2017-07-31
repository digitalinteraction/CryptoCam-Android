package uk.ac.openlab.cryptocam.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.utility.BLERx;
import uk.ac.openlab.cryptocam.utility.Loc;

public class CryptoCamScanService extends Service {

    final String TAG = "CryptoCamScanService";

    BLERx mBle;
    private PowerManager.WakeLock wakeLock;

    CryptoCamBinder mBinder = new CryptoCamBinder();

    public CryptoCamScanService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(mBle == null)
            mBle = new BLERx(getBaseContext());


//        Loc.shared(getBaseContext());//initialise the shared instance of the location helper.

        mBle.startScanning();
        if(!wakeLock.isHeld())
            wakeLock.acquire();

        startForeground(R.string.app_name,CryptoCamNotificationService.getNotification(this));
        return START_STICKY;
    }



    @Override
    public boolean stopService(Intent name) {
        mBle.stopScanning();
        if(wakeLock.isHeld())
            wakeLock.release();

        stopForeground(true);
        return super.stopService(name);
    }


    @Override
    public void onDestroy() {
        mBle.stopScanning();
        super.onDestroy();
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
