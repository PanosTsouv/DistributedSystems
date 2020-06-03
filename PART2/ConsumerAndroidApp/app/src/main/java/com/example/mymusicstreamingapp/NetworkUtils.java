package com.example.mymusicstreamingapp;

import Utils.ArtistName;
import Utils.Info;
import Utils.Value;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

class NetworkUtils {

    private static String serverIP = "192.168.1.2";
    private static String port = "4321";
    private static Socket requestSocket = null;
    private static ObjectOutputStream out = null;
    private static ObjectInputStream in = null;
    private static final String LOG_TAG = NetworkUtils.class.getName();
    private static ArrayList<String> mConsumerInfo;
    private static Info answer = null;
    private static String artistName;
    private static int countChunksReceive = 0;


    private static void restartCountChunksReceive()
    {
        countChunksReceive = 0;
    }

    static synchronized ArrayList<ArtistName> fetchArtists(ArrayList<String> consumerInfo) {
        mConsumerInfo = consumerInfo;
        init();
        connect();
        receiveInfoObject();
        return createArtistList();
    }

    static synchronized ArrayList<String> fetchSongs(String artist) {
        findTheRightBroker(artist);
        HashSet<String> temp = receiveTheListOfSong();
        if (temp != null && !temp.isEmpty())
        {
            return new ArrayList<>(temp);
        }
        return null;
    }

