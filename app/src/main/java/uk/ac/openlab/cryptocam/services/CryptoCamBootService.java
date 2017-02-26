package uk.ac.openlab.cryptocam.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class CryptoCamBootService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context,CryptoCamScanService.class);
        context.startService(serviceIntent);
    }
}
