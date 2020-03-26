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

    private ArrayList<ArrayList<String>> songsInfo;
    private ArrayList<String> ID_hashes;
    private HashMap<String, String> uniqueArtistToBroker = new HashMap<>();
    private ArrayList<String> attributes = new ArrayList<>();
    private static transient HashMap<String, Queue<String>> musicMap;

    private ServerSocket providerSocket = null;
    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    public PublisherNode(String publisherID, String port, String ownPort, String serverIP)
    {
        this.publisherID = publisherID;
        try
        {
            this.ownServerIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        System.out.println("Server part of publisher has ServerIp: " + this.ownServerIP);
        
        this.port = port;
        this.ownPort = ownPort;
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
            System.out.println("Client type sent");
            out.writeObject(attributes);

            ArrayList<ArrayList<String>> request = (ArrayList<ArrayList<String>>) in.readObject();
            for (ArrayList<String> element : (ArrayList<ArrayList<String>>)request)
            {
                brokersInfo.add(element);
                System.out.println("Broker with id " + element.get(0) + " has hash code: " + element.get(3));
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

    @Override
    public String hashTopic(ArtistName artistName) 
    {
        String artsishHash;
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
        artsishHash = sha1.toString(10).substring(0,3);
        artsishHash = Integer.toString(Integer.parseInt(artsishHash) % tempBrokerHashAsList.get(tempBrokerHashAsList.size()-1));
        

        for(int element : tempBrokerHashAsList)
        {
            if(Integer.parseInt(artsishHash) < element)
            {
                brokerId = Integer.toString(tempBrokerHashAsMap.get(element));
                break;
            }
        }
        return brokerId;
    }

    @Override
    public void push(ArtistName artistName, Value musicFile) throws IOException{}

    @Override
    public void init()
    {
        try
        {
            requestSocket = new Socket(serverIP, Integer.parseInt(port));
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
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
            System.out.println("Connection ended!");
        }
        catch (IOException IOE)
        {
            IOE.printStackTrace();
        }
    }

    @Override
    public void updateNodes(){}

    private boolean hasData()
    {
        return true;
    }

    public void openServer()
    {
        try
        {
            providerSocket = new ServerSocket(Integer.parseInt(this.ownPort), 10);
            System.out.println("Server part of publisher is waiting at port: 4500");
            while(true) 
            {
                
                requestSocket = providerSocket.accept();
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());
                Object request = in.readObject();
            }
        } 
        catch (IOException | ClassNotFoundException e)
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
        ReadDataBase a = new ReadDataBase("C:/Users/Panagiotis/Desktop/Distributed Systems/PublisherApp/dataset1", 'A', 'J');
        songsInfo = a.readthePathOfMusicFiles();
        for(ArrayList<String> element : songsInfo)
        { 
            uniqueArtistToBroker.put(element.get(1), hashTopic(new ArtistName(element.get(1))));
        }
        for(Map.Entry<String, String> entry : uniqueArtistToBroker.entrySet()) 
        {
		    System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

    public void sendInitialDataToBrokers()
    {
        for(int i = 0; i < brokersInfo.size(); i++)
        {
            try
            {
                requestSocket = new Socket(brokersInfo.get(i).get(2), Integer.parseInt(brokersInfo.get(i).get(1)));
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());
                out.writeObject("PublisherNode");
                out.writeObject(attributes);
                out.writeObject(uniqueArtistToBroker);
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
