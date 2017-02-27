package uk.ac.openlab.cryptocam.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.openlab.cryptocam.R;
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
    private final String thumbpath = Environment.getExternalStorageDirectory().getAbsolutePath();

    public KeyListAdapter(Context context, KeyListItemListener listener){
        this.listener = listener;
        this.context = context;
    }


    public void reloadData(){
        items = new ArrayList<>();
        List<Video> videos = Video.find(Video.class,null,null,null,"timestamp DESC",null);
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
        holder.title.setText(text);

        holder.interactiveArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClicked(holder.getAdapterPosition());


            }
        });

        //fixme could do in a thread. then call invalidate when it comes back.
        if(items.get(position).localThumb == null && items.get(position).attemptCount < 2) {
            items.get(position).localThumb = items.get(position).checkForLocalThumbnail(thumbpath);
            items.get(position).attemptCount++;
        }

        if(items.get(position).localVideo == null && items.get(position).attemptCount < 2)
            items.get(position).localVideo = items.get(position).checkForLocalVideo(thumbpath);

        if(items.get(position).localThumb != null){
            holder.actionState.setVisibility(View.VISIBLE);
            holder.imageView.setImageURI(Uri.parse(items.get(position).localThumb));
        }else{
            holder.actionState.setVisibility(View.INVISIBLE);
            holder.imageView.setImageResource(R.mipmap.ic_launcher);
        }



        if(items.get(position).localVideo != null) {
            holder.actionState.setImageResource(R.mipmap.ic_play);
        }else{
            holder.actionState.setImageResource(R.mipmap.ic_download);
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
