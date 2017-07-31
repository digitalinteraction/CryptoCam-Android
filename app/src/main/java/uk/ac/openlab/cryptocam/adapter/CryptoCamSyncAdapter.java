package uk.ac.openlab.cryptocam.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

/**
 * Created by Kyle Montague on 30/07/2017.
 */

public class CryptoCamSyncAdapter extends AbstractThreadedSyncAdapter {

    private final String TAG = "SyncAdapter";
    private final String thumbpath = CryptoCamApplication.directory();

    public CryptoCamSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.d(TAG,"Trying to sync");

        ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        assert activeNetwork != null;
        boolean isConnected  = activeNetwork.isConnectedOrConnecting();
        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;


        //if we have an active wifi connection then sync the thumbnails in the background.
        if(isConnected && isWiFi) {
            Log.d(TAG,"Has active wifi");
            List<Video> videoList = Video.find(Video.class, "localthumb IS NULL");
            Log.d(TAG,String.format(Locale.ENGLISH,"Number of videos: %d",videoList.size()));
            for (Video video : videoList) {
                new DownloadTask(getContext()).execute(new DownloadRequest(video.getThumbnailUrl(), thumbpath, video.getKey(), video.getIV()));
            }
        }
    }
}
