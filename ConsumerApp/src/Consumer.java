public interface Consumer extends Node{
	void register();
	void unregister();
	void playData(ArtistName artistName, Value musicFile);
}