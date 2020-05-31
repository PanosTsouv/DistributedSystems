package com.example.mymusicstreamingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.ByteArrayDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


import Utils.Value;


/*
 * package private keys
 * CHUNKS_NUMBER_KEY and SONG_NAME_KEY define at SongActivity
 * CLASS_NAME_KEY and LIFECYCLE_DEBUG define at MainActivity
 */
import static com.example.mymusicstreamingapp.MainActivity.CLASS_NAME_KEY;
import static com.example.mymusicstreamingapp.MainActivity.LIFECYCLE_DEBUG;
import static com.example.mymusicstreamingapp.SongActivity.CHUNKS_NUMBER_KEY;
import static com.example.mymusicstreamingapp.SongActivity.SONG_NAME_KEY;

public class SongPlayer extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Value>, Player.EventListener{

    private static final String LOG_TAG = SongPlayer.class.getName();

    private static final int REWIND = 5000;

    private static final int FORWARD = 5000;

    /**
     * Constant value for the chunk loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     */
    private static final int CHUNK_LOADER_ID = 5;

    LoaderManager loaderManager;

    private String mSongName;

    private boolean online = false;

    private HashMap<Integer,byte[]> chunkList = new HashMap<>();

    private ArrayList<Long> chunkDuration = new ArrayList<>();

    String path;

    int chunkSize = 0;

    //initialize player
    SimpleExoPlayer player;

    ConcatenatingMediaSource myPlaylist;

    Button play;

    Button rewind;

    Button forward;

    MediaMetadataRetriever metadata;

    ImageView songImage;

    SeekBar seekPlayerProgress;

    private TextView txtCurrentTime, txtEndTime;

    private Handler handler;
    Runnable mainProgress;
    private Handler secondHandler;

    private boolean isPlaying = false;

    private boolean isDownloading = false;

    long secondProgress = 0;
    long mCurrentTotalProgress = 0;

    private boolean returnButtonClick = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(LOG_TAG, "************************ SONG PLAYER ***************************");
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onCreate method");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_player);

        chunkSize = this.getIntent().getIntExtra(CHUNKS_NUMBER_KEY, 0);
        mSongName = this.getIntent().getStringExtra(SONG_NAME_KEY);

        txtCurrentTime = findViewById(R.id.time_current);
        txtEndTime = findViewById(R.id.player_end_time);
        songImage = findViewById(R.id.album_image);
        initPlayerButtons();
        initPlayPauseListener();
        initRewindListener();
        initForwardListener();
        initSeekBar();

        metadata = new MediaMetadataRetriever();

