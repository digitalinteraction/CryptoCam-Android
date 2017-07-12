package uk.ac.openlab.cryptocam;

import android.app.Application;

import com.orm.SugarContext;

/**
 * Created by Kyle Montague on 14/02/2017.
 */

public class CryptoCamApplication extends Application {




    @Override
    public void onCreate() {
        super.onCreate();

        SugarContext.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }
}
