package com.example.mymusicstreamingapp;

import android.os.AsyncTask;

public class UnregisterTask extends AsyncTask<Boolean,Void, Void> {
    @Override
    protected Void doInBackground(Boolean... request) {
        if(request[0]) {
            NetworkUtils.unregister();
        }
        else
        {
            NetworkUtils.disconnect();
        }
        return null;
    }
}
