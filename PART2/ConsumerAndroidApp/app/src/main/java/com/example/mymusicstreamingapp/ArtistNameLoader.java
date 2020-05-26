package com.example.mymusicstreamingapp;

import android.content.Context;
import android.util.Log;

import Utils.ArtistName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.ArrayList;

import static com.example.mymusicstreamingapp.MainActivity.ARTIST_LOADER_DEBUG;

public class ArtistNameLoader extends AsyncTaskLoader<ArrayList<ArtistName>> {

    private static final String LOG_TAG = ArtistNameLoader.class.getName();

    private ArrayList<String> mConsumerInfo;

    private ArrayList<ArtistName> mArtists;

    ArtistNameLoader(@NonNull Context context, ArrayList<String> consumerInfo) {
        super(context);
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call loader constructor");
        mConsumerInfo = consumerInfo;
    }

    @Override
    protected void onStartLoading() {
        Log.d(LOG_TAG, "Call onStartLoading");
        if (mArtists != null)
        {
            if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Loader has already artists so don't need to make again background job");
            deliverResult(mArtists);
        }
        else
        {
            if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Loader doesn't have results so it makes background job");
            forceLoad();
        }
    }

    @Nullable
    @Override
    public ArrayList<ArtistName> loadInBackground() {
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call loadInBackground");

        // Perform the network request, parse the response, and extract a list of artist.
        mArtists = NetworkUtils.fetchArtists(mConsumerInfo);
        return mArtists;
    }

    @Override
    public void deliverResult(@Nullable ArrayList<ArtistName> data) {
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call deliverResult");
        mArtists = data;
        super.deliverResult(data);
    }
}
