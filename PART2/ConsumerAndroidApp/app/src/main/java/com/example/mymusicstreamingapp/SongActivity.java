package com.example.mymusicstreamingapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import Utils.Value;

/*
 * package private keys
 * define at MainActivity
 */
import static com.example.mymusicstreamingapp.MainActivity.ARTIST_NAME_KEY;
import static com.example.mymusicstreamingapp.MainActivity.CLASS_NAME_KEY;
import static com.example.mymusicstreamingapp.MainActivity.LIFECYCLE_DEBUG;

public class SongActivity extends AppCompatActivity implements SongNameAdapter.OnListItemDownloadClick ,SongNameAdapter.OnListItemPlayClick{

    private static final String LOG_TAG = SongActivity.class.getName();

    static final boolean SONG_LIST_LOADER_DEBUG = true;

    /**
     * Constant value for the song loader ID and download loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     * Others download loaders use DOWNLOAD_LOADER_ID + chunks(i) of specific song
     * Every chunk has his own loader
     */
    private static final int SONG_LOADER_ID = 2;
    private static final int DOWNLOAD_LOADER_ID = 3;

    /**
     * package private keys
     */
    static final String SONG_NAME_KEY = "songName";
    static final String CHUNKS_NUMBER_KEY = "chunksNumber";

    /**
     * empty textView to initialize activity when list with artists is empty
     */
    private TextView mEmptyStateTextView;

    /**
     * List View initialization
     * mAdapter store song list data
     * mLoaderManager initialize loaders to retrieve data from server
     */

    private ListView mSongListView;
    private SongNameAdapter mAdapter;
    private LoaderManager mLoaderManager;
    private DownloadChunkLoader firstLoaderOfChunks;
    private boolean requestHasChanged = false;

    /**
     * use this variables to write song at external storage
     */
    public FileOutputStream streamID;

    public String path;

    RelativeLayout progressDialog;


    Integer result = 0;

    int count = 0;

    boolean clickButton = false;

    boolean onRestoreInstance = false;

    public static Toast mToast = null;


