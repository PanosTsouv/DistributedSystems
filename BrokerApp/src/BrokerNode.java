import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class BrokerNode implements Broker{
    
    private String brokerID;
    private String port;
    private String serverIP;
    private String hashBroker;
    private int numberOfBrokers;

    private ArrayList<String> attributes = new ArrayList<>();
    private HashMap<String,String> artistToBrokers = new HashMap<>();
    private HashMap<String,String> artistToPublisher = new HashMap<>();

    private ServerSocket providerSocket = null;
    private Socket connection = null;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public BrokerNode(String port, String brokerID, int numberOfBrokers) 
    {

        try 
        {
            this.serverIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) 
        {
            e.printStackTrace();
        }
        this.brokerID = brokerID;
        this.port = port;
        this.numberOfBrokers = numberOfBrokers;
        calculateKeys();
        attributes.add(this.brokerID);
        attributes.add(this.port);
        attributes.add(this.serverIP);
        attributes.add(this.hashBroker);
        brokersInfo.add(attributes);
        System.out.println("Broker " + this.brokerID + " BrokerIp: " + this.serverIP + " Port: " + this.port + " ServerHash: " + this.hashBroker);
    }

    @Override
    public void calculateKeys() 
    {
        BigInteger sha1 = null;
        try 
        {
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
            msdDigest.update((this.port + this.serverIP).getBytes("UTF-8"), 0, (this.port + this.serverIP).length());
            sha1 = new BigInteger(1, msdDigest.digest());
        } 
        catch (UnsupportedEncodingException | NoSuchAlgorithmException e) 
        {
            e.printStackTrace();
        }
        this.hashBroker = sha1.toString(10).substring(0,3);

    }

    @Override
    public void acceptConnectionPublisher(ArrayList<String> publisherInfo) 
    {
        System.out.println("Publisher Client detected");
        PublisherHandlerThread action = new PublisherHandlerThread(out, in, publisherInfo, this);
        System.out.println("Handler created.");
        new Thread(action).start();
    }

    @Override
    public void acceptConnectionConsumer(ArrayList<String> consumerInfo) 
    {
        System.out.println("Consumer Client detected");
        ConsumerHandlerThread action = new ConsumerHandlerThread(out, in, consumerInfo, this);
        System.out.println("Handler created.");
        new Thread(action).start();
    }

    @Override
    public void notifyPublisher(String message){}

    @Override
    public void pull(ArtistName artistName){}

    @SuppressWarnings("unchecked")
    public void openServer() 
    {
        try 
        {
            providerSocket = new ServerSocket(Integer.parseInt(port));
            while (true) 
            {
                connection = providerSocket.accept();
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                new Thread()
                {
                    public void run()
                    {
                        try 
                        {
                            String type;
                            type = (String) in.readObject();
                            ArrayList<String> info = (ArrayList<String>) in.readObject();
                            System.out.println(type);
                            if(type.equals("ConsumerNode")) 
                            {
                                acceptConnectionConsumer(info);
                            } 
                            else if(type.equals("PublisherNode")) 
                            {
                                acceptConnectionPublisher(info);
                            }
                            else 
                            {
                                brokersInfo.add(info);
                                System.out.println("Receive another broker and add it to the list");
                            }
                        } catch (ClassNotFoundException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
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

    @Override
    public void init() 
    {
        for(int i = 0; i < this.numberOfBrokers-1; i++)
        {
            System.out.println("Give the serverIP of connection");
            String connectionserverIP = System.console().readLine();
            System.out.println("Give the port of connection");
            int connectionPort = Integer.parseInt(System.console().readLine());
            try 
            {
                connection = new Socket(connectionserverIP, connectionPort);
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
            } 
            catch(UnknownHostException unknownHost)
            {
                System.err.println("You are trying to connect to an unknown host!");
            }
            catch(IOException ioException)
            {
                ioException.printStackTrace();
            }
            connectWithBrokers();
        }
    }

    @Override
    public ArrayList<ArrayList<String>> getBrokersInfo(){
        return brokersInfo;
    }

    public String getBrokerID()
    {
        return this.brokerID;
    }

    public String getHashBroker() 
    {
        return hashBroker;
    }

    public void setHashBroker(String hashBroker) 
    {
        this.hashBroker = hashBroker;
    }

    @Override
    public void connect() 
    {
        try 
        {
            out.writeObject(null);
        } 
        catch(IOException e) 
        {
            e.printStackTrace();
        }
        System.out.println("Send broker list");
        disconnect();
    }

    @Override
    public void disconnect()
     {
        try 
        {
            in.close();
            out.close();
            connection.close();
        }
        catch (IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    @Override
    public void updateNodes(){}

    public void connectWithBrokers(){
        try 
        {
            for(ArrayList<String> element : brokersInfo)
            {
                if(element.get(0) == this.brokerID)
                {
                    out.writeObject("BrokerNode");
                    out.writeObject(element);
                    out.flush();
                }
            }
        }
        catch(IOException e) 
        {
            e.printStackTrace();
        }
        System.out.println("Send broker signature");
        disconnect();
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
