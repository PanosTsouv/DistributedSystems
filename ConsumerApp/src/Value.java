public class Value{
	
	private MusicFile musicFile;
	
	Value(MusicFile musicFile){ this.musicFile = musicFile; }
	
	public MusicFile getMusicFile(){ return musicFile; }
	public void setMusicFile(MusicFile musicFile) { this.musicFile = musicFile; }
}