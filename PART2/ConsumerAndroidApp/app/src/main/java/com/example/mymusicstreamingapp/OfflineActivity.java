package com.example.mymusicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.example.mymusicstreamingapp.MainActivity.ARTIST_NAME_KEY;

public class OfflineActivity extends AppCompatActivity implements OfflineListAdapter.ListItemClickListener {

    private static final String LOG_TAG = OfflineActivity.class.getName();

    private static final String CLASS_NAME_KEY = "className";

    //key for put songName in intent or bundle
    private static final String SONG_NAME_KEY = "songName";

    private String mParentClass;

    private File mMusicFolder;

    private RecyclerView mDownloadList;

    ArrayList<File> mSongsName;

    private int mNumberOfFiles;

    OfflineListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_activity);

        Log.d(LOG_TAG, "************* OFFLINE ACTIVITY **************");
        if(StorageUtils.isExternalStorageReadable()) {
            mMusicFolder = StorageUtils.getOurMusicFolder(this);
            mNumberOfFiles = StorageUtils.returnNumbersOfFilesContains(mMusicFolder);
        }
        Log.d(LOG_TAG, mMusicFolder.getName());

        TextView folderName = findViewById(R.id.folder_name);
        folderName.setText(mMusicFolder.getName());

        TextView numberOfSongs = findViewById(R.id.files_in_folder_number);
        numberOfSongs.setText(String.valueOf(mNumberOfFiles));
        mParentClass = getIntent().getStringExtra(CLASS_NAME_KEY);

        RelativeLayout view = findViewById(R.id.folder);

        mDownloadList = findViewById(R.id.download_songs);

        mDownloadList.setHasFixedSize(true);

        mDownloadList.setLayoutManager(new LinearLayoutManager(this));

        mSongsName = new ArrayList<>();

        mSongsName.addAll(Arrays.asList(Objects.requireNonNull(mMusicFolder.listFiles())));

        mAdapter = new OfflineListAdapter(OfflineActivity.this, mSongsName,  this);

        mDownloadList.setAdapter(mAdapter);

        view.setOnClickListener(v -> {
            ImageView imageFolder = findViewById(R.id.folder_image);
            imageFolder.setBackgroundResource(R.drawable.round_folder_open_black_48);
            mDownloadList.setVisibility(View.VISIBLE);
        });

        //swipe an item from the list to delete it
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                for (File item : Objects.requireNonNull(mMusicFolder.listFiles()))
                {
                    if(mSongsName.get(viewHolder.getAdapterPosition()).getName().equals(item.getName()))
                    {
                        if(item.delete())
                        {
                            Log.d(LOG_TAG,"File was deleted");
                        }
                    }
                }
                mSongsName.remove(viewHolder.getAdapterPosition());
                TextView numberOfSongs = findViewById(R.id.files_in_folder_number);
                numberOfSongs.setText(String.valueOf(mSongsName.size()));
                mAdapter.notifyDataSetChanged();
            }
        }).attachToRecyclerView(mDownloadList);
    }

    /**
     * @param menu : contain switch button to return to online mode
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemRefresh = menu.findItem(R.id.refresh);
        itemRefresh.setVisible(false);
        MenuItem itemSwitch = menu.findItem(R.id.switch_bar);
        itemSwitch.setActionView(R.layout.switch_layout);
        final Switch sw = menu.findItem(R.id.switch_bar).getActionView().findViewById(R.id.switch_button);
        sw.setChecked(false);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent openParentActivity;
            if(mParentClass.contains("MainActivity")) {
                openParentActivity = new Intent(OfflineActivity.this, MainActivity.class);
            }else
            {
                openParentActivity = new Intent(OfflineActivity.this, SongActivity.class);
                openParentActivity.putExtra(ARTIST_NAME_KEY,OfflineActivity.this.getIntent().getStringExtra(ARTIST_NAME_KEY));
            }
            startActivity(openParentActivity);
            finish();
            Toast.makeText(getApplicationContext(), "ONLINE MODE", Toast.LENGTH_SHORT).show();
        });
        return true;
    }

    @Override
    public void onListItemClick(int clickedItemIndex) {
        String currentSong = mSongsName.get(clickedItemIndex).getName();
        Toast.makeText(OfflineActivity.this, ("Play data from song " + currentSong), Toast.LENGTH_SHORT).show();
        Intent openSongPlayer = new Intent(OfflineActivity.this, SongPlayer.class);
        openSongPlayer.putExtra(CLASS_NAME_KEY, "OfflineActivity");
        openSongPlayer.putExtra(SONG_NAME_KEY, currentSong);
        startActivity(openSongPlayer);
    }
}
