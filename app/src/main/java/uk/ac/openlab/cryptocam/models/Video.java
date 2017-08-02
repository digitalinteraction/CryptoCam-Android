package uk.ac.openlab.cryptocam.models;

import com.orm.dsl.Ignore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;
import uk.ac.openlab.cryptocam.utility.CryptoCamPacket;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Video extends RealmObject {


    @PrimaryKey
    public String id = UUID.randomUUID().toString();

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






    public Video(){
        this.localthumb = null;
        this.localvideo = null;
    }

    public Video(CryptoCamPacket packet){
        this.timestamp = new Date();
        this.encryption = packet.encryption;
        this.key = packet.key;
        this.iv = packet.iv;
        this.url = packet.url;
        this.localthumb = null;
        this.localvideo = null;

    }

    public Video(String encryption, String key, String iv, Date timestamp, String url, Cam cam){
        this.encryption = encryption;
        this.key = key;
        this.iv = iv;
        this.timestamp = timestamp;
        this.url = url;
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


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String checkForLocalVideo(String filepath){
        try {
            URL url = new URL(getVideoUrl());
            String[] parts = url.getFile().split("/");

            File file = new File(filepath,parts[parts.length-1]);
            if(file.exists()) {
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


//    public static Video latestForCamera(long id){
//        List<Video> videos = Video.find(Video.class,"cam = ?",new String[]{String.valueOf(id)},null,"timestamp DESC",null);
//        if(videos.size() > 0)
//            return videos.get(0);
//        return null;
//    }

    public static RealmResults<Video> withoutThumbnails(Realm realm){
        return realm.where(Video.class).isNull("localthumb").findAllSorted("timestamp", Sort.ASCENDING);
    }


    public static Video get(Realm realm, String id) {
        return realm.where(Video.class).equalTo("id",id).findFirst();
    }
}
