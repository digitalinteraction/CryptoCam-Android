package uk.ac.openlab.cryptocam.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import uk.ac.openlab.cryptocam.R;

/**
 * Created by kylemontague on 27/02/2017.
 */

public class KeyListViewHolder extends RecyclerView.ViewHolder {
    TextView title;
    ImageView imageView;
    ImageView actionState;
    View interactiveArea;
    AVLoadingIndicatorView progressView;


    public KeyListViewHolder(View itemView) {
        super(itemView);
        interactiveArea = itemView;
        title = (TextView)itemView.findViewById(R.id.title);
        imageView = (ImageView)itemView.findViewById(R.id.image);
        actionState = (ImageView)itemView.findViewById(R.id.action_state);
        progressView = (AVLoadingIndicatorView)itemView.findViewById(R.id.progress_view);
    }


    public void showProgress(boolean state){
        if(state) {
            actionState.setVisibility(View.GONE);
            progressView.show();
        }
        else {
            actionState.setVisibility(View.VISIBLE);
            progressView.hide();
        }

    }
}

