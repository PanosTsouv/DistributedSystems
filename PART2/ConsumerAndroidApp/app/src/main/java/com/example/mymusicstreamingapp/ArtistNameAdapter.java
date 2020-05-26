package com.example.mymusicstreamingapp;

import Utils.ArtistName;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ArtistNameAdapter extends ArrayAdapter<ArtistName> {

    private static final boolean DEBUG = false;

    private static final String LOG_TAG = ArtistNameAdapter.class.getName();

    ArtistNameAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ArtistName> artists) {
        super(context, resource, artists);
        if (DEBUG) Log.d(LOG_TAG,"Call constructor");
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if(convertView == null)
        {
            if (DEBUG) Log.d(LOG_TAG,"Inflate list's item view");
            listItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        }

        if (DEBUG) Log.d(LOG_TAG,"Find the right artist");
        ArtistName current = getItem(position);

        if (DEBUG) Log.d(LOG_TAG,"Initialize artist's view with specific artist from position" + position);
        TextView artistName = listItemView.findViewById(R.id.artist_name);
        assert current != null;
        artistName.setText(current.getArtistName());

        return listItemView;
    }
}
