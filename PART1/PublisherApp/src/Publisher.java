import java.io.IOException;
import java.io.ObjectOutputStream;

public interface Publisher extends Node {

	void getBrokerList();
	String hashTopic(ArtistName artistName);
	void push(ArtistName artistName, Value musicFile, ObjectOutputStream outToBroker) throws IOException;

}