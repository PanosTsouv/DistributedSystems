import java.io.*;
import java.net.*;
import java.util.ArrayList;

//@serverIP : is the IP of server which the consumer connect
//@port : is the port of server which the consumer connect
//@attributes : contain (userName and Password)....server can take these attributes and save a user to a register list
//@requestSocket : is the connection between server-client....client open a socket to connect with server
//@out@in : client use these streams to communicate with server

//IDEA:
// (1)   Consumer make a random server connect ->
// (2)-> Server send back Info object(message) which contains a list with all brokers(servers) and their attributes(serverID-serverIP-Port) --- 
//       a hashMap with all artistNames as KEYS and brokerID(server) as values ->
// (3)-> we put from this hashMap an artistName which consumer(user) choose and we take the brokerID(serverID) which is responsible for this artistName ->
// (4)-> if this random server contain user's artistName we register this user to this broker(server) and make the request
//       else we should disconnect from this broker(server) ->
// (5)-> find the right broker(server) from list of brokers(so we have his serverIP and Port) ->
// (6)-> register to right broker(server) and make the request
public class ConsumerNode implements Consumer{

    private String consumerName;
    private String consumerPassword;
    private String serverIP;
    private String port;

    private ArrayList<String> attributes = new ArrayList<>();

    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    //constructor + initialize attributes list which we send to server ----- server has access to this list
    //IMPORTANT -- if you want server know a attribute of consumer you should add it to the attributes list 
    public ConsumerNode(String consumerName, String consumerPassword, String serverIP, String port) 
    {
        this.consumerName = consumerName;
        this.consumerPassword = consumerPassword;
        this.serverIP = serverIP;
        this.port = port;

        attributes.add(this.consumerName);
        attributes.add(this.consumerPassword);
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
            requestSocket = new Socket(serverIP, Integer.parseInt(port));//open a connection with a broker(server)
            System.out.println(requestSocket.getInetAddress());//(debug)
            System.out.println(requestSocket.getPort());//(debug)
            out = new ObjectOutputStream(requestSocket.getOutputStream());//receive a message
            in = new ObjectInputStream(requestSocket.getInputStream());//send a message
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
            out.writeObject("ConsumerNode");//Brocker should know the type of client
            System.out.println("Client type sent.");
            out.writeObject(attributes);//send a list of attributes(consumer name - password - first time connect)
            out.flush();
            Info answer = (Info)in.readObject();//Info obejct is a message which contains broker List with broker's attributes
            System.out.println(answer.getBrokers());//(debug)
            System.out.println(answer.getArtistToBroker());//(debug)   
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
