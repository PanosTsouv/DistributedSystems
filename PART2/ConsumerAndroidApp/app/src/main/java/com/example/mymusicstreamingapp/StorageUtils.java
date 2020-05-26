package com.example.mymusicstreamingapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Objects;

public class StorageUtils {

    private static final String LOG_TAG = StorageUtils.class.getName();

    static File getAppSpecificSongStorageDir(Context context, String song) {
        // Get the songs directory that's inside the app-specific directory on
        // external storage.
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), song);
        if (!file.mkdirs()) {
            Log.d(LOG_TAG, "Directory not created");
        }
        return file;
    }

    // Checks if a volume containing external storage is available
    // for read and write.
    static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    // Checks if a volume containing external storage is available to at least read.
    static boolean isExternalStorageReadable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }

    static File getMusicFolder(Context context)
    {
        File file = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (file == null) {
            Log.d(LOG_TAG, "Music Directory not found");
        }
        return file;
    }

    static File getOurMusicFolder(Context context)
    {
        File file = Objects.requireNonNull(getMusicFolder(context).listFiles())[0];
        if (file == null) {
            Log.d(LOG_TAG, "Our Directory not found");
        }
        return file;
    }

    static int returnNumbersOfFilesContains(File folder)
    {
        return Objects.requireNonNull(folder.listFiles()).length;
    }

    static FileOutputStream createASongStream(String songName, Context context) throws FileNotFoundException {
        return new FileOutputStream(StorageUtils.getOurMusicFolder(context) + "/" + songName + ".mp3",true);
    }
}
