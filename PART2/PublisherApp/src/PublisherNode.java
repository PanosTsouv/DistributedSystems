import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import Utils.ArtistName;
import Utils.Value;

//@param publisherID : unique identifier
//@param ownPort : port which publisher listens
//@param ownServerIP : client needs to know this ip to connect with publisher
//@param publisherConnectFirstTime : true(publisher dont have the broker info)
//@param serverIP : the ip of server which client part of publisher want connect
//@param port : the port of server which client part of publisher want connect
//@param path : path of database 
//@param start,end : range of artist which publisher is responsible
//@param requestSocket : is the connection between server-client....client part of publisher open a socket to connect with server
//@param out,in : client part of publisher use these streams to communicate with server

//IDEA:
// (1)   Publisher connects to a random server ->
// (2)-> Receives a list with all brokers(servers) and their attributes(serverID-serverIP-port)
// (3)-> calculates servers hash(serverIP+port) values, artists hash values and create a hashMap with key(ArtistName) and value(BrokerID)->
// (4)-> sends this hashMap to all servers so they can forward a request to a specific publisher ->
// (5)-> publisher accepts a connection, creates a new thread and does 2 jobs 1)return a list with all songs of a specific artist 
//       2)handle the request,create chunks and push them to server which is responsible for this publisher

public class PublisherNode implements Publisher{
    private String publisherID;
    private String ownPort;
    private String publisherConnectFirstTime = "true";
    private String port;
    private String ownServerIP;
    private String serverIP;
    private String path;
    private char start;
    private char end;
    private String displayNameOfConnectionInterface;

    //publicConnectionattribute
    private String publicOwnInternetIP;
    private String publicOwnInternetPort;
    private boolean isPublicConnectionEstablished;

    private ArrayList<ArrayList<String>> songsInfo;
    private HashMap<String, String> uniqueArtistToBroker = new HashMap<>();
    private ArrayList<String> attributes = new ArrayList<>();
    private ServerSocket providerSocket = null;
    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    //constructor + initialize attributes list which we send to clients
    public PublisherNode(String publisherID, String ownPort, String publicOwnInternetPort, String isPublicConnectionEstablished, String port, String serverIP, String path, char start, char end, String displayNameOfConnectionInterface)
    {
        this.publisherID = publisherID;
        this.port = port;
        this.serverIP = serverIP;
        this.path = path;
        this.start = start;
        this.end = end;
        this.displayNameOfConnectionInterface = displayNameOfConnectionInterface;
        if(isPublicConnectionEstablished.equals("true"))
        {
            this.isPublicConnectionEstablished = true;
        }
        getOwnIP();
        this.ownPort = ownPort;

        attributes.add(this.publisherID);
        attributes.add(this.ownServerIP);
        attributes.add(this.ownPort);
        attributes.add(this.publisherConnectFirstTime);
        System.out.println("Publisher " + this.publisherID + " PublisherIp: " + attributes.get(1) + " Port " + attributes.get(2));
        if(this.isPublicConnectionEstablished)
        {
            this.publicOwnInternetPort = publicOwnInternetPort;
            getPublicIP();
            if(!this.publicOwnInternetIP.equals(this.ownServerIP))
            {
                attributes.set(1, this.publicOwnInternetIP);
                attributes.set(2, this.publicOwnInternetPort);
                System.out.println("Publisher " + this.publisherID + " PublicPublisherIp: " + attributes.get(1) + " PublicPort " + attributes.get(2) + "\n");
            }
        }
    }

