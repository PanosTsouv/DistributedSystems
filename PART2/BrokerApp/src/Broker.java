import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import Utils.ArtistName;

public interface Broker extends Node{
	HashMap<String, ArrayList<String>> registeredUsers = new HashMap<>();
	HashMap<String, ArrayList<String>> registeredPublishers = new HashMap<>();
	
	void calculateKeys();
	void pull(ArtistName artistName, String songName, ObjectOutputStream outConsumer);
	String getBrokerID();
}
	