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

import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

/**
 * Created by Kyle Montague on 30/07/2017.
 */

public class CryptoCamSyncAdapter extends AbstractThreadedSyncAdapter {

    private final String TAG = "SyncAdapter";
    private String thumbpath;

    public CryptoCamSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.thumbpath = CryptoCamApplication.directory();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        RealmResults<Video> videoList = Video.withoutThumbnails(Realm.getDefaultInstance());

        //check if we now have the file locally.
        for(Video video:videoList){
            String local = video.checkForLocalThumbnail(thumbpath);
            if(local!=null){
                Realm.getDefaultInstance().executeTransaction(realm -> Video.get(realm,video.getId()).setLocalthumb(local));
            }
        }


        //update the realmresults list before downloading from the web.
        videoList.load();


        ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        assert activeNetwork != null;
        boolean isConnected  = activeNetwork.isConnectedOrConnecting();
        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;


        //if we have an active wifi connection then sync the thumbnails in the background.
        if(isConnected && isWiFi) {
            Log.d(TAG,"Has active wifi");
            Log.d(TAG,String.format(Locale.ENGLISH,"Number of videos: %d",videoList.size()));
            for (Video video : videoList) {
                new DownloadTask().execute(new DownloadRequest(video.getThumbnailUrl(), thumbpath, video.getKey(), video.getIV()));
            }
        }
    }
}
