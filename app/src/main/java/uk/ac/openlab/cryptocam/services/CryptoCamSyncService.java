package uk.ac.openlab.cryptocam.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import uk.ac.openlab.cryptocam.adapter.CryptoCamSyncAdapter;

/**
 * Created by Kyle Montague on 30/07/2017.
 */

public class CryptoCamSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static CryptoCamSyncAdapter sSyncAdapter;


    private boolean isSyncing = false;


    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if(sSyncAdapter == null)
                sSyncAdapter = new CryptoCamSyncAdapter(getApplicationContext(),true);
        }
    }

    public boolean isSyncing() {
        return isSyncing;
    }


    public void start(){
        isSyncing=true;
    }

    public void cancel(){
        isSyncing = false;
    }
}
