package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.KeyListAdapter;
import uk.ac.openlab.cryptocam.data.Video;
import uk.ac.openlab.cryptocam.utility.BLE;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

public class ScanningActivity extends AppCompatActivity implements KeyListAdapter.KeyListItemListener{


    BLE mBle;
    Button button;

    RecyclerView list;
    KeyListAdapter adapter;

    //TODO needs to be put into the config class.
    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/CryptoCam/";


    IntentFilter dataUpdateFilter = new IntentFilter(Video.DATA_UPDATE_VIDEO);
    BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case Video.DATA_UPDATE_VIDEO:
                    adapter.reloadData();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);




        mBle = new BLE(this);
        mBle.init();


        button = (Button)findViewById(R.id.scan_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                adapter.reloadData();
                if(mBle.isScanning()){
                    mBle.stop();
                    button.setText(R.string.scan_start);
                }else{
                    mBle.start();
                    button.setText(R.string.scan_stop);
                }
            }
        });


        list = (RecyclerView)findViewById(R.id.video_recycleview);


        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(llm);

        adapter = new KeyListAdapter(this);
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
    public void itemSelected(int index) {
        Log.d("RecycleView","Item: "+index);

        Video v = Video.findById(Video.class,adapter.getItemId(index));
        Log.d("RecycleView",String.format("id: %d time: %s",v.getId(),v.getTimestamp().toString()));

        DownloadRequest request = new DownloadRequest(v.getUrl(),path,"tmp"+ System.currentTimeMillis(),".mp4");
        new DownloadTask(this).execute(request);

    }



    private boolean checkDirectory(){
        File f = new File(path);
        if(!f.exists())
            return f.mkdirs();


        return true;
    }
}
