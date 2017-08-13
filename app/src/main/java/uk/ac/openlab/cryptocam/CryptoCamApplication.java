package uk.ac.openlab.cryptocam;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import uk.ac.openlab.cryptocam.models.ModelMigration;
import uk.ac.openlab.cryptocam.utility.PrefUtils;

/**
 * Created by Kyle Montague on 14/02/2017.
 */

public class CryptoCamApplication extends Application {


    static String directory = "";
    static Context context;

    public static Context getContext() {
        return context;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        Realm.init(getApplicationContext());
        RealmConfiguration config = new RealmConfiguration.Builder().schemaVersion(1).migration(new ModelMigration()).build();
        Realm.setDefaultConfiguration(config);
        directory = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APP_NAME;



        File f = new File(directory);
        f.mkdirs();
        f.setWritable(true);
        f.setReadable(true,true);


        context = getApplicationContext();

        try {
            File dir = new File(directory());
            if (!dir.exists())
                dir.mkdirs();
        }catch (Exception e){
            FirebaseCrash.log(e.getMessage());
        }

        PrefUtils.shared().init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    public static String directory(){
        return directory;
    }
}
