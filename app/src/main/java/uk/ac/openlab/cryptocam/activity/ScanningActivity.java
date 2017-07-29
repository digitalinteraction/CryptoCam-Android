package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.net.URI;

import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.KeyListAdapter;
import uk.ac.openlab.cryptocam.adapter.KeyListViewHolder;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.services.CryptoCamReceiver;
import uk.ac.openlab.cryptocam.services.CryptoCamScanService;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

public class ScanningActivity extends AppCompatActivity implements KeyListAdapter.KeyListItemListener{


    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_CAMERA = "EXTRA_CAMERA";

    public static final String MODE_GROUPED = "EXTRA_MODE_GROUPED";
    public static final String MODE_ALL = "EXTRA_MODE_ALL";
    public static final String MODE_SINGLE = "EXTRA_MODE_SINGLE";


    private static final String TAG = "UI";
    RecyclerView list;
    KeyListAdapter adapter;


    Intent serviceIntent;


    private String mMode = "EXTRA_MODE_GROUPED";
    private String mCameraID = null;

    String path = CryptoCamApplication.directory();


    CryptoCamReceiver dataUpdateReceiver = new CryptoCamReceiver() {
        @Override
        public void downloadedVideo(URI uri, long id) {
            super.downloadedVideo(uri, id);
        }

        @Override
        public void downloadedThumbnail(URI uri, long id) {
            super.downloadedThumbnail(uri, id);
            adapter.reloadData();
        }

        @Override
        public void videoAdded(long id) {
            super.videoAdded(id);
            adapter.reloadData();
        }


        @Override
        public void newVideoKey(long id) {
            super.newVideoKey(id);
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



        list = (RecyclerView)findViewById(R.id.video_recycleview);




        Bundle b = getIntent().getExtras();
        if(b != null){
            mCameraID = b.getString(EXTRA_CAMERA,null);
            mMode = b.getString(EXTRA_MODE,MODE_GROUPED);
        }

        int col = mMode == MODE_GROUPED? 2:1;
        GridLayoutManager glm = new GridLayoutManager(this,col);
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


        switch (mMode){
            case MODE_GROUPED:
                adapter.loadCameras();
                break;
            case MODE_ALL:
                adapter.reloadData();
                break;
            case MODE_SINGLE:
                if(mCameraID!= null)
                    adapter.loadCameraVideos(mCameraID);
                else {
                    adapter.loadCameras();
                    mMode = MODE_GROUPED;
                }
                break;
        }
        CryptoCamReceiver.registerReceiver(this,dataUpdateReceiver);
        checkDirectory();

        serviceIntent = new Intent(this, CryptoCamScanService.class);
        bindService(serviceIntent,mServiceConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);

    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(dataUpdateReceiver);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mBoundService != null) {
            mBoundService.stopSelf();
            unbindService(mServiceConnection);
        }
    }

    public String getMode(){
        return mMode;
    }



    @Override
    public void itemSelected(int index) {

        Video v = Video.findById(Video.class,adapter.getItemId(index));
        if(v == null)
            return;

        switch (mMode){
            case MODE_GROUPED:
                showCameraVideos(v.getCam());
                break;
            default:
                downloadAndPlay(v, index);
                break;
        }



    }


    private void showCameraVideos(Cam cam){
        if(cam == null){
            Toast.makeText(this, "No Cam", Toast.LENGTH_SHORT).show();
            return;
        }
        long id = cam.getId();
        Intent intent = new Intent(this,ScanningActivity.class);
        intent.putExtra(ScanningActivity.EXTRA_CAMERA,""+id);
        intent.putExtra(ScanningActivity.EXTRA_MODE,MODE_SINGLE);
        this.startActivity(intent);
    }

    private void downloadAndPlay(Video v, int index){
        String local = v.checkForLocalVideo(path);

        if(local == null) {
            final KeyListViewHolder holder = (KeyListViewHolder)list.findViewHolderForAdapterPosition(index);
            holder.showProgress(true);

            DownloadRequest request = new DownloadRequest(v.getVideoUrl(), path, v.getKey(), v.getIV());
            DownloadTask.DownloadTaskProgress progress = new DownloadTask.DownloadTaskProgress() {
                @Override
                public void onProgressUpdate(int progress) {
                    Log.d(TAG,progress+"%");
                }

                @Override
                public void onDownloadComplete(boolean successful, String uri) {
                    if(successful) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setDataAndType(Uri.parse(uri), "video/*");
                        startActivity(intent);
                        holder.showProgress(false);
                        v.localvideo = uri;
                        v.save();
                    }
                }
            };
            new DownloadTask(this,progress).execute(request);
        }else{
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
