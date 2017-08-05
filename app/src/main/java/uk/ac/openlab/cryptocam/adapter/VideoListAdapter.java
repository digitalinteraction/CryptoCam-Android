package uk.ac.openlab.cryptocam.adapter;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class VideoListAdapter extends RealmRecyclerViewAdapter<Video, KeyListViewHolder> {


    RecyclerViewItemClicked listener = null;
    private String thumbpath = CryptoCamApplication.directory();

    public VideoListAdapter(@Nullable OrderedRealmCollection<Video> data, boolean autoUpdate, RecyclerViewItemClicked listener) {
        super(data, autoUpdate);
        this.listener = listener;
    }

    @Override
    public void updateData(@Nullable OrderedRealmCollection<Video> data) {
        super.updateData(data);
        if(data!=null && data.size() >0) {
            assert getData() != null;
            for(Video video:getData().where().isNotNull("localthumb").findAll()) {
                getThumbnails(video);
            }
            notifyDataSetChanged();
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.keylistitem, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;

    }


    private void itemClicked(int index) {
        if(listener!=null){
            listener.itemSelected(getItem(index).getId(),index);
        }
    }




    @Override
    public void onBindViewHolder(final KeyListViewHolder holder, int position) {



        Video video = getItem(holder.getAdapterPosition());
        assert video != null;
        String text = video.getDateString();

        holder.title.setText(text);
        holder.interactiveArea.setOnClickListener(v -> itemClicked(holder.getAdapterPosition()));

        if(video.getLocalthumb() == null) {
            video.checkForLocalThumbnail(thumbpath);
        }

        if(video.getLocalvideo() == null)
            video.checkForLocalVideo(thumbpath);

        if(video.getLocalthumb() != null){
            holder.actionState.setVisibility(View.VISIBLE);
            holder.imageView.setImageURI(Uri.parse(video.getLocalthumb()));
        }else{
            holder.actionState.setVisibility(View.INVISIBLE);
            holder.imageView.setImageResource(R.mipmap.ic_launcher);
        }


        holder.actionState.setImageResource(video.getLocalvideo() == null?R.drawable.ic_download: R.drawable.ic_play);
        holder.actionState.setVisibility(View.VISIBLE);
        holder.progressView.hide();

    }



    public class ViewHolder extends KeyListViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);

        }
    }





    //only do on the single camera view
    private void getThumbnails(Video video){
            final String local = video.checkForLocalThumbnail(thumbpath);
            final String id = video.getId();
            if(local == null){ // we don't have the file. make a request to download it
                Realm.getDefaultInstance().executeTransaction(realm -> {
                    Video v = realm.where(Video.class).equalTo("id",id).findFirst();
                    v.setLocalthumb(local);
                });
                DownloadRequest request = new DownloadRequest(video.getThumbnailUrl(),thumbpath,video.getKey(),video.getIV());
                new DownloadTask().execute(request);
            }

    }


}
