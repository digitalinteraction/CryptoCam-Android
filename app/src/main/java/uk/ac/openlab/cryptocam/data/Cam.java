package uk.ac.openlab.cryptocam.data;

import com.orm.SugarRecord;
import java.util.List;

/**
 * Created by Kyle Montague on 27/01/2017.
 */

public class Cam extends SugarRecord {

    private Long id;
    String name;

    public Long getId() {
        return id;
    }

    public Cam(){

    }

    public Cam(String name){
        this.name = name;
    }

    List<Video> getVideos(){
        return Video.find(Video.class,"cam = ?", String.valueOf(getId()));
    }
}
