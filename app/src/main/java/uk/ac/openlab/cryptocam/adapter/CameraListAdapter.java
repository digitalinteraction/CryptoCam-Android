package uk.ac.openlab.cryptocam.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.realm.Realm;
import io.realm.RealmChangeListener;
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

public class CameraListAdapter extends RealmListAdapter  {


    RealmResults<Cam> items;
    RecyclerViewItemClicked listener = null;
    Context context;

    private RealmChangeListener realmChangeListener = o -> {
        //todo might need to load again.
        notifyDataSetChanged();
    };

    private final String thumbpath = CryptoCamApplication.directory();

    public CameraListAdapter(Context context, RecyclerViewItemClicked listener){
        this.listener = listener;
        this.context = context;
    }

    @Override
    public void setCamData(RealmResults<Cam> cameras){
        items = cameras;
        if(items!=null && items.size() >0) {
            items.addChangeListener(realmChangeListener);
            for(Cam cam:items) {
                getThumbnails(cam.getVideosWithThumbnails().first());
            }
            notifyDataSetChanged();
        }
    }

    @Override
    public void reload(){
        if(items!=null)
            items.load();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.keylistitem, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;

    }



    private void itemClicked(int index) {
        if(listener!=null){
            listener.itemSelected(items.get(index).getId(),index);
        }
    }




    @Override
    public void onBindViewHolder(final KeyListViewHolder holder, int position) {


        Cam cam = items.get(holder.getAdapterPosition());
        Video video = cam.getVideosWithThumbnails().first();
        String text = cam.description();

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


        holder.actionState.setImageResource(R.drawable.ic_linked_camera);
        holder.actionState.setVisibility(View.VISIBLE);
        holder.progressView.hide();

    }

    @Override
    public int getItemCount() {
        return items.size();
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
                new DownloadTask(context).execute(request);
            }

    }


}
