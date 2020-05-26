package com.example.mymusicstreamingapp;

import android.os.AsyncTask;

public class NumberOfChunksTask extends AsyncTask<String,Void, Integer> {

    @Override
    protected Integer doInBackground(String... strings) {
        return NetworkUtils.receiveTotalChunkNumber(strings[0]);
    }

    @Override
    protected void onPostExecute(Integer s) {
        super.onPostExecute(s);
    }
}
