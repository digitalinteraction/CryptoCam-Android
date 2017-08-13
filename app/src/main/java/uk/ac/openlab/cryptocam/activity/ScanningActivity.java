package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.CameraListAdapter;
import uk.ac.openlab.cryptocam.adapter.KeyListViewHolder;
import uk.ac.openlab.cryptocam.adapter.VideoListAdapter;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.services.CryptoCamScanService;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

public class ScanningActivity extends AppCompatActivity {


    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_CAMERA = "EXTRA_CAMERA";

    public static final String MODE_GROUPED = "EXTRA_MODE_GROUPED";
//    public static final String MODE_ALL = "EXTRA_MODE_ALL";
    public static final String MODE_SINGLE = "EXTRA_MODE_SINGLE";


    private static final String TAG = "UI";
    RecyclerView list;
    TextView emptyView;
    CameraListAdapter adapter;
    VideoListAdapter videoAdapter;



    RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            updateUI();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            updateUI();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            updateUI();
        }


    };

    Intent serviceIntent;


    private String mMode = "EXTRA_MODE_GROUPED";
    private String mCameraID = null;

    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        path = CryptoCamApplication.directory();

        list = (RecyclerView)findViewById(R.id.video_recycleview);
        emptyView = (TextView)findViewById(R.id.emptyView);



        Bundle b = getIntent().getExtras();
        if(b != null){
            mCameraID = b.getString(EXTRA_CAMERA,null);
            mMode = b.getString(EXTRA_MODE,MODE_GROUPED);
        }


        int col = Objects.equals(mMode, MODE_GROUPED) ? 2:1;
        GridLayoutManager glm = new GridLayoutManager(this,col);
        glm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(glm);


        if(Objects.equals(mMode, MODE_GROUPED)) {
            adapter = new CameraListAdapter(Cam.all(Realm.getDefaultInstance()), true, (id, index) -> showCameraVideos(id,index));
            adapter.registerAdapterDataObserver(dataObserver);
            list.setAdapter(adapter);

        }else{
            Cam cam = Cam.get(Realm.getDefaultInstance(),mCameraID);
            videoAdapter = new VideoListAdapter(cam.getVideos(), true, (id, index) -> downloadAndPlay(id,index));
            videoAdapter.registerAdapterDataObserver(dataObserver);
            list.setAdapter(videoAdapter);
        }

        list.invalidate();




        // Quick permission check
        int permissionCheck;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001); //Any number
            }
        }

        checkDirectory();
        checkBluetooth();


    }




    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }



    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }



    int REQUEST_ENABLE_BT = 1;
    private void checkBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }else{
                startScanning();
            }
        }
    }

    private void startScanning() {
        serviceIntent = new Intent(this, CryptoCamScanService.class);
        startService(serviceIntent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK) {
                startScanning();
            }else{
                new AlertDialog.Builder(ScanningActivity.this)
                        .setTitle("Bluetooth")
                        .setMessage("Your bluetooth is currently disabled. Cryptocam is unable to scan for nearby videos without bluetooth enabled.")
                        .setPositiveButton("Activate", (dialog, which) -> {
                            dialog.dismiss();
                            checkBluetooth();
                        })
                        .setNegativeButton("Quit", (dialog, which) -> finishAffinity())
                        .create().show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(mMode.equals(MODE_GROUPED)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_stop_scanning:

                new AlertDialog.Builder(this)
                        .setTitle("Stopping CryptoCam")
                        .setMessage("Are you sure you want to stop scanning for CryptoCam videos? This wont start again until you next launch the app")
                        .setPositiveButton("Yes, Quit", (dialog, which) -> {
                            serviceIntent = new Intent(ScanningActivity.this, CryptoCamScanService.class);
                            stopService(serviceIntent);
                            finish();
                        })
                        .setNegativeButton("No, Keep Scanning", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();

                break;
        }
        return true;
    }


    private void updateUI(){
        RealmRecyclerViewAdapter a = mMode.equals(MODE_GROUPED)? adapter:videoAdapter;
        if(a.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(mMode.equals(MODE_GROUPED)?R.string.empty_text_no_cameras:R.string.empty_text_no_videos);
        }else{
            emptyView.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
    }



    private void showCameraVideos(String id, int index){
        if(id == null){
            Toast.makeText(this, "No Cam", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this,ScanningActivity.class);
        intent.putExtra(ScanningActivity.EXTRA_CAMERA,id);
        intent.putExtra(ScanningActivity.EXTRA_MODE,MODE_SINGLE);
        this.startActivity(intent);
    }

    private void downloadAndPlay(String id, int index){

        Video v = Realm.getDefaultInstance().where(Video.class).equalTo("id",id).findFirst();
        if(v == null)
            return;

        String local = Video.checkForLocal(path,v.getVideoUrl());

        if(local == null || !new File(local).exists()) {

            String videoId = v.getId();

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

                        Realm.getDefaultInstance().executeTransaction(realm -> {
                            Video video = realm.where(Video.class).equalTo("id",videoId).findFirst();
                            video.setLocalvideo(uri);
                        });
                    }
                }
            };
            new DownloadTask(progress).execute(request);
        }else{
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(local));
            intent.setDataAndType(Uri.parse(local), "video/*");
            startActivity(intent);
        }
    }



    private boolean checkDirectory() {
        File f = new File(path);
        return f.exists() || f.mkdirs();
    }

}
