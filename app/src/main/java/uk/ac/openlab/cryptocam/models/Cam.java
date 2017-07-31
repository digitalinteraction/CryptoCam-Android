package uk.ac.openlab.cryptocam.models;

import android.content.Context;
import android.location.Location;

import com.orm.SugarRecord;

import java.util.List;
import java.util.Locale;

import uk.ac.openlab.cryptocam.services.CryptoCamReceiver;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Cam extends SugarRecord {

    public String macaddress;
    public String name;
    public String version;
    public String mode;
    public String location_type;

    public Location location;


    public long saveAndNotify(Context context) {
        long ID =  super.save();
        CryptoCamReceiver.newCamera(context,ID);
        return ID;
    }


    public Cam(){
        this.name = null;
        this.macaddress = null;

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

    public static Cam fromMacaddress(String macaddress){
        List<Cam> cams = Cam.find(Cam.class,"macaddress = ?", new String[]{macaddress.toLowerCase()});
        Cam cam = null;
        if(cams.size() == 0) {
            cam = new Cam(null, macaddress);
            cam.save();
        }
        else
            cam = cams.get(0);

        return cam;
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


    public String description(){
        if(location_type == null)
            location_type = "";
        return String.format(Locale.getDefault(),"%s\n%s",name,location_type);
    }

    public boolean hasDetails() {
        return name != null && location_type != null && mode != null && version != null;
    }
}
