package uk.ac.openlab.cryptocam.utility;

/**
 * Created by kylemontague on 15/02/2017.
 */

public class DownloadRequest{

    String url;
    String path;
    String filename;


    public DownloadRequest(String url, String path, String targetFilename, String fileExt){
        this.url = String.format("%s%s",url,fileExt);
        this.path = path;
        this.filename = targetFilename;
    }
}
