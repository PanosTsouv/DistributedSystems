import java.io.IOException;
import java.util.ArrayList;

public interface Node {
	
	ArrayList<ArrayList<String>> brokersInfo = new ArrayList<>();
	
	void init();
	ArrayList<ArrayList<String>> getBrokersInfo();
	void connect() throws IOException;
	void disconnect();
	void updateNodes();
}
