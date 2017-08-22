package uk.ac.openlab.cryptocam.models;

import android.util.Log;

import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Cam extends RealmObject {

    private static final String TAG = "Cam";
    @PrimaryKey
    public String id = UUID.randomUUID().toString();

    public String macaddress;
    public String name;
    public String version;
    public String mode;
    public String location;

    public long lastseen;
    public long updatedOn = System.currentTimeMillis();


    public RealmList<Video> videos;


    public Cam(){
        this.videos = new RealmList<>();
        this.lastseen = System.currentTimeMillis();
    }

    public Cam(String name, String macaddress){
        this.name = name;
        this.macaddress = macaddress.toLowerCase();
        this.videos = new RealmList<>();
        this.lastseen = System.currentTimeMillis();
    }


    public static long lastUpdated(Realm realm, String macaddress){
        long lastUpdated = Long.MAX_VALUE;
        Cam cam = existingFromMacaddress(realm,macaddress);
        if(cam!=null)
            lastUpdated = cam.updatedOn;
        return lastUpdated;
    }


    public static boolean exists(Realm realm, String macaddress){
        int count = (int)realm.where(Cam.class).equalTo("macaddress",macaddress.toLowerCase()).count();
        Log.d(TAG,"EXISTING: "+count);
        return (count != 0);
    }

    public static Cam fromMacaddress(Realm realm, String macaddress){
        Cam cam = realm.where(Cam.class).equalTo("macaddress",macaddress.toLowerCase()).findFirst();

        if(cam == null) {
            realm.beginTransaction();
            cam = new Cam(null, macaddress);
            realm.insert(cam);
            realm.commitTransaction();
        }
        return cam;
    }

    public static Cam existingFromMacaddress(Realm realm, String macaddress){
        return realm.where(Cam.class).equalTo("macaddress",macaddress.toLowerCase()).findFirst();
    }


    public void updated(){
        this.updatedOn = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMacaddress(String macaddress) {
        this.macaddress = macaddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public RealmResults<Video> getVideos(){
        return videos.sort("timestamp", Sort.DESCENDING);
    }


    public RealmResults<Video> getVideosWithThumbnails(){
        return videos.where().isNotNull("localthumb").findAll().sort("timestamp", Sort.DESCENDING);
    }

    public String getMacaddress() {
        return macaddress;
    }

    public String getName() {
        return name;
    }


    public String description(){
        return String.format(Locale.getDefault(),"%s\n%s",getName(), getLocation());
    }

    public boolean hasDetails() {
        return name != null && location != null && mode != null && version != null;
    }


    public static RealmResults<Cam> all(Realm realm){
        return realm.where(Cam.class).findAllSorted("lastseen",Sort.DESCENDING);
    }

    public static long total(Realm realm){
        return realm.where(Cam.class).count();
    }

    public static Cam get(Realm realm, String id) {
        return realm.where(Cam.class).equalTo("id",id).findFirst();
    }


    public static RealmResults<Cam> recentlySeen(Realm realm, long interval){
        return realm.where(Cam.class).greaterThanOrEqualTo("lastseen",System.currentTimeMillis()-interval).findAll();
    }


}
