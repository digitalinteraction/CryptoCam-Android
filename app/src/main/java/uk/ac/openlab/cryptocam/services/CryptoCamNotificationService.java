package uk.ac.openlab.cryptocam.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.activity.ScanningActivity;

/**
 * Created by kylemontague on 27/02/2017.
 */

public class CryptoCamNotificationService {


    static int NOTIFICATION_ID = R.string.app_name;

    static Notification getNotification(Context context){

        Intent intent = new Intent(context, ScanningActivity.class);//todo need to swap out for the correct activity
        PendingIntent pendingIntent = PendingIntent.getActivity(context,(int)System.currentTimeMillis(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

//        Intent finishIntent = new Intent(DataCollectionBroadcastReceiver.ACTION_DATA_COLLECTION);
//        finishIntent.putExtra(DataCollectionBroadcastReceiver.EXTRA_ACTIVATE_SERVICE,false);
//        PendingIntent pendingFinish = PendingIntent.getBroadcast(context, (int)System.currentTimeMillis()+1,finishIntent,PendingIntent.FLAG_UPDATE_CURRENT);
//
//        Intent audioIntent = new Intent(context,AudioRecorderActivity.class);
//        PendingIntent pendingAudio = PendingIntent.getActivity(context,(int)System.currentTimeMillis()+2,audioIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Listening for video keys")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentIntent(pendingIntent)
//                .addAction(R.drawable.ic_done_white_24px, "Finished", pendingFinish) //todo show a dialog box, are you sure you want to quit
//                .addAction(R.drawable.ic_record, "Record Audio", pendingAudio)
                .build();

        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return notification;
    }


    public static void showNotification(Context context){
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID,getNotification(context));
    }

    public static void dismissNotification(Context context){
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);

    }


    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
