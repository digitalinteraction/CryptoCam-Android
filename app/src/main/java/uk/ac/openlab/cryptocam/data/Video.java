package uk.ac.openlab.cryptocam.data;

import com.orm.SugarRecord;

import java.util.Date;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Video extends SugarRecord {

    private Long id;
    String encryption;
    String key;
    Date timestamp;
    String url;
    Cam cam;


    public Video(){

    }

    public Video(String encryption, String key, Date timestamp, String url, Cam cam){
        this.encryption = encryption;
        this.key = key;
        this.timestamp = timestamp;
        this.url = url;
        this.cam = cam;
    }

    public Video(String encryption, String key, Date timestamp, String url, Long camID){
        this.encryption = encryption;
        this.key = key;
        this.timestamp = timestamp;
        this.url = url;
        this.cam = Cam.findById(Cam.class,camID);
    }

    public Long getId() {
        return id;
    }
}
