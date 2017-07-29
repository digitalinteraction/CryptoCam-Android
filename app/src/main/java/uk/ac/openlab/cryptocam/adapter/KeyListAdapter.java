package uk.ac.openlab.cryptocam.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.openlab.cryptocam.CryptoCamApplication;
import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;
import uk.ac.openlab.cryptocam.utility.DownloadRequest;
import uk.ac.openlab.cryptocam.utility.DownloadTask;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class KeyListAdapter extends RecyclerView.Adapter<KeyListViewHolder>  {


    ArrayList<Video> items;
    KeyListItemListener listener = null;
    Context context;
    Mode mode = Mode.CAM;

    enum Mode{
        CAM,
        VIDEO
    }


    private final String thumbpath = CryptoCamApplication.directory();

    public KeyListAdapter(Context context, KeyListItemListener listener){
        this.listener = listener;
        this.context = context;
    }


    public void reloadData(){
        mode = Mode.VIDEO;
        setData(Video.find(Video.class,null,null,null,"timestamp DESC",null));
    }

    public void loadCameras(){

        List<Cam> cameras = Cam.find(Cam.class,null,null,null,"name",null);
        ArrayList<Video> videos= new ArrayList<>();
        for(Cam camera:cameras){

            List<Video> tmp =Video.find(Video.class,"cam = ? AND localthumb NOT NULL",new String[]{String.valueOf(camera.getId())},null,"timestamp DESC","1");
            if(tmp.size() == 0)
                tmp =Video.find(Video.class,"cam = ?",new String[]{String.valueOf(camera.getId())},null,"timestamp DESC","1");

            if(tmp.size() > 0)
                videos.add(tmp.get(0));
        }



        mode = Mode.CAM;
        setData(videos);
    }

    public void loadCameraVideos(String cameraID){
        mode = Mode.VIDEO;
        setData(Video.find(Video.class,"cam = ?",new String[]{cameraID},null,"timestamp DESC",null));
    }

    public void setData(List<Video> videos){
        items = new ArrayList<>();
        if(videos!=null && videos.size() >0) {
            items.addAll(videos);//Video.listAll(Video.class,"timestamp DESC"));
            getThumbnails();
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
            listener.itemSelected(index);
        }
    }




    @Override
    public void onBindViewHolder(final KeyListViewHolder holder, int position) {

        String text = items.get(position).getDateString();
        if(mode == Mode.CAM) {
            Cam cam = items.get(position).getCam();
            if (cam != null) {
                text = cam.description();
            }else{
                text = "Unknown";
            }
        }

        holder.title.setText(text);

        holder.interactiveArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClicked(holder.getAdapterPosition());


            }
        });

        //fixme could do in a thread. then call invalidate when it comes back.
        if(items.get(position).localthumb == null && items.get(position).attemptCount < 2) {
            items.get(position).localthumb = items.get(position).checkForLocalThumbnail(thumbpath);
            items.get(position).attemptCount++;
        }

        if(items.get(position).localvideo == null && items.get(position).attemptCount < 2)
            items.get(position).localvideo = items.get(position).checkForLocalVideo(thumbpath);

        if(items.get(position).localthumb != null){
            holder.actionState.setVisibility(View.VISIBLE);
            holder.imageView.setImageURI(Uri.parse(items.get(position).localthumb));
        }else{
            holder.actionState.setVisibility(View.INVISIBLE);
            holder.imageView.setImageResource(R.mipmap.ic_launcher);
        }


        if(mode == Mode.CAM) {
            holder.actionState.setImageResource(R.drawable.ic_linked_camera);
        }else {
            if (items.get(position).localvideo != null || items.get(position).checkForLocalVideo(thumbpath) != null) {
                holder.actionState.setImageResource(R.drawable.ic_play);
            } else {
                holder.actionState.setImageResource(R.drawable.ic_download);
            }
        }
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


    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }



    private void getThumbnails(){
        for(Video video:items){
            String local = video.checkForLocalThumbnail(thumbpath);
            if(local == null){ // we don't have the file. make a request to download it
                Log.d("REQUEST","Getting thumbnail");
                DownloadRequest request = new DownloadRequest(video.getThumbnailUrl(),thumbpath,video.getKey(),video.getIV());
                new DownloadTask(context).execute(request);

            }
        }

    }

    public interface KeyListItemListener{
        void itemSelected(int index);
    }
}
