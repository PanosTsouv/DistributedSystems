public interface Consumer extends Node{
	void register(int brokerID, ArtistName artistName);
	void disconnect(int brokerID, ArtistName artistName);
	void playData(ArtistName artistName, Value musicFile);
}