package uk.ac.openlab.cryptocam.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.adapter.KeyListAdapter;
import uk.ac.openlab.cryptocam.data.Video;
import uk.ac.openlab.cryptocam.utility.BLE;

public class ScanningActivity extends AppCompatActivity {


    BLE mBle;
    Button button;

    RecyclerView list;
    KeyListAdapter adapter;


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

        adapter = new KeyListAdapter();
        adapter.reloadData();
        list.setAdapter(adapter);

        list.invalidate();




        // Quick permission check
        int permissionCheck = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");

            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        adapter.reloadData();
        this.registerReceiver(dataUpdateReceiver,dataUpdateFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(dataUpdateReceiver);
    }
}
