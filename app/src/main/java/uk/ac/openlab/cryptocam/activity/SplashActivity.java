package uk.ac.openlab.cryptocam.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

import uk.ac.openlab.cryptocam.services.CryptoCamAccountManager;

/**
 * Created by Kyle Montague on 30/07/2017.
 */

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = new Intent(this, ScanningActivity.class);
        startActivity(intent);
        CryptoCamAccountManager.createSyncAccount(getApplicationContext());
        FirebaseApp.initializeApp(this);



        finish();
    }
}
