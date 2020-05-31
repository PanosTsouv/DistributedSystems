package com.example.mymusicstreamingapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import Utils.Value;

public class DownloadChunkLoader extends AsyncTaskLoader<Value> {
    private Value mChunk;
    private String mSongName;

    private static final String LOG_TAG = DownloadChunkLoader.class.getName();

    DownloadChunkLoader(@NonNull Context context, String songName) {
        super(context);
        mSongName = songName;
    }

    @Override
    protected synchronized void onStartLoading() {
        Log.d(LOG_TAG, getId() + " start");
        if (mChunk != null)
        {
            Log.d(LOG_TAG, "Loader " + getId() + " has already artists so don't need to make again background job");
            deliverResult(mChunk);
        }
        else
        {
            Log.d(LOG_TAG, "Loader " + getId() + " doesn't have results so it makes background job");
            forceLoad();
        }
    }

    @Nullable
    @Override
    public synchronized Value loadInBackground() {
        Log.d(LOG_TAG, getId() + " start back job");
        mChunk = NetworkUtils.downloadChunk(getId());
        Log.d(LOG_TAG, "Loader " + getId() + " HAS DATA");
        return mChunk;
    }

    @Override
    public synchronized void deliverResult(@Nullable Value data) {
        //Log.d(LOG_TAG, "Call deliverResult thread with id" + this.getId());
        mChunk = data;
        super.deliverResult(data);
    }

    String getLoaderSongName()
    {
        return  mSongName;
    }
}
