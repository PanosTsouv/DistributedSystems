import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;

import com.mpatric.mp3agic.*;

import Utils.ArtistName;
import Utils.MusicFile;
import Utils.Value;

public class BrokerHandlerThread extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ArrayList<ArrayList<String>> songsInfo;
    private ArrayList<Value> chunks;
    private PublisherNode publisher;

    // constructor - initialize connection attributes
    // songsInfo is the list which we create when we read the database(artist
    // name,song name, path of song)
    // the publisher object (access server part of publisher's attributes)
    public BrokerHandlerThread(Socket requestSocket, PublisherNode publisher, ArrayList<ArrayList<String>> songsInfo) {
        this.songsInfo = songsInfo;
        this.publisher = publisher;
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 2 category of request 1)the chunks of a specific song 2)a list of songs for a
    // specific artist
    // 1) Waiting for an artist name and a song name->
    // ->search the list with info of all songs and find the request's path->
    // ->call findChunks method to read the request from disk and create chunks
    // list->
    // ->push chunks to broker
    // 2)call songsOFSpecificArtist method which return all song of an artist
    public void run() {
        System.out.println("Server part of publisher :: New thread created");
        try {
            String answerCategory = (String) in.readObject();
            if (answerCategory.equals("Request")) {
                int i;
                ArtistName userArtist = (ArtistName) in.readObject();
                String userSong = (String) in.readObject();
                System.out.println("Server part of publisher :: Receives a new request { " + userArtist.getArtistName()
                        + " , " + userSong + "}");
                for (i = 0; i < this.songsInfo.size(); i++) {
                    if (userArtist.getArtistName().equals(songsInfo.get(i).get(1))
                            && userSong.equals(songsInfo.get(i).get(2))) {
                        break;
                    }
                }
                chunks = findChunks(this.songsInfo.get(i).get(0));
                System.out.println("Server part of publisher :: Creation of chunks was successful -- " + chunks.size()
                        + " chunks created");

                out.writeObject(chunks.size());
                int count = 0;
                while (!chunks.isEmpty()) {
                    this.publisher.push(userArtist, chunks.remove(0), out);
                    System.out.println("Server part of publisher :: Publisher push chunk " + count + " of song " + userSong);
                    count++;
                }
                System.out.println("Server part of publisher :: Publisher sent all chunks");
            } else if (answerCategory.equals("List")) {
                ArtistName userArtist = (ArtistName) in.readObject();
                System.out.println("Server part of publisher :: Return a list with songs of artist: "
                        + userArtist.getArtistName());
                out.writeObject(songsOfSpecificArtist(userArtist.getArtistName()));
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                System.out.println("Server part of publisher :: A request is done!!!Close thread.");
                System.out.println();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // create a hashSet with songs of a given artist
    public HashSet<String> songsOfSpecificArtist(String userArtist) {
        HashSet<String> temp = new HashSet<>();
        for (ArrayList<String> element : publisher.getSongInfo()) {
            if (element.get(1).equals(userArtist)) {
                temp.add(element.get(2));
            }
        }
        return temp;
    }

    // return a list with all chunks as value objects for a specific song with its
    // path
    // every value object(chunk) has artistName,trackName,albumInfo,genre and an
    // array of bytes(data of chunk)
    // chunk size is 512 KB you can change it if sizeOf chunk variable change
    public ArrayList<Value> findChunks(String path) {
        final int sizeOfChunk = 512;
        File file = new File(path);
        Mp3File song = null;
        String artistName = "";
        String trackName = "";
        String albumInfo = "";
        String genre = "";
        long trackDuration = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] buf = new byte[sizeOfChunk * 1024];
        ArrayList<Value> chunks = new ArrayList<>();
        try {
            int count = 0;
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                ByteArrayOutputStream parts = new ByteArrayOutputStream();
                parts.write(buf, 0, readNum);
                try {
                    song = new Mp3File(path);
                } catch (UnsupportedTagException | InvalidDataException | IOException e) {
                    e.printStackTrace();
                }
                if (song != null && song.hasId3v1Tag()) {
                    ID3v1 id3v1Tag = song.getId3v1Tag();
                    artistName = id3v1Tag.getArtist();
                    trackName = id3v1Tag.getTitle();
                    albumInfo = id3v1Tag.getAlbum();
                    genre = Integer.toString(id3v1Tag.getGenre());
                }
                if (song != null && song.hasId3v2Tag()) {
                    ID3v2 id3v2tag = song.getId3v2Tag();
                    artistName = id3v2tag.getArtist();
                    trackName = id3v2tag.getTitle();
                    albumInfo = id3v2tag.getAlbum();
                    genre = Integer.toString(id3v2tag.getGenre());
                }
                trackDuration = song.getLengthInMilliseconds();
                MusicFile tempMusicFile = new MusicFile(trackName, artistName, albumInfo, genre, parts.toByteArray(),
                        trackDuration, song.getLength());
                Value tempValue = new Value(tempMusicFile);
                chunks.add(tempValue);

                File temp = new File(trackName + count + ".mp3");
                BufferedOutputStream bos = null;
        
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(temp));
                    bos.write(parts.toByteArray());
                    bos.flush();
                    Mp3File song0 = new Mp3File(temp);
                    chunks.get(count).getMusicFile().setFirstTrackDuration(song0.getLengthInMilliseconds());
                    bos.close();
                    temp.delete();
                } catch (UnsupportedTagException | InvalidDataException | IOException e1) {
                    e1.printStackTrace();
                }
                count++;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return chunks;
    }
}