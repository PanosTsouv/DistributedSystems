import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public interface Broker extends Node{
	ArrayList<ArrayList<String>> registeredUsers = new ArrayList<>();
	HashMap<String, ArrayList<String>> registeredPublishers = new HashMap<>();
	
	void calculateKeys();
	void pull(ArtistName artistName, String songName, ObjectOutputStream outConsumer);
	String getBrokerID();
}
	