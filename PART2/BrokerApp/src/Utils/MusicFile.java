package Utils;

import java.io.Serializable;

public class MusicFile implements Serializable {
	
	private static final long serialVersionUID = 3047038005316037603L;
	private String trackName;
	private String artistName;
	private String albumInfo;
	private String genre;
	private byte[] musicFileExtract;
	private long trackDuration;
	private long trackLength;
	private long firstTrackDuration;
	
	public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] musicFileExtract, long trackDuration, long trackLength)
	{
		this.trackName = trackName;
		this.artistName = artistName;
		this.albumInfo = albumInfo;
		this.genre = genre;
		this.musicFileExtract = musicFileExtract;
		this.trackDuration = trackDuration;
		this.trackLength = trackLength;
		this.firstTrackDuration = 0;
	}
	
	//Getters
	public String getTrackName(){ return trackName; }
	public String getArtistName() { return artistName; }
	public String getAlbumInfo() { return albumInfo; }
	public String getGenre() { return genre; }
	public byte[] getMusicFileExtract() { return musicFileExtract; }
	public long getTrackDuration() { return trackDuration; }
	public long getTrackLength() { return trackLength; }
	public long getFirstTrackDuration() { return firstTrackDuration; }
	
	//Setters
	public void setTrackName(String trackName) { this.trackName = trackName; }
	public void setArtistName(String artistName) { this.artistName = artistName; }
	public void setAlbumInfo(String albumInfo) { this.albumInfo = albumInfo; }
	public void setGenre(String genre) { this.genre = genre; }
	public void setMusicFileExtract(byte[] musicFileExtract) { this.musicFileExtract = musicFileExtract; }
	public void setTrackDuration(long trackDuration) { this.trackDuration = trackDuration; }
	public void setTrackLength(long trackLength) { this.trackLength = trackLength; }
	public void setFirstTrackDuration(long firstTrackDuration) { this.firstTrackDuration = firstTrackDuration; }
}
