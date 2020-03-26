import java.io.Serializable;

public class MusicFile implements Serializable{
	
	private static final long serialVersionUID = 3047038005316037603L;
	private String trackName;
	private String artistName;
	private String albumInfo;
	private String genre;
	private byte[] musicFileExtract;
	
	MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] musicFileExtract){
		this.trackName = trackName;
		this.artistName = artistName;
		this.albumInfo = albumInfo;
		this.genre = genre;
		this.musicFileExtract = musicFileExtract;
	}
	
	//Getters
	public String getTrackName(){ return trackName; }
	public String getArtistName() { return artistName; }
	public String getAlbumInfo() { return albumInfo; }
	public String getGenre() { return genre; }
	public byte[] getMusicFileExtract() { return musicFileExtract; }
	
	//Setters
	public void setTrackName(String trackName) { this.trackName = trackName; }
	public void setArtistName(String artistName) { this.artistName = artistName; }
	public void setAlbumInfo(String albumInfo) { this.albumInfo = albumInfo; }
	public void setGenre(String genre) { this.genre = genre; }
	public void setMusicFileExtract(byte[] musicFileExtract) { this.musicFileExtract = musicFileExtract; }
}
