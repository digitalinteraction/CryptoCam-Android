package uk.ac.openlab.cryptocam.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import io.realm.Realm;
import io.realm.RealmResults;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.utility.BLERx;

public class CryptoCamScanService extends Service {

    final String TAG = "CryptoCamScanService";

    BLERx mBle;
    private PowerManager.WakeLock wakeLock;

    CryptoCamBinder mBinder = new CryptoCamBinder();

    RealmResults<Cam> allCams;
    RealmResults<Cam> recentCams;

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

        allCams = Cam.all(Realm.getDefaultInstance());
        allCams.addChangeListener(cams -> {
            recentCams.load();
        });
        recentCams = Cam.recentlySeen(Realm.getDefaultInstance(),60*1000);//seen in the last minute
        recentCams.addChangeListener(cams -> {
            Notification notification = CryptoCamNotificationService.getNotification(this,getResources().getQuantityString(R.plurals.cryptocams,recentCams.size(),recentCams.size()));
            CryptoCamNotificationService.showNotification(this,notification);
        });

        if(mBle == null)
            mBle = new BLERx(this);


//        Loc.shared(getBaseContext());//initialise the shared instance of the location helper.

        if(!mBle.isActive())
            mBle.startScanning();

        if(!wakeLock.isHeld())
            wakeLock.acquire();

        startForeground(R.string.app_name,CryptoCamNotificationService.getNotification(this));
        return START_STICKY;
    }



    @Override
    public boolean stopService(Intent name) {
        mBle.stopScanning();
        mBle.terminate();
        if(wakeLock.isHeld())
            wakeLock.release();

        stopForeground(true);
        return super.stopService(name);
    }


    @Override
    public void onDestroy() {
        mBle.stopScanning();
        mBle.terminate();
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
