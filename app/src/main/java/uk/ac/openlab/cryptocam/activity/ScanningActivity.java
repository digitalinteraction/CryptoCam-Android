package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.File;
import java.net.URI;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.KeyListAdapter;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.services.CryptoCamReceiver;
import uk.ac.openlab.cryptocam.services.CryptoCamScanService;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

public class ScanningActivity extends AppCompatActivity implements KeyListAdapter.KeyListItemListener{


    RecyclerView list;
    KeyListAdapter adapter;



    Intent serviceIntent;


    //TODO needs to be put into the config class.
    String path = Environment.getExternalStorageDirectory().getAbsolutePath();


    IntentFilter dataUpdateFilter = new IntentFilter(Video.DATA_UPDATE_VIDEO);
    CryptoCamReceiver dataUpdateReceiver = new CryptoCamReceiver() {
        @Override
        public void downloadedVideo(URI uri, long id) {
            super.downloadedVideo(uri, id);
        }

        @Override
        public void downloadedThumbnail(URI uri, long id) {
            super.downloadedThumbnail(uri, id);
        }

        @Override
        public void videoAdded(long id) {
            super.videoAdded(id);
            adapter.reloadData();
        }
    };


    CryptoCamScanService mBoundService;
    boolean mServiceBound = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CryptoCamScanService.CryptoCamBinder myBinder = (CryptoCamScanService.CryptoCamBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        CryptoCamReceiver.registerReceiver(this,dataUpdateReceiver);
        serviceIntent = new Intent(this, CryptoCamScanService.class);
        bindService(serviceIntent,mServiceConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);


        list = (RecyclerView)findViewById(R.id.video_recycleview);

        GridLayoutManager glm = new GridLayoutManager(this,3);
        glm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(glm);

        adapter = new KeyListAdapter(this,this);
        list.setAdapter(adapter);

        list.invalidate();

        // Quick permission check
        int permissionCheck = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001); //Any number
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        adapter.reloadData();
        this.registerReceiver(dataUpdateReceiver,dataUpdateFilter);
        checkDirectory();


    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(dataUpdateReceiver);


    }

    @Override
    protected void onDestroy() {
        //TODO will kill the background service when the app is closed.
        if(mBoundService != null) {
            mBoundService.stopSelf();

            stopService(serviceIntent);
            unbindService(mServiceConnection);
        }

        super.onDestroy();

    }

    @Override
    public void itemSelected(int index) {
        Video v = Video.findById(Video.class,adapter.getItemId(index));
        String local = v.checkForLocalVideo(path);
        if(local == null) {
            DownloadRequest request = new DownloadRequest(v.getVideoUrl(), path, v.getKey(), v.getIV());
            new DownloadTask(this).execute(request);
        }else{
            //todo open video
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(local));
            intent.setDataAndType(Uri.parse(local), "video/*");
            startActivity(intent);
        }

    }



    private boolean checkDirectory(){
        File f = new File(path);
        if(!f.exists())
            return f.mkdirs();


        return true;
    }
}
