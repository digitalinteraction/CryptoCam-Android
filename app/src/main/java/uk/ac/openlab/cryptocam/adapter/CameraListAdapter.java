package uk.ac.openlab.cryptocam.adapter;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class CameraListAdapter extends RealmRecyclerViewAdapter<Cam, KeyListViewHolder> {


    private static final String TAG = "CameraAdapter";
    RecyclerViewItemClicked listener = null;
    private String thumbpath;

    private RealmChangeListener realmChangeListener = o -> {
        Log.d(TAG,"CAMERA LIST CHANGED");
        notifyDataSetChanged();
    };


    public CameraListAdapter(@Nullable OrderedRealmCollection<Cam> data, boolean autoUpdate, RecyclerViewItemClicked listener) {
        super(data, autoUpdate);
        this.listener = listener;
        this.thumbpath = CryptoCamApplication.directory();

    }

    public CameraListAdapter(@Nullable OrderedRealmCollection<Cam> data, boolean autoUpdate) {
        super(data, autoUpdate);
        this.thumbpath = CryptoCamApplication.directory();
    }

    @Override
    public void updateData(@Nullable OrderedRealmCollection<Cam> data) {
        super.updateData(data);
        if(getData()!=null) {
            int index = 0;
            for (Cam cam : getData()) {
                RealmResults<Video> videos = cam.getVideosWithThumbnails();
                if (videos.size() > 0) {
                    getThumbnails(videos.first(),index);
                }
                index++;
            }
        }
    }



    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.keylistitem, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;

    }



    private void itemClicked(int index) {
        if(listener!=null && getData()!=null){
            listener.itemSelected(getData().get(index).getId(),index);
        }
    }




    @Override
    public void onBindViewHolder(final KeyListViewHolder holder, int position) {

        Cam cam = getItem(holder.getAdapterPosition());
        assert cam != null;

        RealmResults<Video> videos = cam.getVideosWithThumbnails();
        Video video = null;
        if(videos.size() == 0){
            videos = cam.getVideos();
        }

        if(videos.size() >0){
            video = videos.first();
        }


        String text = cam.description();
        holder.title.setText(text);

        holder.interactiveArea.setOnClickListener(v -> itemClicked(holder.getAdapterPosition()));
        if(video!=null) {
            if (video.getLocalthumb() == null) {
                getThumbnails(video, holder.getAdapterPosition());
            }

            if (video.getLocalthumb() != null && new File(video.getLocalthumb()).exists()) {
                holder.actionState.setVisibility(View.VISIBLE);
                holder.imageView.setImageURI(Uri.parse(video.getLocalthumb()));
            } else {
                holder.actionState.setVisibility(View.INVISIBLE);
                holder.imageView.setImageResource(R.mipmap.ic_launcher);
            }

        }

//        holder.actionState.setImageResource(R.drawable.ic_linked_camera);
        holder.actionState.setVisibility(View.GONE);
        holder.progressView.hide();

    }



    public class ViewHolder extends KeyListViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);

        }
    }





    //only do on the single camera view
    private void getThumbnails(Video video, int index){

        if(video.getLocalthumb()!= null && new File(video.getLocalthumb()).exists())
            return;


        final String id = video.getId();
        DownloadRequest request = new DownloadRequest(video.getThumbnailUrl(), thumbpath, video.getKey(), video.getIV());
        new DownloadTask(new DownloadTask.DownloadTaskProgress(){

            @Override
            public void onProgressUpdate(int progress) {

            }

            @Override
            public void onDownloadComplete(boolean successful, String uri) {
                if(successful) {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        Video v = realm.where(Video.class).equalTo("id", id).findFirst();
                        v.setLocalthumb(uri);
                    });
                }
            }
        }).execute(request);
    }


}