    private void getOwnIP()
    {
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e2) {
            e2.printStackTrace();
        }
        for (NetworkInterface netint : Collections.list(nets))
        {
            if (netint.getDisplayName().equals(this.displayNameOfConnectionInterface) && netint.getInterfaceAddresses().size() > 0)
            {
                this.ownServerIP = netint.getInterfaceAddresses().get(0).getAddress().getHostAddress();
                break;
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
    }

    private void getPublicIP()
    {
        try
        { 
            URL url_name = new URL("http://bot.whatismyipaddress.com"); 
  
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream())); 
  
            // reads system IPAddress 
            this.publicOwnInternetIP = sc.readLine().trim(); 
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            this.publicOwnInternetIP = this.ownServerIP; 
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getBrokerList() 
    {
        try
        {
            out.writeObject("PublisherNode");
            out.flush();
            System.out.println("Client part of publisher :: Sends its type");
            out.writeObject(attributes);
            out.flush();

            ArrayList<ArrayList<String>> request = (ArrayList<ArrayList<String>>) in.readObject();
            System.out.println("Client part of publisher :: Receives broker's list and it start print this list....");
            for (ArrayList<String> element : (ArrayList<ArrayList<String>>)request)
            {
                brokersInfo.add(element);
                System.out.println("{Broker " + element.get(0) + " has hash code: " + element.get(3) + "}");
            }
            out.flush();
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        this.publisherConnectFirstTime = "false";
        this.attributes.set(3, this.publisherConnectFirstTime);
        disconnect();
    }

    public ArrayList<ArrayList<String>> getSongInfo()
    {
        return this.songsInfo;
    }

    //take as input an artistName ,calculate hash value and return the if of broker whick is responsible for this artist
    //@param tempBrokerHashAsList contains brokes's hash values which we sort 
    //@param tempBrokerHashAsMap contains brokes's hash values as key and brokerID as value so we know after sort which broker has a specifiq value
    //Second for loop find the first broker in sorted list that his hash value is bigger than artist'hash value 
    //if all hash values of brokers is lower than artist hash value then the method return the broker with the lowest hash value
    @Override
    public String hashTopic(ArtistName artistName) 
    {
        String artistHash;
        BigInteger md5 = null;
        String brokerId = "";
        HashMap<Integer,Integer> tempBrokerHashAsMap = new HashMap<>();
        ArrayList<Integer> tempBrokerHashAsList = new ArrayList<>();
        
        for(ArrayList<String> element : brokersInfo)
        {
            tempBrokerHashAsMap.put(Integer.parseInt(element.get(3)), Integer.parseInt(element.get(0)));
            tempBrokerHashAsList.add(Integer.parseInt(element.get(3)));
        }
        Collections.sort(tempBrokerHashAsList);
        
        try
        {
            MessageDigest msdDigest = MessageDigest.getInstance("MD5");
            byte[] messageDigest = msdDigest.digest(artistName.getArtistName().getBytes());
            md5 = new BigInteger(1, messageDigest);
        } 
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        artistHash = md5.toString(10).substring(0,3);
        

        for(int element : tempBrokerHashAsList)
        {
            if(Integer.parseInt(artistHash) > tempBrokerHashAsList.get(tempBrokerHashAsList.size()-1))
            {
                brokerId = Integer.toString(tempBrokerHashAsMap.get(tempBrokerHashAsList.get(0)));
                break;
            }
            if(Integer.parseInt(artistHash) < element)
            {
                brokerId = Integer.toString(tempBrokerHashAsMap.get(element));
                break;
            }
        }
        return brokerId;
    }

    //forward a chunk to a specific broker
    @Override
    public void push(ArtistName artistName, Value musicFile, ObjectOutputStream outToBroker) throws IOException
    {
        outToBroker.writeObject(musicFile);
        outToBroker.flush();
    }

    //open a connection with server
    @Override
    public void init()
    {
        try
        {
            requestSocket = new Socket(serverIP, Integer.parseInt(port));
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            System.out.println("Client part of publisher :: Publisher " + this.publisherID + " is successfully connected with server with IP: " + serverIP + " Port: " + port);
        } 
        catch(UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown host!");
        } 
        catch (IOException ioException) 
        {
            System.err.println("You are trying to connect to an offline server.Check the server IP and port");
            System.exit(1);
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
            out.writeObject("PublisherNode");
            out.flush();
            out.writeObject(attributes);
            out.flush();
            System.out.println("Client part of publisher :: Sends its type");
            out.writeObject(uniqueArtistToBroker);
            out.flush();
            System.out.println("Client part of publisher :: Sends a HashMap {Key:Artist , Value:BrokerID}");
            disconnect();
        } 
        catch (UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown host!");
        } 
        catch (IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    //end a connection with server
    @Override
    public void disconnect() 
    {
        try
        {
            out.close();
            in.close();
            requestSocket.close();
            System.out.println("Client part of publisher :: Publisher " + this.publisherID + " disconnected");
        }
        catch (IOException IOE)
        {
            IOE.printStackTrace();
        }
    }

    //create a new thread every time the server part of publisher receives a request
    public void acceptConnectionBroker(Socket requestSocket, PublisherNode publisher) {
        System.out.println("Server part of publisher :: Broker Client detected");
        BrokerHandlerThread action = new BrokerHandlerThread(requestSocket, publisher, this.songsInfo);
        System.out.println("Server part of publisher :: Handler created.");
        new Thread(action).start();
    }

    //open the server part of publisher
    public void openServer()
    {
        try
        {
            providerSocket = new ServerSocket(Integer.parseInt(this.ownPort));
            System.out.println("Server part of publisher is waiting at port: " + this.ownPort);
            while(true) 
            {
                requestSocket = providerSocket.accept();
                acceptConnectionBroker(requestSocket, this);
            }
        } 
        catch (IOException  e)
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

    //every new publisher read the database and finds the artists which he is responsible
    //create a hashMap with artistName as key and brokerID as value
    public void calculateBrokerArtistMap()
    {
        ReadDataBase a = new ReadDataBase(this.path, this.start, this.end);
        songsInfo = a.readthePathOfMusicFiles();
        for(ArrayList<String> element : songsInfo)
        { 
            uniqueArtistToBroker.put(element.get(1), hashTopic(new ArtistName(element.get(1))));
        }
        System.out.println("Server part of publisher :: Calculates and prints a HashMap with Key: Artist and Value: BrokerID");
        for(Map.Entry<String, String> entry : uniqueArtistToBroker.entrySet()) 
        {
		    System.out.println("{Artist: " + entry.getKey() + " -> Broker " + entry.getValue() + "}");
        }
    }

    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    public void setPort(String port)
    {
        this.port = port;
    }
}