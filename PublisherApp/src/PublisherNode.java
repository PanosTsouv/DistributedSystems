import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    private ArrayList<ArrayList<String>> songsInfo;
    private HashMap<String, String> uniqueArtistToBroker = new HashMap<>();
    private ArrayList<String> attributes = new ArrayList<>();
    private ServerSocket providerSocket = null;
    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    public PublisherNode(String publisherID, String port, String ownPort, String serverIP, String path, char start, char end)
    {
        this.publisherID = publisherID;
        this.port = port;
        this.path = path;
        this.start = start;
        this.end = end;
        try
        {
            this.ownServerIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        this.ownPort = ownPort;
        System.out.println("Publisher " + this.publisherID + " PublisherIp: " + this.ownServerIP + " Port " + this.ownPort + "\n");
        this.serverIP = serverIP;
        attributes.add(this.publisherID);
        attributes.add(this.ownServerIP);
        attributes.add(this.ownPort);
        attributes.add(this.publisherConnectFirstTime);
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

    @Override
    public String hashTopic(ArtistName artistName) 
    {
        String artistHash;
        BigInteger sha1 = null;
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
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
            msdDigest.update(artistName.getArtistName().getBytes("UTF-8"), 0, artistName.getArtistName().length());
            sha1 = new BigInteger(1, msdDigest.digest());
        } 
        catch (UnsupportedEncodingException | NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        artistHash = sha1.toString(10).substring(0,3);
        artistHash = Integer.toString(Integer.parseInt(artistHash) % tempBrokerHashAsList.get(tempBrokerHashAsList.size()-1));
        

        for(int element : tempBrokerHashAsList)
        {
            if(Integer.parseInt(artistHash) < element)
            {
                brokerId = Integer.toString(tempBrokerHashAsMap.get(element));
                break;
            }
        }
        return brokerId;
    }

    @Override
    public void push(ArtistName artistName, Value musicFile, ObjectOutputStream outToBroker) throws IOException
    {
        outToBroker.writeObject(musicFile);
        outToBroker.flush();
    }

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
            ioException.printStackTrace();
        }
    }

    @Override
    public ArrayList<ArrayList<String>> getBrokersInfo() 
    {
        return brokersInfo;
    }

    @Override
    public void connect() throws IOException {}

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

    public void acceptConnectionBroker(Socket requestSocket, PublisherNode publisher) {
        System.out.println("Server part of publisher :: Broker Client detected");
        BrokerHandlerThread action = new BrokerHandlerThread(requestSocket, publisher, this.songsInfo);
        System.out.println("Server part of publisher :: Handler created.");
        new Thread(action).start();
    }

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

    public void sendInitialDataToBrokers()
    {
        for(int i = 0; i < brokersInfo.size(); i++)
        {
            try
            {
                requestSocket = new Socket(brokersInfo.get(i).get(2), Integer.parseInt(brokersInfo.get(i).get(1)));
                System.out.println("Client part of publisher :: Publisher " + this.publisherID + " is successfully connected with server with IP: " + brokersInfo.get(i).get(2) + " Port: " + brokersInfo.get(i).get(1));
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());
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
    }
}
