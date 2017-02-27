package uk.ac.openlab.cryptocam.models;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.ac.openlab.cryptocam.utility.CryptoCamPacket;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Video extends SugarRecord {

    @Ignore
    private static final String VIDEO_FORMAT = ".mp4";
    @Ignore
    private static final String THUMBNAIL_FORMAT = ".jpg";
    @Ignore
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM - hh:mm:ss",Locale.ENGLISH);


    @Ignore
    public int attemptCount = 0;

    @Ignore
    public String localThumb = null;

    @Ignore
    public String localVideo = null;

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
        List<Cam> cams = Cam.find(Cam.class,"macaddress = ?",""+macaddress.toLowerCase());
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



    public String getIV(){
        return iv;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getVideoUrl() {
        return url+VIDEO_FORMAT;
    }

    public String getThumbnailUrl() {
        return url+THUMBNAIL_FORMAT;
    }


    //todo hardcode the file path to search in.
    public String checkForLocalVideo(String filepath){
        try {
            URL url = new URL(getVideoUrl());
            File file = new File(filepath,url.getFile());
            Log.d("FILE",file.getAbsolutePath());
            if(file.exists())
                return file.getAbsolutePath();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }


    //todo hardcode the file path to search in.
    public String checkForLocalThumbnail(String filepath){
        try {
            URL url = new URL(getThumbnailUrl());


            File file = new File(filepath,url.getFile());
            Log.d("FILE",file.getAbsolutePath());
            if(file.exists())
                 return file.getAbsolutePath();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDateString(){
        return simpleDateFormat.format(timestamp);
    }

    public Cam getCam() {
        return cam;
    }



}