    /**
     * Song List loader callback listener
     */
    private LoaderManager.LoaderCallbacks<ArrayList<String>> dataResultSongListLoaderListener
            = new LoaderManager.LoaderCallbacks<ArrayList<String>>()
    {
        @NonNull
        @Override
        public Loader<ArrayList<String>> onCreateLoader(int id, @Nullable Bundle args) {
            if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onCreateLoader");
            Log.d(LOG_TAG, SongActivity.this.getIntent().getStringExtra(ARTIST_NAME_KEY) + "");
            return new SongUiLoader(SongActivity.this, SongActivity.this.getIntent().getStringExtra(ARTIST_NAME_KEY));
        }

        @Override
        public void onLoadFinished(@NonNull Loader<ArrayList<String>> loader, ArrayList<String> songs) {
            if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onLoadFinished");
            ProgressBar progressView = findViewById(R.id.loading_spinner1);
            progressView.setVisibility(View.GONE);

            // Clear the adapter of previous songs data
            mAdapter.clear();

            // If there is a valid list of {@link songs}, then add them to the adapter's
            // data set. addAll() method contains notifyDataSetChanged() method which trigger ListView to update.
            if (songs != null && !songs.isEmpty()) {
                if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Update list view with song list");
                mAdapter.addAll(songs);
            }
            else
            {
                if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Set the empty view");
                // Set empty state text to display "No songs found."
                mEmptyStateTextView.setText(R.string.no_songs);
                mEmptyStateTextView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<ArrayList<String>> loader) {
            // Loader reset, so we can clear out our existing data.
            //clear() method contains notifyDataSetChanged() method which trigger ListView to update.
            if(SONG_LIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onLoaderReset");
            mAdapter.clear();
        }
    };


    /**
     * Chunk download loader callback listener
     * here loaders return chunks , we write them to external storage and update UI with progress bar
     */
    private LoaderManager.LoaderCallbacks<Value> dataResultDownloadChunksLoaderListener
            = new LoaderManager.LoaderCallbacks<Value>()
    {
        @NonNull
        @Override
        public synchronized Loader<Value> onCreateLoader(int id, Bundle args) {
            Log.d(LOG_TAG, "Call onCreateLoader type of DownloadChunksLoader");
            if (id == DOWNLOAD_LOADER_ID)
            {
                firstLoaderOfChunks = new DownloadChunkLoader(SongActivity.this, args.getString(SONG_NAME_KEY));
                return firstLoaderOfChunks;
            }
            return new DownloadChunkLoader(SongActivity.this, args.getString(SONG_NAME_KEY));
        }

        @Override
        public synchronized void onLoadFinished(@NonNull Loader<Value> loader, Value data) {
            Log.d(LOG_TAG, loader.getId() + " type of DownloadChunksLoader call ONLOADFINISH JOB --------------------");
            if (data == null) {
                Log.d(LOG_TAG, "Null Value, problem occurred with server");
                closeOutputFileStream();
                deleteExistingFile();
                return;
            }
            if(loader.getId() == DOWNLOAD_LOADER_ID) {
                deleteExistingFile();
                try {
                    streamID = StorageUtils.createASongStream(data.getMusicFile().getTrackName(), SongActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                streamID.write(data.getMusicFile().getMusicFileExtract());
                streamID.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            count++;
            Bundle bundle = new Bundle();
            bundle.putString(SONG_NAME_KEY, data.getMusicFile().getTrackName());
            if (count < result)
            {
                Log.d(LOG_TAG,requestHasChanged + "");
                Log.d(LOG_TAG, (mLoaderManager.getLoader(DOWNLOAD_LOADER_ID + count) == null) + "***************************");
                Log.d(LOG_TAG,count + "");
                if(requestHasChanged || mLoaderManager.getLoader(DOWNLOAD_LOADER_ID + count) == null)
                    mLoaderManager.restartLoader(DOWNLOAD_LOADER_ID + count, bundle, dataResultDownloadChunksLoaderListener);
            }
            ((ProgressBar)findViewById(R.id.progress_dialog_bar)).setProgress(count);
            if(count == result)
            {
                progressDialog.setVisibility(View.GONE);
                openUIInteraction();
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Value> loader) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "************* SONG ACTIVITY **************");
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onCreate method");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        // Find a reference to the ListView in the layout
        mSongListView = findViewById(R.id.song_list);

        mEmptyStateTextView = findViewById(R.id.empty_view1);
        mSongListView.setEmptyView(mEmptyStateTextView);

        //create a new custom adapter
        mAdapter = new SongNameAdapter(this, 0, new ArrayList<>());

        mAdapter.setOnItemClickPlayListener(this);

        mAdapter.setOnItemClickDownloadListener(this);

        //set adapter to the list from which it can update UI
        mSongListView.setAdapter(mAdapter);

        mLoaderManager = LoaderManager.getInstance(this);

        if (NetworkUtils.isNetworkAvailable(this)) {
            Log.d(LOG_TAG, "Initialize loader ");
            mLoaderManager.initLoader(SONG_LOADER_ID, null, dataResultSongListLoaderListener);
        }
        else
        {
            View loadingIndicator = findViewById(R.id.loading_spinner1);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }
    }

    @Override
    public void onListItemDownloadClick(String songName) {
        //handle no internet connection at else statement
        if (NetworkUtils.isNetworkAvailable(this)) {
            clickButton = true;
            requestHasChanged = false;
            count = 0;
            if((firstLoaderOfChunks != null && !firstLoaderOfChunks.getLoaderSongName().equals(songName)) || onRestoreInstance)
            {
                requestHasChanged = true;
                for(int i = 0; i < result; i++)
                {
                    if(mLoaderManager.getLoader(DOWNLOAD_LOADER_ID + i) != null)
                        mLoaderManager.destroyLoader(DOWNLOAD_LOADER_ID + i);
                }
            }
            else if(!requestHasChanged && firstLoaderOfChunks != null)
            {
                Toast.makeText(this, "Previous request was same", Toast.LENGTH_SHORT).show();
                return;
            }

            //Loading spinner show at UI
            findViewById(R.id.loading_spinner1).setVisibility(View.VISIBLE);

            closeUIInteraction();

            if(firstLoaderOfChunks == null || requestHasChanged) {
                result = executeTaskToReceiveChunkNumber(songName);
            }
            handleErrorOccurredWithNumberOfChunks(songName);

            if (result != null && result != 0) {
                Log.d(LOG_TAG, "Chunks " + result);

                //receive number of chunks make loading spinner invisible
                findViewById(R.id.loading_spinner1).setVisibility(View.GONE);

                path = StorageUtils.getOurMusicFolder(this) + "/" + songName + ".mp3";
                progressDialog = findViewById(R.id.progress_dialog);
                ((ProgressBar) progressDialog.findViewById(R.id.progress_dialog_bar)).setProgress(0);
                ((ProgressBar) progressDialog.findViewById(R.id.progress_dialog_bar)).setMax(result);
                progressDialog.setVisibility(View.VISIBLE);
                //initialize loaders to receive chunks
                Bundle bundle = new Bundle();
                bundle.putString(SONG_NAME_KEY, songName);
                if(requestHasChanged || firstLoaderOfChunks == null)
                    mLoaderManager.restartLoader(DOWNLOAD_LOADER_ID, bundle, dataResultDownloadChunksLoaderListener);
            }
        }
        else
        {
            View loadingIndicator = findViewById(R.id.loading_spinner1);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mSongListView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListItemPlayClick(String songName) {
        //handle no internet connection at else statement
        if (NetworkUtils.isNetworkAvailable(this)) {
            clickButton = true;
            Intent openSongPlayer = new Intent(this, SongPlayer.class);
            result = executeTaskToReceiveChunkNumber(songName);
            handleErrorOccurredWithNumberOfChunks(songName);
            if (result != null && result != 0) {
                openSongPlayer.putExtra(SONG_NAME_KEY, songName);
                openSongPlayer.putExtra(CLASS_NAME_KEY, "SongActivity");
                openSongPlayer.putExtra(CHUNKS_NUMBER_KEY, result);
                findViewById(R.id.loading_spinner1).setVisibility(View.GONE);
                openUIInteraction();
                startActivity(openSongPlayer);
            }
        }
        else
        {
            View loadingIndicator = findViewById(R.id.loading_spinner1);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mSongListView.setVisibility(View.GONE);
        }
    }

    public Integer executeTaskToReceiveChunkNumber(String songName)
    {
        //receive number of chunks
        NumberOfChunksTask myTask = new NumberOfChunksTask();
        try {
            result = myTask.execute(songName).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.d(LOG_TAG, "We don't receive the number of total chunks");
            Log.e(LOG_TAG, String.valueOf(e));
        }
        Log.d(LOG_TAG, "We receive the number of total chunks which is " + result);
        return result;
    }

    public void handleErrorOccurredWithNumberOfChunks(String songName)
    {
        if (result == null || result == 0) {
            UnregisterTask myTask = new UnregisterTask();
            try {
                if(NetworkUtils.getSocket() != null)
                    myTask.execute(false).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            OpenConnectionFromStartTask openConnectionTask = new OpenConnectionFromStartTask();
            try {
                if(openConnectionTask.execute(this.getIntent().getStringExtra(ARTIST_NAME_KEY)).get() == 0)
                {
                    Log.d(LOG_TAG, "New connection was successful");
                    result = executeTaskToReceiveChunkNumber(songName);
                    if (result != null && result != 0) {
                        return;
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            findViewById(R.id.loading_spinner1).setVisibility(View.GONE);
            openUIInteraction();
            mEmptyStateTextView.setText(R.string.ChunkNumberProblem);
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mSongListView.setVisibility(View.GONE);
        }
    }

    /**
     * while download progress bar is visible close user's interaction with UI
     */
    public void closeUIInteraction()
    {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * when all chunks were downloaded open user's interaction with UI
     */
    public void openUIInteraction()
    {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void closeOutputFileStream()
    {
        try {
            if (streamID != null) {
                streamID.flush();
                streamID.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteExistingFile()
    {
        File fileExist = new File(path);
        if (fileExist.exists()) {
            if (fileExist.delete()) {
                Log.d(LOG_TAG, "file Deleted :" + path);
            } else {
                Log.d(LOG_TAG, "file not Deleted :" + path);
            }
        }
    }

    /**
     * @param menu : initialize a custom menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemSwitch = menu.findItem(R.id.switch_bar);
        itemSwitch.setActionView(R.layout.switch_layout);
        final Switch sw = menu.findItem(R.id.switch_bar).getActionView().findViewById(R.id.switch_button);
        sw.setChecked(true);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent openOfflineActivity = new Intent(SongActivity.this, OfflineActivity.class);
                openOfflineActivity.putExtra(CLASS_NAME_KEY, "com.example.mymusicstreamingapp." + this.getClass());
                UnregisterTask myTask = new UnregisterTask();
                myTask.execute(clickButton);
                openOfflineActivity.putExtra(ARTIST_NAME_KEY, SongActivity.this.getIntent().getStringExtra(ARTIST_NAME_KEY));
                finish();
                startActivity(openOfflineActivity);
                Toast.makeText(SongActivity.this, "OFFLINE MODE", Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }


    /**
     * @param item : contains all menu items and we can find a specific item from menu with his id
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (item.getItemId() == android.R.id.home) {
            UnregisterTask myTask = new UnregisterTask();
            try {
                myTask.execute(true).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        if (id == R.id.refresh)
        {
            UnregisterTask myTask = new UnregisterTask();
            try {
                myTask.execute(true).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            mAdapter.clear();
            finish();
            overridePendingTransition( 0, 0);
            startActivity(getIntent());
            overridePendingTransition( 0, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            UnregisterTask myTask = new UnregisterTask();
            try {
                myTask.execute(true).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * LifeCycle methods
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onSaveInstanceState method");
        outState.putInt(CHUNKS_NUMBER_KEY, result);
        if(count < result-1 && count != 0)
            deleteExistingFile();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onRestoreInstanceState method");
        result = savedInstanceState.getInt(CHUNKS_NUMBER_KEY);
        onRestoreInstance = true;
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onStart method");
        super.onStart();
    }

    @Override
    protected void onResume() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onResume method");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onPause method");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onStop method");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onDestroy method");
        closeOutputFileStream();
        super.onDestroy();
    }
}
