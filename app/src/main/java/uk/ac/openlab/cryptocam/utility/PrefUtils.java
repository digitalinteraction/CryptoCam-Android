package uk.ac.openlab.cryptocam.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Kyle Montague on 21/06/2017.
 */

public class PrefUtils {


    public static String IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";
    public static String USER_ACCOUNT_ID = "USER_ACCOUNT_ID";
    public static String USER_ACCOUNT_COMPLETE = "USER_ACCOUNT_COMPLETE";



    private static PrefUtils sharedInstance;

    public static PrefUtils shared(){
        if(sharedInstance == null)
            sharedInstance = new PrefUtils();
        return sharedInstance;
    }


    private SharedPreferences sharedPreferences;

    public PrefUtils init(Context context){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return this;
    }

    public SharedPreferences getter(){
        return sharedPreferences;
    }

    public SharedPreferences.Editor setter(){
        return getter().edit();
    }



    public static void resetAccount(){
        SharedPreferences.Editor editor = PrefUtils.shared().setter();
        editor.putString(PrefUtils.USER_ACCOUNT_ID,null).apply();
        editor.putBoolean(PrefUtils.IS_FIRST_LAUNCH, true).apply();
        editor.putBoolean(USER_ACCOUNT_COMPLETE,false).apply();
    }

    public static boolean accountSetup(){
        return (PrefUtils.shared().getter().getBoolean(USER_ACCOUNT_COMPLETE,false) && (PrefUtils.shared().getter().getString(PrefUtils.USER_ACCOUNT_ID,null) != null));
    }
}
