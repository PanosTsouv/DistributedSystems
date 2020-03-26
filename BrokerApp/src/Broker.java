import java.util.ArrayList;

public interface Broker extends Node{
	ArrayList<ArrayList<String>> registeredUsers = new ArrayList<>();
	ArrayList<ArrayList<String>> registeredPublishers = new ArrayList<>();
	
	void calculateKeys();
	void acceptConnectionPublisher(ArrayList<String> PN);
	void acceptConnectionConsumer(ArrayList<String> CN);
	void notifyPublisher(String message);
	void pull(ArtistName artistName);
	String getHashBroker();
	void setHashBroker(String hashBroker);
	String getBrokerID();
}
	