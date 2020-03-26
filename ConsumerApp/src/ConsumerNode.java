import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ConsumerNode implements Consumer{

    private String consumerName;
    private String consumerPassword;
    private String serverIP;
    private String port;

    private ArrayList<String> attributes = new ArrayList<>();

    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    public ConsumerNode(String consumerName, String consumerPassword, String serverIP, String port) 
    {
        this.consumerName = consumerName;
        this.consumerPassword = consumerPassword;
        this.serverIP = serverIP;
        this.port = port;
        attributes.add(this.consumerName);
        attributes.add(this.consumerPassword);
        attributes.add(this.serverIP);
        attributes.add(this.port);
    }

    @Override
    public void register(int brokerID, ArtistName artistName){}

    @Override
    public void disconnect(int brokerID, ArtistName artistName){}

    @Override
    public void playData(ArtistName artistName, Value musicFile){}

    @Override
    public void init() 
    {
        try 
        {
            requestSocket = new Socket(serverIP, Integer.parseInt(port));
            System.out.println(requestSocket.getInetAddress());
            System.out.println(requestSocket.getPort());
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } 
        catch(UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown host!");
        } 
        catch(IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    @Override
    public ArrayList<ArrayList<String>> getBrokersInfo() 
    {
        return brokersInfo;
    }

    @Override
    public void connect() 
    {
        try 
        {
            out.writeObject("ConsumerNode");
            System.out.println("Client type sent.");
            out.writeObject(attributes);
            out.flush();
            Info answer = (Info)in.readObject();
            System.out.println(answer.getBrokers());
            System.out.println(answer.getArtistToBroker());   
            disconnect();
        } 
        catch(IOException | ClassNotFoundException e) 
        {  
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() 
    {
        try 
        {
            in.close(); 
            out.close();
            requestSocket.close();
        } 
        catch(IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    @Override
    public void updateNodes() {}

    public String getConsumerName()
    {
        return this.consumerName;
    }

    public String getConsumerPassword()
    {
        return this.consumerPassword;
    }

    public String getServerIP()
    {
        return this.serverIP;
    }

    public String getPort()
    {
        return this.port;
    }
}
