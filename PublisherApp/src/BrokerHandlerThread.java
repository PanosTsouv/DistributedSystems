import java.io.*;
import java.net.*;
import java.util.ArrayList;

import com.mpatric.mp3agic.*;

public class BrokerHandlerThread extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ArrayList<ArrayList<String>> songsInfo;
    private ArrayList<Value> chunks;
    private PublisherNode publisher;

    public BrokerHandlerThread(Socket requestSocket, PublisherNode publisher ,ArrayList<ArrayList<String>> songsInfo) {
        this.songsInfo = songsInfo;
        this.publisher = publisher;
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        System.out.println("Server part of publisher :: New thread created");
        try{
            int i;
            ArtistName userArtist = (ArtistName) in.readObject();
            String userSong = (String) in.readObject();
            System.out.println("Server part of publisher :: Receives a new request { " + userArtist.getArtistName() + " , " + userSong + "}");
            for(i = 0; i < this.songsInfo.size(); i++)
            {
                if(userArtist.getArtistName().equals(songsInfo.get(i).get(1)) && userSong.equals(songsInfo.get(i).get(2)))
                {
                    break;
                }
            }
            chunks = findChunks(this.songsInfo.get(i).get(0));
            System.out.println("Server part of publisher :: Creation of chunks were successful -- " + chunks.size() + " chunks created");
            out.writeObject(chunks.size());
            while(!chunks.isEmpty())
            {
                if(userArtist.getArtistName().equals("Kevin MacLeod"))
                {
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                this.publisher.push(userArtist, chunks.remove(0), out);
            }
            System.out.println("Server part of publisher :: Publisher sends all chunks");
        }catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    public ArrayList<Value> findChunks(String path) {
        File file = new File(path);
        Mp3File song = null;
        String artistName = "";
        String trackName = "";
        String albumInfo = "";
        String genre = "";
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        byte[] buf = new byte[512*1024];
        ArrayList<Value> chunks = new ArrayList<>();
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                ByteArrayOutputStream parts = new ByteArrayOutputStream();
                parts.write(buf, 0, readNum);
                try {
                    song = new Mp3File(path);
                } catch (UnsupportedTagException | InvalidDataException | IOException e) {
                    e.printStackTrace();
                }
                if (song!=null && song.hasId3v1Tag()) {
                    ID3v1 id3v1Tag = song.getId3v1Tag();
                    artistName = id3v1Tag.getArtist();
                    trackName = id3v1Tag.getTitle();
                    albumInfo = id3v1Tag.getAlbum();
                    genre = Integer.toString(id3v1Tag.getGenre());
                }
                if (song!=null && song.hasId3v2Tag()) {
                    ID3v2 id3v2tag = song.getId3v2Tag();
                    artistName = id3v2tag.getArtist();
                    trackName = id3v2tag.getTitle();
                    albumInfo = id3v2tag.getAlbum();
                    genre = Integer.toString(id3v2tag.getGenre());
                }
                MusicFile tempMusicFile = new MusicFile(trackName, artistName, albumInfo, genre, parts.toByteArray());
                Value tempValue = new Value(tempMusicFile);
                chunks.add(tempValue);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return chunks;
    }
}