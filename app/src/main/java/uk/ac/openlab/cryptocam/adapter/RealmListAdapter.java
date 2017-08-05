package uk.ac.openlab.cryptocam.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import io.realm.OrderedRealmCollection;
import io.realm.RealmResults;
import uk.ac.openlab.cryptocam.models.Cam;
import uk.ac.openlab.cryptocam.models.Video;

/**
 * Created by Kyle Montague on 02/08/2017.
 */

public class RealmListAdapter extends RecyclerView.Adapter <KeyListViewHolder>{


    @Override
    public KeyListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(KeyListViewHolder holder, int position) {

    }


    @Override
    public int getItemCount() {
        return 0;
    }

    public void reload(){

    }



    public void setVideoData(RealmResults<Video> data){

    }

    public void setCamData(OrderedRealmCollection<Cam> all) {
    }
}
