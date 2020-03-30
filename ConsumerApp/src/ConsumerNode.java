import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

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
    private String connectForFirstTime;
    private String artistName;
    private String songName;
    private Info answer = null;

    private ArrayList<String> attributes = new ArrayList<>();
    private ArrayList<File> streaming = new ArrayList<>();

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
        this.connectForFirstTime = "true";

        attributes.add(this.consumerName);
        attributes.add(this.consumerPassword);
        attributes.add(this.connectForFirstTime);
    }

    //we use register method when the broker, which user connect for first time,is wrong
    @Override
    public void register(){
        init();
        connect();
    }

    @Override
    public void unregister() {
        try {
            out.writeObject("I want to unregister");
            String answer = (String) in.readObject();
            System.out.println(answer);
            in.close();
            out.close();
            requestSocket.close();
            System.out.println("Client unregister from server...");
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        }
    }

    @Override
    public void playData() {
        while (streaming.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("We are playing data");
        int count = 0;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        Player player = null;

        while (count != streaming.size()) {
            try {
                fis = new FileInputStream(this.streaming.get(count).getPath());
                bis = new BufferedInputStream(fis);
                player = new Player(bis);
                player.play();
            } catch (JavaLayerException | FileNotFoundException e) {
                e.printStackTrace();
            }
            count++;
        }
        try
        {
            fis.close();
            bis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        player.close();
    }

    @Override
    public void init()
    {
        try 
        {
            requestSocket = new Socket(serverIP, Integer.parseInt(port));//open a connection with a broker(server)
            System.out.println("Server which client try to connect");//(debug)
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
        boolean flag = false;
        try 
        {
            out.writeObject("ConsumerNode");//Broker should know the type of client
            System.out.println("Client type sent.");
            out.writeObject(attributes);//send a list of attributes(consumer name - password - first time connect)

            if(this.connectForFirstTime.equals("true"))
            {  
                flag = connectForFirstTime(flag);
            }
            if (!flag)//send artist and song to server and revieve user's song
            {
                new Thread()
                {
                    public void run()
                    {
                        playData();
                    }
                }.start();
                out.writeObject("i want to register");
                out.writeObject(new ArtistName(this.artistName));
                out.writeObject(this.songName);
                writeData();//receive and write the song
                unregister();
            }
        } 
        catch(IOException | ClassNotFoundException e) 
        {  
            e.printStackTrace();
        }
    }

    public boolean connectForFirstTime(boolean flag) throws ClassNotFoundException, IOException
    {
        answer = (Info)in.readObject();//Info object is a message which contains broker List with broker's attributes
        brokersInfo.addAll(answer.getBrokers());
        System.out.println(brokersInfo);//(debug)
        System.out.println(answer.getArtistToBroker());//(debug)  
        this.connectForFirstTime = "false";
        attributes.set(2, "false");

        String tempBrokerId = null;
        while(tempBrokerId == null)//ask user for an existing artist
        {
            tempBrokerId = answer.getArtistToBroker().get(getArtistNameFromUser());
            System.out.println("Server which is responsible for this Artits, has ID: " + tempBrokerId);//(debug)
        }
        getSongNameFromUser();//ask user for a song
        for(ArrayList<String> element : brokersInfo)//ckeck if the random server is right if not disconnect and connect to right server
        {
            if(element.get(0).equals(tempBrokerId))
            {
                if(!(element.get(1).equals(this.port) && element.get(2).equals(this.serverIP)))//ckeck if the random broker at start is the correct broker
                {
                    out.writeObject("i do not want to register");
                    out.flush();
                    disconnect();
                    setPort(element.get(1));
                    setServerIP(element.get(2));
                    register();
                    flag = true;
                    return flag;
                }
            }
        }
        return false;
    }

    public void writeData() throws ClassNotFoundException, IOException
    {
        int size = (int) in.readObject();
        StringBuilder path = new StringBuilder();
        String prefix = ".";
        File someFile;
        FileOutputStream fos = null;
        FileOutputStream streamID = null;
        
        path.append(System.getProperty("user.dir") + "/download/" + this.songName + "-part0.mp3");
        int count = 0;
        while(count != size)
        {
            int k = path.indexOf(prefix);
            if (k != -1) {
                path.delete(k-1, k + path.length());
            }
            path.append(count);
            path.append(".mp3");
            System.out.println("Waiting to receive song");
            System.out.println(path);
            Value temp = (Value)in.readObject();
            System.out.println(temp);
            
            someFile = new File(path.toString());
            fos = new FileOutputStream(someFile);
            fos.write(temp.getMusicFile().getMusicFileExtract());
            fos.flush();
            streaming.add(someFile);
            
            streamID = new FileOutputStream(System.getProperty("user.dir") + "/download/" + this.songName + ".mp3",true);
            streamID.write(temp.getMusicFile().getMusicFileExtract());
            
            count++;
        }
        streamID.flush();
        streamID.close();

        out.flush();
        fos.close();
    }

    @Override
    public void disconnect() 
    {
        try 
        {
            in.close(); 
            out.close();
            requestSocket.close();
            System.out.println("Client end first connection with server...");
        } 
        catch(IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    private String getArtistNameFromUser()
    {
        System.out.println("Give an existing artist");
        this.artistName = System.console().readLine();
        return this.artistName;
    }

    private String getSongNameFromUser()
    {
        System.out.println("Give a song");
        this.songName =  System.console().readLine();
        return this.songName;
    }
}
