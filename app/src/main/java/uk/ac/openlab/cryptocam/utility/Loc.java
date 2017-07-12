package uk.ac.openlab.cryptocam.utility;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import com.google.firebase.crash.FirebaseCrash;


/**
 * Created by Kyle Montague on 09/05/2017.
 */

public class Loc {

    private final Context mContext;


    boolean checkGPS = false;
    boolean checkNetwork = false;
    boolean canGetLocation = false;

    Location location;

    boolean shouldTrack = false;


    public synchronized boolean isTracking() {
        return isTracking;
    }

    boolean isTracking;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 100;


    protected LocationManager locationManager;

    static Loc sharedInstance;
    public  static Loc shared(Context mContext){
        if(sharedInstance == null)
            sharedInstance = new Loc(mContext);
        return sharedInstance;
    }

    public Loc(Context mContext) {
        this.mContext = mContext;
        init();
    }

    private void init() {

        try {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            // getting GPS status
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        } catch (Exception e) {
            FirebaseCrash.report(e);
        }
    }


    public void getCurrentLocation(LocationListener listener){
        if(location!=null && listener != null){
            long delta = (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1000000000;
            if(delta > 30){
                //request the location
                requestSingleUpdate(listener);
            }else{
                listener.onLocationChanged(location);
            }
        }else{
            requestSingleUpdate(listener);
        }
    }

    private void requestSingleUpdate(LocationListener listener){
        try {
            locationManager.requestSingleUpdate( LocationManager.GPS_PROVIDER, listener, null );
        } catch ( SecurityException e ) {
            FirebaseCrash.report(e);
            listener.onLocationChanged(null);
        }
    }


    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS Not Enabled");
        alertDialog.setMessage("Do you wants to turn On GPS");
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

}
