package com.example.mymusicstreamingapp;

import android.os.AsyncTask;

import java.util.ArrayList;

public class OpenConnectionFromStartTask extends AsyncTask<String ,Void, Integer> {

    @Override
    protected Integer doInBackground(String... data) {
        if (NetworkUtils.getSocket() == null)
        {
            NetworkUtils.openConnection();
            ArrayList<String> songs = NetworkUtils.fetchSongs(data[0]);
            if (songs != null && !songs.isEmpty())
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
        return 2;

    }

    @Override
    protected void onPostExecute(Integer s) {
        super.onPostExecute(s);
    }
}
