package uk.ac.openlab.cryptocam.data;

import android.content.Context;
import android.content.Intent;

import com.orm.SugarRecord;

import java.util.Date;
import java.util.List;

import uk.ac.openlab.cryptocam.utility.CryptoCamPacket;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Video extends SugarRecord {


    String encryption;
    String key;
    String iv;
    Date timestamp;
    String url;
    Cam cam;


    public Video(){

    }


    public final static String DATA_UPDATE_VIDEO = "SUGAR_RECORD.DATA_UPDATE.VIDEO";
    public long saveAndNotify(Context context) {
        long ID =  super.save();
        context.sendBroadcast(new Intent(DATA_UPDATE_VIDEO));
        return ID;
    }

    public Video(CryptoCamPacket packet, String macaddress){
        this.timestamp = new Date();
        this.encryption = packet.encryption;
        this.key = packet.key;
        this.iv = packet.iv;
        this.url = packet.url;
        List<Cam> cams = Cam.find(Cam.class,"macaddress = ?",macaddress.toLowerCase());
        if(cams!=null && cams.size() > 0){
            this.cam = cams.get(0);
        }
    }

    public Video(String encryption, String key, String iv, Date timestamp, String url, Cam cam){
        this.encryption = encryption;
        this.key = key;
        this.iv = iv;
        this.timestamp = timestamp;
        this.url = url;
        this.cam = cam;
    }

    public Video(String encryption, String key, String iv, Date timestamp, String url, Long camID){
        this.encryption = encryption;
        this.key = key;
        this.iv = iv;
        this.timestamp = timestamp;
        this.url = url;
        this.cam = Cam.findById(Cam.class,camID);
    }


    public String getEncryption() {
        return encryption;
    }

    public String getKey() {
        return key;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }

    public Cam getCam() {
        return cam;
    }
}
