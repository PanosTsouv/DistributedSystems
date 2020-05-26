import java.io.IOException;
import java.io.ObjectOutputStream;

import Utils.ArtistName;
import Utils.Value;

public interface Publisher extends Node {

	void getBrokerList();
	String hashTopic(ArtistName artistName);
	void push(ArtistName artistName, Value musicFile, ObjectOutputStream outToBroker) throws IOException;

}