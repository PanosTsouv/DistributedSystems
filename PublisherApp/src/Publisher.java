import java.io.IOException;

public interface Publisher extends Node {

	void getBrokerList();
	String hashTopic(ArtistName artistName);
	void push(ArtistName artistName, Value musicFile) throws IOException;

}