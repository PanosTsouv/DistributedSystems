package com.example.mymusicstreamingapp;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;


public class SongNameAdapter extends ArrayAdapter<String> {

    private static final boolean DEBUG = false;

    private static final String LOG_TAG = SongNameAdapter.class.getName();

    private OnListItemDownloadClick onClickDownloadListener;

    private OnListItemPlayClick onClickPlayListener;

    private SingleClickListener operationListener = new SingleClickListener() {
        @Override
        void performClick(View v, int performOperation) {
            performOperation(v, (int)v.getTag(R.layout.song_list_item));
        }
    };

    void setOnItemClickPlayListener(OnListItemPlayClick onClickListener)
    {
        onClickPlayListener = onClickListener;
    }

    void setOnItemClickDownloadListener(OnListItemDownloadClick onClickListener)
    {
        onClickDownloadListener = onClickListener;
    }


    SongNameAdapter(@NonNull Context context, int resource, @NonNull ArrayList<String> songs) {
        super(context, resource, songs);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if(convertView == null)
        {
            if (DEBUG)
                Log.d(LOG_TAG,"Inflate list's item view");
            listItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false);
        }

        if (DEBUG)
            Log.d(LOG_TAG,"Find the right artist");
        String current = getItem(position);

        if (DEBUG)
            Log.d(LOG_TAG,"Initialize artist's view with specific artist from position" + position);
        TextView songName = listItemView.findViewById(R.id.song_name);
        songName.setText(current);

        ImageButton play = listItemView.findViewById(R.id.play_button);
        ImageButton download = listItemView.findViewById(R.id.download_button);

        play.setTag(position);
        play.setTag(R.layout.song_list_item,0);
        download.setTag(position);
        download.setTag(R.layout.song_list_item,1);

        play.setImageResource(R.drawable.round_play_circle_outline_black_24);
        download.setImageResource(R.drawable.round_save_alt_24);

        play.setOnClickListener(operationListener);

        download.setOnClickListener(operationListener);

        return listItemView;
    }

    private void performOperation(View v, int performOperation)
    {
        if(performOperation == 0) {
            int positionOfDownloadItem = (Integer) v.getTag();
            onClickPlayListener.onListItemPlayClick(getItem(positionOfDownloadItem));
        }
        else if(performOperation == 1)
        {
            int positionOfDownloadItem = (Integer) v.getTag();
            onClickDownloadListener.onListItemDownloadClick(getItem(positionOfDownloadItem));
        }
    }

    public interface OnListItemDownloadClick{
        void onListItemDownloadClick(String songName);
    }

    public interface OnListItemPlayClick{
        void onListItemPlayClick(String songName);
    }

    public abstract static class SingleClickListener implements View.OnClickListener {

        int defaultInterval;
        private long lastTimeClicked = 0;

        SingleClickListener() {
            this(1000);
        }

        SingleClickListener(int minInterval) {
            this.defaultInterval = minInterval;
        }

        @Override
        public void onClick(View v) {
            if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                return;
            }
            lastTimeClicked = SystemClock.elapsedRealtime();
            performClick(v, (int)v.getTag(R.layout.song_list_item));
        }

        abstract void performClick(View v, int performOperation);

    }
}
