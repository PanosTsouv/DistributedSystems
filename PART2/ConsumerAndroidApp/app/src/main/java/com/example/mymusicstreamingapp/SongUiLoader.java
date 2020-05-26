package com.example.mymusicstreamingapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.ArrayList;

import static com.example.mymusicstreamingapp.SongActivity.SONG_LIST_LOADER_DEBUG;

public class SongUiLoader extends AsyncTaskLoader<ArrayList<String>> {

    private static final String LOG_TAG = SongUiLoader.class.getName();

    private ArrayList<String> mSongs;

    private String mArtistName;

    SongUiLoader(@NonNull Context context, String artistName) {
        super(context);
        mArtistName = artistName;
        if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call loader constructor");
    }

    @Override
    protected void onStartLoading() {
        if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onStartLoading");
        if (mSongs != null)
        {
            if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Loader has already songs so don't need to make again background job");
            if(NetworkUtils.getSocket() == null)
            {
                forceLoad();
                return;
            }
            deliverResult(mSongs);
        }
        else
        {
            if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Loader doesn't have results so it makes background job");
            forceLoad();
        }
    }

    @Nullable
    @Override
    public ArrayList<String> loadInBackground() {
        if(NetworkUtils.getSocket() == null)
        {
            NetworkUtils.openConnection();
        }
        return NetworkUtils.fetchSongs(mArtistName);
    }

    @Override
    public void deliverResult(@Nullable ArrayList<String> data) {
        if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call deliverResult");
        mSongs = data;
        super.deliverResult(data);
    }
}
