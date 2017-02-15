package uk.ac.openlab.cryptocam.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.ac.openlab.cryptocam.R;
import uk.ac.openlab.cryptocam.data.Video;

/**
 * Created by Kyle Montague on 13/02/2017.
 */

public class KeyListAdapter extends RecyclerView.Adapter<KeyListAdapter.ViewHolder>  {


    ArrayList<Video> items;
    KeyListItemListener listener = null;


    public KeyListAdapter(KeyListItemListener listener){
        this.listener = listener;
    }


    public void reloadData(){
        items = new ArrayList<>();
        List<Video> videos = Video.find(Video.class,null,null,null,"timestamp DESC",null);
        if(videos!=null && videos.size() >0) {
            items.addAll(videos);//Video.listAll(Video.class,"timestamp DESC"));
            notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.keylistitem, parent, false);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClicked(parent.indexOfChild(v));
            }
        });
        ViewHolder vh = new ViewHolder(v);
        return vh;

    }

    private void itemClicked(int index) {
        if(listener!=null){
            listener.itemSelected(index);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.title.setText(items.get(position).getTimestamp().toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;



        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.title);
        }
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    public interface KeyListItemListener{

        void itemSelected(int index);
    }
}
