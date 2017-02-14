package uk.ac.openlab.cryptocam.data;

import android.content.Context;
import android.content.Intent;

import com.orm.SugarRecord;
import java.util.List;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Cam extends SugarRecord {

    public String macaddress;
    public String name;


    public final static String DATA_UPDATE_CAM = "SUGAR_RECORD.DATA_UPDATE.CAM";
    public long saveAndNotify(Context context) {
        long ID =  super.save();
        context.sendBroadcast(new Intent(DATA_UPDATE_CAM));
        return ID;
    }

    public Cam(String name, String macaddress){
        this.name = name;
        this.macaddress = macaddress;
    }

    List<Video> getVideos(){
        return Video.find(Video.class,"cam = ?", String.valueOf(getId()));
    }

    public String getMacaddress() {
        return macaddress;
    }

    public String getName() {
        return name;
    }
}
