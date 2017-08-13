package uk.ac.openlab.cryptocam.utility;

import android.util.Log;

import java.util.Locale;

/**
 * Created by kylemontague on 15/02/2017.
 */

public class DownloadRequest{

    String url;
    String path;
    String key;
    String iv;


    public DownloadRequest(String url, String path, String key, String iv){
        this.url = url;
        this.path = path;
        this.key = key;
        this.iv = iv;

        Log.d("DownloadRequest",String.format(Locale.ENGLISH,"%s  %s",url,path));
    }
}
