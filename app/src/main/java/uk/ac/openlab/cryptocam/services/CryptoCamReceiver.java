package uk.ac.openlab.cryptocam.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.net.URI;

/**
 * Created by Kyle Montague on 26/02/2017.
 */

public class CryptoCamReceiver extends BroadcastReceiver implements ICryptoCamReceiver {


    public static final String ACTION_NEW_CAMERA = "CRYPTO_CAM.ACTION.NEW_CAMERA";
    public static final String ACTION_VIDEO_KEYS = "CRYPTO_CAM.ACTION.VIDEO_KEYS";

    public static final String ACTION_VIDEO_DOWNLOADED = "CRYPTO_CAM.ACTION.VIDEO_DOWNLOADED";
    public static final String ACTION_THUMBNAIL_DOWNLOADED = "CRYPTO_CAM.ACTION.THUMBNAIL_DOWNLOADED";

    public static final String ACTION_STARTED_SCANNING = "CRYPTO_CAM.ACTION.STARTED_SCANNING";
    public static final String ACTION_STOPPED_SCANNING = "CRYPTO_CAM.ACTION.STOPPED_SCANNING";



    public static final String EXTRA_URI = "CRYPTO_CAM.EXTRA.URI";
    public static final String EXTRA_ID = "CRYPTO_CAM.EXTRA.ID";

    private static final String TAG = "CCReceiver";

    public static void registerReceiver(Context context, CryptoCamReceiver receiver){
        IntentFilter filter = new IntentFilter(ACTION_NEW_CAMERA);
        filter.addAction(ACTION_VIDEO_KEYS);
        filter.addAction(ACTION_VIDEO_DOWNLOADED);
        filter.addAction(ACTION_THUMBNAIL_DOWNLOADED);
        context.registerReceiver(receiver, filter);
    }


    public static void newCamera(Context context, long id){
        Intent i = new Intent(ACTION_NEW_CAMERA);
        i.putExtra(EXTRA_ID,id);
        context.sendBroadcast(i);
    }


    public static void newKey(Context context){
        broadcast(context, ACTION_VIDEO_KEYS);
    }


    public static void newVideo(Context context){
        broadcast(context, ACTION_VIDEO_DOWNLOADED);
    }

    public static void newThumbnail(Context context){
        broadcast(context, ACTION_THUMBNAIL_DOWNLOADED);
    }

    public static void startedScanning(Context context){
        broadcast(context, ACTION_STARTED_SCANNING);
    }

    public static void stoppedScanning(Context context){
        broadcast(context, ACTION_STOPPED_SCANNING);
    }

    private static void broadcast(Context context, String action){
        context.sendBroadcast(new Intent(action));

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String uri = null;
        if(intent.getExtras()!=null) {
            try {
                uri = intent.getExtras().getString(EXTRA_URI, null);
            }catch (Exception e){
                Log.e(TAG,e.toString());
            }
            long id = intent.getExtras().getLong(EXTRA_ID, -1);

            switch (action) {
                case ACTION_VIDEO_DOWNLOADED:
                    downloadedVideo(URI.create(uri), id);
                    break;
                case ACTION_THUMBNAIL_DOWNLOADED:
                    downloadedThumbnail(URI.create(uri), id);
                    break;
                case ACTION_VIDEO_KEYS:
                    newVideoKey(id);
                    break;
            }
        }
    }

    @Override
    public void downloadedVideo(URI uri, long id) {

    }

    @Override
    public void downloadedThumbnail(URI uri, long id) {

    }

    @Override
    public void cameraHasAppeared(long id) {

    }

    @Override
    public void cameraHasDisappeared(long id) {

    }

    @Override
    public void scanningForCameras() {

    }

    @Override
    public void stoppedScanning() {

    }

    @Override
    public void cameraAdded(long id) {

    }

    @Override
    public void videoAdded(long id) {

    }


    @Override
    public void newVideoKey(long id){

    }
}
