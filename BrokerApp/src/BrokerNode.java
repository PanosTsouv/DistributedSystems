import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class BrokerNode implements Broker {

    //attributes of broker as Server
    private String brokerID;
    private String ownPort;
    private String ownServerIP;
    private String hashBroker;
    private int numberOfBrokers;
    private ServerSocket providerSocket = null;
    private Socket connection = null;

    private String serverIP;
    private String port;
    private Socket connectionAsClient = null;
    private ObjectInputStream inAsClient = null;
    private ObjectOutputStream outAsClient = null;

    private ArrayList<String> attributes = new ArrayList<>();
    private HashMap<String, String> artistToBrokers = new HashMap<>();
    private HashMap<String, String> artistToPublisher = new HashMap<>();
    private HashMap<String, Value[]> songsInCache = new HashMap<>();

    public BrokerNode(){}

    public BrokerNode(String ownPort, String brokerID, int numberOfBrokers) {

        this.brokerID = brokerID;
        this.ownPort = ownPort;
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e2) {
            e2.printStackTrace();
        }
        for (NetworkInterface netint : Collections.list(nets))
        {
            if (netint.getName().equals("eth1") && netint.getInterfaceAddresses().size()>0)
            {
                this.ownServerIP = netint.getInterfaceAddresses().get(0).getAddress().getHostAddress();
            }
			else
			{
				try 
				{
					this.ownServerIP = InetAddress.getLocalHost().getHostAddress();
				} 
				catch (UnknownHostException e) 
				{
					e.printStackTrace();
				}
			}
        }
        this.numberOfBrokers = numberOfBrokers;
        calculateKeys();

        attributes.add(this.brokerID);
        attributes.add(this.ownPort);
        attributes.add(this.ownServerIP);
        attributes.add(this.hashBroker);
        brokersInfo.add(attributes);
        System.out.println("Broker " + this.brokerID + " BrokerIp: " + this.ownServerIP + " Port: " + this.ownPort + " ServerHash: " + this.hashBroker + "\n");
    }

    
    /////////////////////////////////////////////////
    ///////////////                  ///////////////
    //////////////    CLIENT PART   ///////////////
    /////////////                  ///////////////
    /////////////////////////////////////////////

    @Override
    public void init() {
        try 
        {
            connectionAsClient = new Socket(this.serverIP, Integer.parseInt(this.port));
            outAsClient = new ObjectOutputStream(connectionAsClient.getOutputStream());
            inAsClient = new ObjectInputStream(connectionAsClient.getInputStream());
            System.out.println("Client part of Broker :: Connected.");
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
    public void connect() throws IOException {
       

    }

    @Override
    public void disconnect(){
        try 
        {
            inAsClient.close();
            outAsClient.close();
            connectionAsClient.close();
            System.out.println("Client part of Broker :: Disconnected.");
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void connectWithBrokers(){
        try 
        {
            outAsClient.writeObject("BrokerNode");
            outAsClient.writeObject(this.attributes);
            outAsClient.flush();
        }
        catch(IOException e) 
        {
            e.printStackTrace();
        }
        System.out.println("Client part of broker :: Send broker signature");
        disconnect();
    }

    public void setConnectionServerIP()
    {
        System.out.println("Client part of broker :: Give the serverIP of connection");
        this.serverIP = System.console().readLine();
    }

    public void setConnectionPort()
    {
        System.out.println("Client part of broker :: Give the port of connection");
        this.port = System.console().readLine();
    }

    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    public void setPort(String port)
    { 
        this.port = port;
    }

    public ObjectInputStream getInAsClient()
    {
        return this.inAsClient;
    }

    public ObjectOutputStream getOutAsClient()
    { 
        return this.outAsClient;
    }


    /////////////////////////////////////////////////
    ///////////////                  ///////////////
    //////////////   SERVER PART    ///////////////
    /////////////                  ///////////////
    /////////////////////////////////////////////

    public void openServer() 
    {
        try 
        {
            providerSocket = new ServerSocket(Integer.parseInt(this.ownPort));
            while (true) 
            {
                connection = providerSocket.accept();
                System.out.println("Server part of broker :: Client connected.");
                ActionsForClients add = new ActionsForClients(connection, this);
                System.out.println("Server part of broker :: Handler created.");
                new Thread(add).start();
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
        finally 
        {
            try 
            {
                providerSocket.close();
            } 
            catch (IOException ioException) 
            {
                ioException.printStackTrace();
            }
        }
    }

    
    public int getNumbersOfBroker()
    {
        return this.numberOfBrokers;
    }

    @Override
    public ArrayList<ArrayList<String>> getBrokersInfo() {
        
        return brokersInfo;
    }


    @Override
    public void calculateKeys() {
        BigInteger sha1 = null;
        try {
            MessageDigest msdDigest = MessageDigest.getInstance("MD5");
            byte[] messageDigest = msdDigest.digest((this.serverIP + this.ownPort).getBytes());
            sha1 = new BigInteger(1, messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.hashBroker = sha1.toString().substring(0, 3);
    }

    @Override
    public void pull(ArtistName artistName, String songName, ObjectOutputStream outConsumer) 
    {
        try 
        {
            outConsumer.writeObject(getSongsInCache().get(songName).length);
            int count = 0;
            while (count != getSongsInCache().get(songName).length)
            {
                while(getSongsInCache().get(songName)[count] == null)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                outConsumer.writeObject(getSongsInCache().get(songName)[count]);
                count++;
            }
            outConsumer.flush();
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }

    }

    

    @Override
    public String getBrokerID() {
        return brokerID;
    }

    public HashMap<String,Value[]> getSongsInCache()
    {
        return this.songsInCache;
    }

	public HashMap<String, ArrayList<String>> getRegisteredPublishers()
    {
        return registeredPublishers;
    }

    public ArrayList<ArrayList<String>> getRegisteredUsers()
    {
        return registeredUsers;
    }

    public HashMap<String,String> getArtistToBrokers()
    {
        return this.artistToBrokers;
    }

    public HashMap<String,String> getArtistToPublisher()
    {
        return this.artistToPublisher;
    }

    
}