package uk.ac.openlab.cryptocam.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.crash.FirebaseCrash;

import uk.ac.openlab.cryptocam.BuildConfig;
import uk.ac.openlab.cryptocam.utility.PrefUtils;

/**
 * Created by Kyle Montague on 28/06/2017.
 */

public class CryptoCamAccountManager {

    public static final String AUTHORITY = "uk.ac.openlab.cryptocam.provider";
    public static final String ACCOUNT_TYPE = "uk.ac.openlab.cryptocam";
    public static final String ACCOUNT_NAME = "CryptoCam";
    public static final int SYNC_FREQUENCY = BuildConfig.SYNC_INTERVAL_MINUTES * 60;


    private static final String PREF_SETUP_COMPLETE = "PREF_SETUP_COMPLETE";


    public static Account createSyncAccount(Context context) {

        boolean created = false;
        boolean setupComplete = PrefUtils.shared().init(context).getter().getBoolean(PREF_SETUP_COMPLETE,false);

        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(newAccount, AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(newAccount, AUTHORITY, new Bundle(),SYNC_FREQUENCY);
            created = true;
        }

        if (created || !setupComplete) {
            requestSync(newAccount);
            PrefUtils.shared().setter().putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }

        return newAccount;
    }

    public static void requestPeriodicSync(Account account){
        //Turn on periodic syncing
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);
    }


    public static void requestSync(Account account){
        try {
            Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(account, AUTHORITY, Bundle.EMPTY);
        }catch (Exception e){
            FirebaseCrash.log("Error while requesting manual sync - likely null user.");
        }
    }

}