    static int receiveTotalChunkNumber(String songName)
    {
        restartCountChunksReceive();
        int chunkSize = 0;
        try {
            if (out != null) {
                out.writeObject(songName);
                out.flush();
            }
            Log.d(LOG_TAG,"Send songName" + songName);// (debug)
            if(in != null) {
                chunkSize = (Integer) in.readObject();
            }
            Log.d(LOG_TAG,"Receive chunk size " + chunkSize);// (debug)
        } catch (IOException | ClassNotFoundException | ClassCastException | InternalError |ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return chunkSize;
    }

    static synchronized Value downloadChunk(int id)
    {
        Value chunk = null;
        try {
            if (in != null) {
                long startBackgroundJobTime = new Date().getTime();
                chunk = (Value) in.readObject();
                long endBackgroundJobTime = new Date().getTime();
                Log.d(LOG_TAG,"ELAPSE BACKGROUND JOB TIME: " + (endBackgroundJobTime - startBackgroundJobTime));
            }
            Log.d(LOG_TAG,"Receive " + countChunksReceive++ + " chunk from thread with id " + id);// (debug)
        } catch (IOException | ClassNotFoundException | ClassCastException | InternalError |ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
        return chunk;
    }

    /**
     * Create a private constructor because no one should ever create a {@link NetworkUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name NetworkUtils (and an object instance of NetworkUtils is not needed).
     */
    private NetworkUtils() {
    }

    // open a connection with a broker(server)
    // initialize output-input stream
    private static void init() {
        try {
            Log.d(LOG_TAG,"Server which Client try to connect has ServerIP: " + serverIP + " Port: " + port);// (debug)
            SocketAddress socketAddress= new InetSocketAddress(serverIP, Integer.parseInt(port));
            requestSocket = new Socket();
            requestSocket.connect(socketAddress,3000);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            Log.d(LOG_TAG,"You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            Log.d(LOG_TAG,"You are trying to connect to an offline server.Check the server IP and port");
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Port should be number");
        }
    }

    //TEST TO SEND WRONG VALUES AND WE HANDLE THIS-----------------------------------
    // send user type(consumer) and his attributes
    private static void connect() {
        try {
            if (out != null) {
                out.writeObject("ConsumerNode");
                out.flush();
                out.writeObject(mConsumerInfo);
                out.flush();
            }
        } catch (IOException e) {
            Log.d(LOG_TAG,"Connect throw IOexception");
            Log.d(LOG_TAG, String.valueOf(e));
        }
    }

    //close a connection with server
    static void disconnect()
    {
        try
        {
            if (requestSocket != null) {
                requestSocket.close();
                requestSocket = null;
                Log.d(LOG_TAG,"Connection close");
            }
            if (in != null) {
                in.close();
                in = null;
                Log.d(LOG_TAG,"InputStream close");
            }
            if (out != null) {
                out.close();
                out = null;
                Log.d(LOG_TAG,"OutputStream close");
            }
        }
        catch(IOException ioException)
        {
            Log.d(LOG_TAG,"Disconnect throw IOexception");
            Log.d(LOG_TAG, String.valueOf(ioException));
        }
    }

    //TEST TO RECEIVE WRONG VALUES FOR INFO OBJECT AND WE HANDLE THIS-----------------------------------
    private static void receiveInfoObject()
    {
        try {
            if (in != null) {
                answer = (Info) in.readObject();
                mConsumerInfo.set(2, "false");
            }
        } catch (ClassCastException | IOException | ClassNotFoundException e) {
            Log.e(LOG_TAG, String.valueOf(answer));
            Log.e(LOG_TAG, "Don't Receive Info Object");
            e.printStackTrace();
            Log.e(LOG_TAG, "ErrorHandler");
        }
    }

    //TEST TO RECEIVE WRONG VALUES FOR INFO OBJECT AND WE HANDLE THIS-----------------------------------
    private static ArrayList<ArtistName> createArtistList()
    {
        if (answer == null)
        {
            return null;
        }
        ArrayList<ArtistName> artists = new ArrayList<>();
        for(Map.Entry<ArtistName, String> entry : answer.getArtistToBroker().entrySet())
        {
            artists.add(entry.getKey());
        }
        return artists;
    }

    // check if the random broker at start is the correct broker
    private static boolean isTheRightBroker(String tempBrokerId) {
        for (ArrayList<String> element : answer.getBrokers()) {
            if (element.get(0).equals(tempBrokerId)) {
                if (element.get(1).equals(port) && element.get(2).equals(serverIP)) {
                    return true;
                }
                Log.d(LOG_TAG,"Set new values IP: " + element.get(2) + " Port " + element.get(1));
                setPort(element.get(1));
                setServerIP(element.get(2));
            }
        }
        return false;
    }


    //TEST DONE
    private static void findTheRightBroker(String temp) {
        String tempBrokerId = null;
        artistName = temp;
        if (answer != null) {
            for (Map.Entry<ArtistName, String> entry : answer.getArtistToBroker().entrySet()) {
                if (entry.getKey().getArtistName().equals(temp)) {
                    tempBrokerId = entry.getValue();
                    break;
                }
            }
        }
        if(tempBrokerId != null)
        {
            if (isTheRightBroker(tempBrokerId)) {
                register();
            } else {
                try {
                    if(out != null) {
                        out.writeObject("disconnect");
                        out.flush();
                    }
                    disconnect();
                    init();
                    connect();
                    register();
                } catch (IOException e) {
                    Log.d(LOG_TAG, "findTheRightBroker throw IOexception");
                    Log.d(LOG_TAG, String.valueOf(e));
                }
            }
        }
    }

    //TEST DONE IF AN ERROR OCCURRED WE RETURN NULL
    @SuppressWarnings("unchecked")
    private static HashSet<String> receiveTheListOfSong() {
        try {
            if (in != null) {
                return (HashSet<String>) in.readObject();
            }
        } catch (ClassNotFoundException | IOException | ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    // we use register method when the broker, which user connect for first time,is
    // wrong
    //TEST DONE IF AN ERROR OCCURRED
    private static void register() {
        try {
            if(out != null) {
                out.writeObject("register");
                out.flush();
                out.writeObject(artistName);
                out.flush();
            }
        } catch (IOException e) {
            Log.d(LOG_TAG,"register throw IOexception");
            Log.d(LOG_TAG, String.valueOf(e));
        }
    }

    static void unregister() {
        try {
            if(out != null) {
                out.writeObject("I want to unregister");
            }
            if(in != null) {
                String answer = (String) in.readObject();
            }
            if (requestSocket != null) {
                requestSocket.close();
                requestSocket = null;
                Log.d(LOG_TAG,"Connection close");
            }
            if (in != null) {
                in.close();
                in = null;
                Log.d(LOG_TAG,"InputStream close");
            }
            if (out != null) {
                out.close();
                out = null;
                Log.d(LOG_TAG,"OutputStream close");
            }
        } catch (IOException | ClassNotFoundException | ClassCastException | InternalError ioException) {
            ioException.printStackTrace();
            disconnect();
        }
    }

    static Socket getSocket()
    {
        return requestSocket;
    }

    static synchronized void openConnection()
    {
        init();
        connect();
    }

    static void setPort(String s) {
        port = s;
    }

    static void setServerIP(String s) {
        serverIP = s;
    }

    static void setAnswerToNull()
    {
        answer= null;
    }

    //check internet connection
    static boolean isNetworkAvailable(Context context) {
        boolean result = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = true;
                    }
                }
            }
        } else {
            if (cm != null) {

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        result = true;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
