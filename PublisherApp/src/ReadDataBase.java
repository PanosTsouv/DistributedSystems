import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.mpatric.mp3agic.*;
public class ReadDataBase{
    private String path;
    private char startChar;
    private char endChar;

    public ReadDataBase(String path, char startChar, char endChar)
    {
        this.path = path;
        this.startChar = startChar;
        this.endChar = endChar;
    }

    public ArrayList<ArrayList<String>> readthePathOfMusicFiles()
    {
        File file = new File(path);
        String[] names = file.list();// all directories inside dataset1 direcory
        ArrayList<ArrayList<String>> pathOfMusicFiles = new ArrayList<>();
        String artistName = "";
        String trackName = "";
        Mp3File song = null;
        for (int i = 0; i < names.length; i++) 
        {
            File file1 = new File(path + '/' + names[i]);
            if (file1.isDirectory())
            {
                String[] fileNames = file1.list();
                for (String fileName : fileNames)// all files in directory
                {
                    if (fileName.contains(".mp3") && !(fileName.charAt(0) == '.'))
                    {
                        try
                        {
                            song = new Mp3File(path + '/' + names[i] + '/' + fileName);
                        }
                        catch (UnsupportedTagException | InvalidDataException | IOException e)
                        {
                            e.printStackTrace();
                        }
                        if(song!=null && song.hasId3v1Tag())
                        {
                            ID3v1 id3v1Tag = song.getId3v1Tag();
                            artistName = id3v1Tag.getArtist();
                            trackName = id3v1Tag.getTitle();
                        }
                        if(song!=null && song.hasId3v2Tag())
                        {
                            ID3v2 id3v2tag = song.getId3v2Tag();
                            artistName = id3v2tag.getArtist();
                            trackName = id3v2tag.getTitle();
                        }
                        if(artistName != null && artistName.length() >= 1)
                        {
                            if(artistName.charAt(0) >= startChar && artistName.charAt(0) <= endChar)
                            {
                                ArrayList<String> songAttributes = new ArrayList<>();
                                songAttributes.add(path + '/' + names[i] + '/' + fileName);
                                songAttributes.add(artistName);
                                songAttributes.add(trackName);
                                pathOfMusicFiles.add(songAttributes);
                            }
                        }
                    }
                }
            }
        }
        return pathOfMusicFiles;
    }
}