package Utils;

import java.io.Serializable;

public class Value implements Serializable{
	
	private static final long serialVersionUID = 2698505294638713122L;
	private MusicFile musicFile;
	
	Value(MusicFile musicFile){ this.musicFile = musicFile; }
	
	public MusicFile getMusicFile(){ return musicFile; }
	public void setMusicFile(MusicFile musicFile) { this.musicFile = musicFile; }
}