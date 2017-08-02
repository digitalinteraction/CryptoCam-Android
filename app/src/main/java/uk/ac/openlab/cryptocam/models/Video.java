package uk.ac.openlab.cryptocam.models;

import android.content.Context;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.ac.openlab.cryptocam.services.CryptoCamReceiver;
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


    String localthumb;
    String localvideo;
    String encryption;
    String key;
    String iv;
    Date timestamp;
    String url;
    Cam cam;



    public long saveAndNotify(Context context) {
        long ID =  super.save();
        CryptoCamReceiver.newKey(context);
        return ID;
    }


    public Video(){
        this.localthumb = null;
        this.localvideo = null;
    }

    public Video(CryptoCamPacket packet, String macaddress){
        this.timestamp = new Date();
        this.encryption = packet.encryption;
        this.key = packet.key;
        this.iv = packet.iv;
        this.url = packet.url;
        this.localthumb = null;
        this.localvideo = null;
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
        this.localthumb = null;
        this.localvideo = null;
    }

    public Video(String encryption, String key, String iv, Date timestamp, String url, Long camID){
        this.encryption = encryption;
        this.key = key;
        this.iv = iv;
        this.timestamp = timestamp;
        this.url = url;
        this.localthumb = null;
        this.localvideo = null;
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



    public String checkForLocalVideo(String filepath){
        try {
            URL url = new URL(getVideoUrl());
            String[] parts = url.getFile().split("/");

            File file = new File(filepath,parts[parts.length-1]);
            if(file.exists()) {
                localvideo = file.getAbsolutePath();
                save();
                return file.getAbsolutePath();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String checkForLocalThumbnail(String filepath){
        try {
            URL url = new URL(getThumbnailUrl());

            String[] parts = url.getFile().split("/");

            File file = new File(filepath,parts[parts.length-1]);
            if(file.exists()) {
                localthumb = file.getAbsolutePath();
                save();
                return file.getAbsolutePath();
            }
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

    public String getLocalthumb() {
        return localthumb;
    }

    public void setLocalthumb(String localthumb) {
        this.localthumb = localthumb;
    }

    public String getLocalvideo() {
        return localvideo;
    }

    public void setLocalvideo(String localvideo) {
        this.localvideo = localvideo;
    }


    public static Video latestForCamera(long id){
        List<Video> videos = Video.find(Video.class,"cam = ?",new String[]{String.valueOf(id)},null,"timestamp DESC",null);
        if(videos.size() > 0)
            return videos.get(0);
        return null;
    }
}
