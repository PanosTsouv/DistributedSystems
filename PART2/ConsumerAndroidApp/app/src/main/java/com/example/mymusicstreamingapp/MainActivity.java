package com.example.mymusicstreamingapp;

import Utils.ArtistName;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<ArtistName>>, SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Constant value for the artist loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     */
    private static final int ARTIST_LOADER_ID = 1;

    /**
     * package private keys
     */
    private static final String SONGS_FOLDER = "AllSongs";

    static final String PATH_SONGS_FOLDER = "songsFolder";

    static final String ARTIST_NAME_KEY = "artistName";

    static final String CLASS_NAME_KEY = "className";

    private static final String CONSUMER_INFO_KEY = "consumerInfo";

    /**
    * empty textView to initialize activity when list with artists is empty
    */
    private TextView mEmptyStateTextView;

    /**
     * DEBUG KEYS VARIABLES
     */
    private static final String LOG_TAG = MainActivity.class.getName();
    static final boolean ARTIST_LOADER_DEBUG = true;

    /**
     * LIFECYCLE DEBUG
     */
    static final boolean LIFECYCLE_DEBUG = true;

    /**
     * Grid View initialization
     * mAdapter store artist list data
     * mLoaderManager initialize loaders to retrieve data from server
     */
    private ArtistNameAdapter mAdapter;

    LoaderManager mLoaderManager;

    private ArrayList<String> mConsumerInfo = new ArrayList<>();

    private File mMusicFolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(LOG_TAG, "************* MAIN ACTIVITY **************");
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "Call onCreate method");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //create our app folder to store music files
        mMusicFolder = StorageUtils.getAppSpecificSongStorageDir(this, SONGS_FOLDER);

        mConsumerInfo.add("Panos");
        mConsumerInfo.add("123456789");
        mConsumerInfo.add("true");
        mConsumerInfo.add("Panostsouv95@gmail.com");
        defaultSetup();

        // Find a reference to the GridView in the layout
        GridView artistListView = findViewById(R.id.artist_list);

        mEmptyStateTextView = findViewById(R.id.empty_view);
        artistListView.setEmptyView(mEmptyStateTextView);

        if(checkDefaultValues()){return;}
        //create a new custom adapter
        mAdapter = new ArtistNameAdapter(this, 0, new ArrayList<>());

        //set adapter to the list from which it can update UI
        artistListView.setAdapter(mAdapter);

        //listen artist click --JOB--
        artistListView.setOnItemClickListener((parent, view, position, id) -> {
            ArtistName currentArtist = mAdapter.getItem(position);
            Intent songs = new Intent(MainActivity.this, SongActivity.class);
            assert currentArtist != null;
            songs.putExtra(ARTIST_NAME_KEY, currentArtist.getArtistName());
            songs.putStringArrayListExtra(CONSUMER_INFO_KEY, mConsumerInfo);
            startActivity(songs);
        });

        if (NetworkUtils.isNetworkAvailable(this)) {
            mLoaderManager = LoaderManager.getInstance(this);

            if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Initialize loader ");
            mLoaderManager.initLoader(ARTIST_LOADER_ID, null, this);
        }
        else
        {
            View loadingIndicator = findViewById(R.id.loading_spinner);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }
    }


    /**
     * @param id : loader's id
     * @param args : any data that we wanna pass into loader (it is null here)
     * @return ArtistNameLoader object
     */
    @NonNull
    @Override
    public Loader<ArrayList<ArtistName>> onCreateLoader(int id, @Nullable Bundle args) {
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onCreateLoader");
        return new ArtistNameLoader(this, mConsumerInfo);
    }


    /**
     * @param loader : loader which did the background job
     * @param artists : the list with artist that loader retrieve
     */
    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<ArtistName>> loader, ArrayList<ArtistName> artists) {
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onLoadFinished");
        ProgressBar progressView = findViewById(R.id.loading_spinner);
        progressView.setVisibility(View.GONE);

        // Clear the adapter of previous artist data
        mAdapter.clear();

        // If there is a valid list of {@link artists}, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (artists != null && !artists.isEmpty()) {
            if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Update list view with artist list");
            mAdapter.addAll(artists);
        }
        else
        {
            Log.d(LOG_TAG, "Set the empty view");
            // Set empty state text to display "No artists found."
            mEmptyStateTextView.setText(R.string.no_artists);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<ArtistName>> loader) {
        // Loader reset, so we can clear out our existing data.
        if(ARTIST_LOADER_DEBUG) Log.d(LOG_TAG, "Call onLoaderReset");
        mAdapter.clear();
    }

    private void defaultSetup()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mConsumerInfo.set(0,sharedPreferences.getString(getString(R.string.pref_client_name_key), getString(R.string.pref_client_name_default_value)));
        mConsumerInfo.set(1,sharedPreferences.getString(getString(R.string.pref_client_pass_key), getString(R.string.pref_client_pass_default_value)));
        mConsumerInfo.set(3,sharedPreferences.getString(getString(R.string.pref_client_email_key), getString(R.string.pref_client_email_default_value)));
        NetworkUtils.setServerIP(sharedPreferences.getString(getString(R.string.pref_random_ip_key), getString(R.string.pref_random_ip_default_value)));
        NetworkUtils.setPort(sharedPreferences.getString(getString(R.string.pref_random_port_key), getString(R.string.pref_random_port_default_value)));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        Log.d(LOG_TAG,mConsumerInfo.toString());
    }

    private boolean checkDefaultValues()
    {
        if (mConsumerInfo.get(0).equals(getString(R.string.pref_client_name_default_value)) ||
                mConsumerInfo.get(1).equals(getString(R.string.pref_client_pass_default_value)) ||
                mConsumerInfo.get(3).equals(getString(R.string.pref_client_email_default_value)))
        {
            View loadingIndicator = findViewById(R.id.loading_spinner);
            loadingIndicator.setVisibility(View.GONE);
            mEmptyStateTextView.setText(R.string.change_default_values_message);
            return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_client_name_key))) {
            mConsumerInfo.set(0, sharedPreferences.getString(key, getResources().getString(R.string.pref_client_name_default_value)));
        }else if(key.equals(getString(R.string.pref_client_pass_key))){
            mConsumerInfo.set(1, sharedPreferences.getString(key, getResources().getString(R.string.pref_client_pass_default_value)));
        }else if(key.equals(getString(R.string.pref_client_email_key))){
            mConsumerInfo.set(3, sharedPreferences.getString(key, getResources().getString(R.string.pref_client_email_default_value)));
        }else if(key.equals(getString(R.string.pref_random_ip_key))){
            NetworkUtils.setServerIP(sharedPreferences.getString(key, getResources().getString(R.string.pref_random_ip_default_value)));
        }else if(key.equals(getString(R.string.pref_random_port_key))){
            NetworkUtils.setPort(sharedPreferences.getString(key, getResources().getString(R.string.pref_random_port_default_value)));
        }
        Log.d(LOG_TAG,mConsumerInfo.toString());
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
                Intent openOfflineActivity = new Intent(MainActivity.this, OfflineActivity.class);
                openOfflineActivity.putExtra(CLASS_NAME_KEY, "com.example.mymusicstreamingapp." + this.getClass());
                openOfflineActivity.putExtra(PATH_SONGS_FOLDER, mMusicFolder);
                UnregisterTask myTask = new UnregisterTask();
                myTask.execute(false);
                startActivity(openOfflineActivity);
                finish();
                Toast.makeText(getApplicationContext(), "OFFLINE MODE", Toast.LENGTH_SHORT).show();
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
        if (id == R.id.refresh)
        {
            UnregisterTask myTask = new UnregisterTask();
            myTask.execute(false);
            if (mAdapter != null) {
                mAdapter.clear();
            }
            finish();
            overridePendingTransition( 0, 0);
            startActivity(getIntent());
            overridePendingTransition( 0, 0);
            return true;
        }
        if (id == R.id.preferences)
        {
            NetworkUtils.setAnswerToNull();
            Intent startSettingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * LifeCycle methods
     */
    @Override
    protected void onStart() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onStart method");
        super.onStart();
    }

    @Override
    protected void onResume() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onResume method");
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
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
}
