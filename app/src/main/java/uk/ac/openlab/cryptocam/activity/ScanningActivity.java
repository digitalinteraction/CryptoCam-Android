package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
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

import io.realm.Realm;
import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.CameraListAdapter;
import uk.ac.openlab.cryptocam.adapter.KeyListViewHolder;
import uk.ac.openlab.cryptocam.adapter.RealmListAdapter;
import uk.ac.openlab.cryptocam.adapter.VideoListAdapter;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.services.CryptoCamScanService;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

public class ScanningActivity extends AppCompatActivity{


    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_CAMERA = "EXTRA_CAMERA";

    public static final String MODE_GROUPED = "EXTRA_MODE_GROUPED";
    public static final String MODE_ALL = "EXTRA_MODE_ALL";
    public static final String MODE_SINGLE = "EXTRA_MODE_SINGLE";


    private static final String TAG = "UI";
    RecyclerView list;
    TextView emptyView;
    RealmListAdapter adapter;


    Intent serviceIntent;


    private String mMode = "EXTRA_MODE_GROUPED";
    private String mCameraID = null;

    String path = CryptoCamApplication.directory();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        list = (RecyclerView)findViewById(R.id.video_recycleview);
        emptyView = (TextView)findViewById(R.id.emptyView);



        Bundle b = getIntent().getExtras();
        if(b != null){
            mCameraID = b.getString(EXTRA_CAMERA,null);
            mMode = b.getString(EXTRA_MODE,MODE_GROUPED);
        }


        int col = mMode == MODE_GROUPED? 2:1;
        GridLayoutManager glm = new GridLayoutManager(this,col);
        glm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(glm);


        if(mMode == MODE_GROUPED) {
            adapter = new CameraListAdapter(this, this::showCameraVideos);
            adapter.setCamData(Cam.all(Realm.getDefaultInstance()));
        }else{
            adapter = new VideoListAdapter(this, this::downloadAndPlay);
            Cam camera = Cam.get(Realm.getDefaultInstance(),mCameraID);
            if(camera!=null)
                adapter.setVideoData(camera.getVideos());
        }
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
        });

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

        checkDirectory();

    }




    @Override
    protected void onResume() {
        super.onResume();

        adapter.reload();
        serviceIntent = new Intent(this, CryptoCamScanService.class);
        startService(serviceIntent);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
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
        emptyView.setVisibility(adapter.getItemCount() == 0? View.VISIBLE:View.GONE);
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

        String local = v.checkForLocalVideo(path);

        if(local == null) {

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