        //if parent activity is SongActivity then we initialize player for online mode
        //else initialize player for offline mode
        if (Objects.equals(this.getIntent().getStringExtra(CLASS_NAME_KEY), "SongActivity")) {
            online = true;
            myPlaylist = new ConcatenatingMediaSource();
            loaderManager = LoaderManager.getInstance(this);
            loaderManager.initLoader(CHUNK_LOADER_ID, null, this);
            isDownloading = true;
        }else{
            path = StorageUtils.getOurMusicFolder(SongPlayer.this).getPath() + "/" + mSongName;
            Uri uri = Uri.parse(path);
            metadata.setDataSource(this, uri);
            String durationStr = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int millSecond = Integer.parseInt(durationStr);
            initMaxValues(millSecond);
            secondProgress = millSecond;
            setSecondProgress();
            setAlbumImageAsImageView();
            initializeUriPlayer();
        }
    }

    public void initPlayerButtons()
    {
        play = findViewById(R.id.play_button);
        play.setBackgroundResource(R.drawable.round_play_arrow_white_24);
        rewind = findViewById(R.id.rewind_button);
        rewind.setBackgroundResource(R.drawable.round_fast_rewind_24);
        forward = findViewById(R.id.forward_button);
        forward.setBackgroundResource(R.drawable.round_fast_forward_24);
    }

    public void initializeUriPlayer()
    {
        if (player == null) {
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(SongPlayer.this, Util.getUserAgent(SongPlayer.this, "ExoPlayer"));

            // Produces Extractor instances for parsing the media data.
            DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                    .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);
            MediaSource source = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(Uri.parse(path));
            TrackSelector trackSelector = new DefaultTrackSelector(this);
            player = new SimpleExoPlayer.Builder(SongPlayer.this).setTrackSelector(trackSelector).build();
            player.addListener(SongPlayer.this);
            player.prepare(source);
            setPlayPause(true);
        }
    }

    public void initializeByteArrayPlayer() {
        if (player == null) {
            TrackSelector trackSelector = new DefaultTrackSelector(this);
            player = new SimpleExoPlayer.Builder(SongPlayer.this).setTrackSelector(trackSelector).build();
            player.addListener(SongPlayer.this);
            player.prepare(myPlaylist);
        }
    }

    private MediaSource createMediaSourceFromByteArray(byte[] data) {
        final ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(data);
        DataSource.Factory factory = () -> byteArrayDataSource;

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(factory, extractorsFactory)
                .createMediaSource(Uri.EMPTY);
        return Objects.requireNonNull(mediaSource, "MediaSource cannot be null");
    }

    private void setPlayPause(boolean isCurrentPlaying){
        isPlaying = isCurrentPlaying;
        if(player != null)
            player.setPlayWhenReady(isCurrentPlaying);
        if(!isPlaying){
            play.setBackgroundResource(R.drawable.round_play_arrow_white_24);
        }else{
            play.setBackgroundResource(R.drawable.round_pause_24);
        }
    }

    public void initPlayPauseListener()
    {
        play.setOnClickListener(v -> {
            if(player == null) {
                showToast("Buffering...");
                return;
            }
            if(player.getPlayWhenReady() && (mCurrentTotalProgress + player.getCurrentPosition()) >= seekPlayerProgress.getSecondaryProgress()) {
                showToast("Buffering...");
                return;
            }
            setPlayPause(!player.getPlayWhenReady());
        });
    }
    public void initRewindListener()
    {
        rewind.setOnClickListener(v -> {
            if (player!=null) {
                long current = player.getCurrentPosition();
                if(player.getCurrentWindowIndex() == 0 && current < REWIND)
                {
                    player.seekTo(0);
                }
                else if(player.getCurrentWindowIndex() != 0 && current < REWIND)
                {
                    if(online) {
                        player.seekTo(player.getCurrentWindowIndex() - 1, chunkDuration.get(player.getCurrentWindowIndex() - 1) + (current - REWIND));
                    }
                }
                else if(player.getCurrentPosition() > REWIND)
                {
                    player.seekTo(current - REWIND);
                }
            }
        });
    }
    public void initForwardListener()
    {
        forward.setOnClickListener(v -> {
            if (player!=null) {
                long current = player.getCurrentPosition();
                if (chunkList.size() == 0)
                {
                    player.seekTo(current + FORWARD);
                }
                if(online) {
                    if (mCurrentTotalProgress + current + FORWARD >= seekPlayerProgress.getSecondaryProgress() && chunkList.size() != chunkSize) {
                        Log.d(LOG_TAG, "buffering...");
                        showToast("Buffering...");
                        return;
                    }
                    if ((current + FORWARD) < chunkDuration.get(player.getCurrentWindowIndex())) {
                        player.seekTo(current + FORWARD);
                    } else if (player.getCurrentWindowIndex() == (player.getCurrentTimeline().getWindowCount() - 1) && (current + FORWARD) > chunkDuration.get(player.getCurrentWindowIndex())) {
                        player.seekTo(chunkDuration.get(player.getCurrentWindowIndex()) - 1000);
                    } else if (player.getCurrentWindowIndex() != (player.getCurrentTimeline().getWindowCount() - 1) && (current + FORWARD) > chunkDuration.get(player.getCurrentWindowIndex())) {
                        if ((player.getCurrentWindowIndex() + 1) == (player.getCurrentTimeline().getWindowCount() - 1) && (FORWARD - (chunkDuration.get(player.getCurrentWindowIndex()) - current)) > chunkDuration.get(player.getCurrentWindowIndex() + 1)) {
                            player.seekTo(player.getCurrentWindowIndex() + 1, chunkDuration.get(player.getCurrentWindowIndex() + 1) - 1000);
                        } else {
                            player.seekTo(player.getCurrentWindowIndex() + 1, FORWARD - (chunkDuration.get(player.getCurrentWindowIndex()) - current));
                        }
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public Loader<Value> onCreateLoader(int id, @Nullable Bundle args) {
        //Log.d(LOG_TAG, "Call onCreateLoader type of DownloadChunksLoader");
        return new DownloadChunkLoader(this, mSongName);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Value> loader, Value data) {
        if (data == null)
        {
            return;
        }
        for(Map.Entry<Integer, byte[]> entry1 : chunkList.entrySet())
        {
            if (entry1.getKey() == loader.getId())
            {
                Log.d(LOG_TAG, "Loader with id " + loader.getId() + " return without put chunk in the list");
                return;
            }
        }
        if (chunkList.size() != chunkSize) {

            Log.d(LOG_TAG, "add chunk " + chunkList.size() + " to the list from loader with id " + loader.getId());
            chunkList.put(CHUNK_LOADER_ID + chunkList.size(), data.getMusicFile().getMusicFileExtract());
            //create MediaSource of chunk and add to to playlist
            MediaSource source = createMediaSourceFromByteArray(chunkList.get(CHUNK_LOADER_ID + chunkList.size()-1));
            myPlaylist.addMediaSource(source);


            long duration = data.getMusicFile().getFirstTrackDuration();

            secondProgress = secondProgress + duration;

            chunkDuration.add(duration);

            if(loader.getId()==CHUNK_LOADER_ID){
                initializeByteArrayPlayer();
                initMaxValues(data.getMusicFile().getTrackDuration());
                setPlayPause(true);
                setSecondProgress();

                MediaDataSource metada = new ByteMediaDataSource(chunkList.get(CHUNK_LOADER_ID));
                metadata.setDataSource(metada);
                setAlbumImageAsImageView();
            }
        }
        if (chunkList.size() < chunkSize)
        {
            if(!returnButtonClick)
                loaderManager.initLoader(CHUNK_LOADER_ID + chunkList.size(), null, this);
        }
        if (chunkList.size() == chunkSize)
        {
            Log.d(LOG_TAG, chunkList.size() + "");
        }

    }

    public void setAlbumImageAsImageView()
    {
        byte[] songImageByte = metadata.getEmbeddedPicture();
        if (songImageByte != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(songImageByte, 0, songImageByte.length);
            songImage.setImageBitmap(bitmap);
        }
        else
        {
            songImage.setBackgroundResource(R.drawable.default_cover_art);
        }
    }

    public void clearResources()
    {
        setPlayPause(false);
        isDownloading = false;
        if(myPlaylist != null) {
            myPlaylist.clear();
        }
        if (player != null) {
            player.release();   //it is important to release a player
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Value> loader) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            returnButtonClick = true;
            clearResources();
            UnregisterTask myTask = new UnregisterTask();
            try {
                myTask.execute(true).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            returnButtonClick = true;
            clearResources();
            UnregisterTask myTask = new UnregisterTask();
            try {
                myTask.execute(true).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(LOG_TAG,"onTracksChanged");
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(LOG_TAG,"onPlayerStateChanged: playWhenReady = "+ playWhenReady
                +" playbackState = "+playbackState);
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                //You can use progress dialog to show user that video is preparing or buffering so please wait
                Log.d(LOG_TAG,"ExoPlayer buffering!");
                setPlayPause(playWhenReady);
                break;
            case Player.STATE_IDLE:
                //idle state
                Log.d(LOG_TAG,"ExoPlayer idle mode!");
                break;
            case Player.STATE_READY:
                if(online) {
                    if (player.getCurrentWindowIndex() < chunkDuration.size()) {
                        Log.d(LOG_TAG, "ExoPlayer ready! pos: " + player.getCurrentPosition()
                                + " maxChunkFromArray: " + chunkDuration.get(player.getCurrentWindowIndex()));
                    }
                }
                Log.d(LOG_TAG,"ExoPlayer ready! pos: "+player.getCurrentPosition()
                        +" max: "+player.getDuration());
                setProgress();
                // dismiss your dialog here because our video is ready to play now
                break;
            case Player.STATE_ENDED:
                Log.d(LOG_TAG,"Playback ended!");
                setPlayPause(false);
                player.seekTo(0,0);
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d(LOG_TAG,"onPlaybackError: "+error.getMessage());
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if(online) {
            mCurrentTotalProgress = 0;
            if (player.getCurrentWindowIndex() != 0) {
                for (int i = player.getCurrentWindowIndex() - 1; i >= 0; i--) {
                    mCurrentTotalProgress = mCurrentTotalProgress + chunkDuration.get(i);
                }
            }
            Log.d(LOG_TAG, "onPositionDiscontinuity");
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    //update seekBar first progress bar
    private void setProgress() {
        seekPlayerProgress.setProgress((int)(mCurrentTotalProgress + player.getCurrentPosition()));
        txtCurrentTime.setText(stringForTime((int)(mCurrentTotalProgress + player.getCurrentPosition())));

        if(handler == null)handler = new Handler();
        //Make sure you update Seekbar on UI thread
        if(mainProgress == null) {
            mainProgress = new Runnable() {
                @Override
                public synchronized void run() {
                    if (player != null && isPlaying) {
//                        Log.d(LOG_TAG, "player.getCurrentWindowIndex() " + player.getCurrentWindowIndex());
//                        Log.d(LOG_TAG, "mCurrentTotalProgress " + mCurrentTotalProgress);
//                        Log.d(LOG_TAG, "player.getCurrentPosition() " + player.getCurrentPosition());

                        if (online) {
                            if (player.getCurrentPosition() > chunkDuration.get(player.getCurrentWindowIndex())) {
                                int mCurrentPosition = (int) (mCurrentTotalProgress + chunkDuration.get(player.getCurrentWindowIndex()));
                                seekPlayerProgress.setProgress(mCurrentPosition);
                                txtCurrentTime.setText(stringForTime(mCurrentPosition));
                                handler.postDelayed(this, 0);
                                return;
                            }
                        }
                        int mCurrentPosition = (int) (mCurrentTotalProgress + player.getCurrentPosition());
                        seekPlayerProgress.setProgress(mCurrentPosition);
                        txtCurrentTime.setText(stringForTime(mCurrentPosition));
                        handler.postDelayed(this, 0);
                    }
                }
            };
        }
        handler.post(mainProgress);
    }

    //update seekBar second progress bar
    private void setSecondProgress() {
        seekPlayerProgress.setSecondaryProgress((int)secondProgress);

        if(secondHandler == null)secondHandler = new Handler();
        //Make sure you update Seekbar on UI thread
        secondHandler.post(new Runnable() {
            @Override
            public synchronized void run() {
                if (isDownloading) {
                    seekPlayerProgress.setSecondaryProgress((int)secondProgress);
                    secondHandler.postDelayed(this, 0);
                    if(chunkList.size() == SongPlayer.this.getIntent().getIntExtra(CHUNKS_NUMBER_KEY, 0))
                    {
                        isDownloading = false;
                    }
                }
            }
        });
    }

    private String stringForTime(int timeMs) {
        StringBuilder mFormatBuilder;
        Formatter mFormatter;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        int totalSeconds =  timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void initSeekBar() {
        seekPlayerProgress = findViewById(R.id.progressBar);
        seekPlayerProgress.requestFocus();

        seekPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }
                Log.d(LOG_TAG,"Progress " + progress);
                if(player != null) {
                    if(online){
                        int temp = progress;
                        for (int i = 0; i < player.getCurrentTimeline().getWindowCount(); i++) {
                            temp = (int)(temp - chunkDuration.get(i));
                            if(i > 0)
                            {
                                progress = (int)(progress - chunkDuration.get(i-1));
                            }
                            if(progress >= seekBar.getSecondaryProgress())
                            {
                                progress = seekBar.getSecondaryProgress()-100;
                            }
                            if (temp <= 0)
                            {
                                Log.e(LOG_TAG, progress + " || " + i + " || " + txtCurrentTime.getText() + " || " + mCurrentTotalProgress + " || " + secondProgress);
                                player.seekTo(i,progress);
                                return;
                            }
                        }
                    }
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(LOG_TAG,"onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(LOG_TAG,"onStopTrackingTouch");
            }
        });
    }

    private void initMaxValues(long maxDuration)
    {
        seekPlayerProgress.setMax((int)maxDuration);
        txtEndTime.setText(stringForTime((int)maxDuration));
    }

    /**
     * Method which create toast messages from static variable mToast in SongActivity
     */

    public void showToast(String toastMessage){
        if (SongActivity.mToast!= null) {
            SongActivity.mToast.cancel();
        }
        SongActivity.mToast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT);
        SongActivity.mToast.show();
    }

    /**
     * Custom mediaDataSource from byte array
     */

    static class ByteMediaDataSource extends MediaDataSource {

        private byte[] _data;

        ByteMediaDataSource(byte[] data) { _data = data; }

        @Override
        public long getSize() {
            return _data.length;
        }

        @Override
        public int readAt(long position, byte[] buffer, int offset, int size) {

            int length = _data.length;

            if (position >= length) { return -1; }

            if (size == 0) { return 0; }

            if (position + size > length) {
                size = (int) (length - position);
            }

            System.arraycopy(_data, (int)position, buffer, offset, size);

            return size;
        }

        @Override
        public void close() {}

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
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onPause method");
        if(returnButtonClick)
        {
            if(player != null)
                setPlayPause(false);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onStop method");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(LIFECYCLE_DEBUG) Log.d(LOG_TAG, "call onDestroy method");
        super.onDestroy();
        clearResources();
    }
}