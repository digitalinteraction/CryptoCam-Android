package uk.ac.openlab.cryptocam.models;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.orm.SugarRecord;

import java.util.List;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Cam extends SugarRecord {

    public String macaddress;
    public String name;

    public Location location;


    public final static String DATA_UPDATE_CAM = "SUGAR_RECORD.DATA_UPDATE.CAM";
    public long saveAndNotify(Context context) {
        long ID =  super.save();
        context.sendBroadcast(new Intent(DATA_UPDATE_CAM));
        return ID;
    }


    public Cam(){
        this.name = "";
        this.macaddress = "";

    }

    public Cam(String name, String macaddress){
        this.name = name;
        this.macaddress = macaddress.toLowerCase();
    }


    public Cam(String name, String macaddress, Location location){
        this.name = name;
        this.macaddress = macaddress.toLowerCase();
        this.location = location;
    }
    public static boolean exists(String macaddress){
        long count = Cam.count(Cam.class,"macaddress = ?", new String[]{macaddress.toLowerCase()});
        return (count > 0);
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
