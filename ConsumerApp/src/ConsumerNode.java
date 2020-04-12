import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

//@param serverIP : is the IP of server which the consumer connect
//@param port : is the port of server which the consumer connect
//@param connectForFirstTime : true(user dont have info for brokers)
//@param attributes : contain (userName and Password)....server can take these attributes and save a user to a register list
//@paramlistOfSong : contain all song of a artist which user request
//@param requestSocket : is the connection between server-client....client open a socket to connect with server
//@param out,in : client use these streams to communicate with server

//IDEA:
// (1)   Consumer make a random server connect ->
// (2)-> Server send back Info object(message) which contains a list with all brokers(servers) and their attributes(serverID-serverIP-port) --- 
//       a hashMap with all artistNames as KEYS and brokerID(server) as values ->
// (3)-> we put from this hashMap an artistName which consumer(user) choose and we take the brokerID(serverID) which is responsible for this artistName ->
// (4)-> if this random server contain user's artistName we register this user to this broker(server) and make the request
//       else we should disconnect from this broker(server) ->
// (5)-> find the right broker(server) from list of brokers(so we have his serverIP and Port) ->
// (6)-> register to right broker(server) and make the request
public class ConsumerNode implements Consumer {

    private String consumerName;
    private String consumerPassword;
    private String consumerEmail;
    private String serverIP;
    private String port;
    private String connectForFirstTime;
    private String artistName;
    private String songName;
    private Info answer = null;
    private String loginEmail;
    private String loginPassword;

    private ArrayList<String> attributes = new ArrayList<>();
    private ArrayList<File> streaming = new ArrayList<>();
    private HashSet<String> listOfSong = new HashSet<>();

    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    // constructor + initialize attributes list which we send to server ----- server
    // has access to this list
    // IMPORTANT -- if you want server know a attribute of consumer you should add
    // it to the attributes list
    public ConsumerNode(String consumerEmail, String consumerName, String consumerPassword, String serverIP,
            String port) {
        this.consumerName = consumerName;
        this.consumerPassword = consumerPassword;
        this.consumerEmail = consumerEmail;
        this.serverIP = serverIP;
        this.port = port;
        this.connectForFirstTime = "true";

        attributes.add(this.consumerName);
        attributes.add(this.consumerPassword);
        attributes.add(this.connectForFirstTime);
        attributes.add(this.consumerEmail);
    }

    // ask user for an existing song
    public void songRequest() {
        try {
            boolean songExist = false;
            while (!songExist) {
                songName = getSongNameFromUser();
                songExist = listOfSong.contains(songName);
            }
            out.writeObject(songName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // play and write the parts of song that user ask
    public void userSelectLive() {
        try {
            Thread thread = new Thread() {
                public void run() {
                    playData();
                }
            };
            thread.start();

            writeData();// receive and write the song

            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // receive and write the song
    public void userSelectOffline() {
        try {
            writeData();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // we use register method when the broker, which user connect for first time,is
    // wrong
    @Override
    public void register() {
        try {
            out.writeObject("register");
            out.writeObject(this.artistName);
            out.flush();
            System.out.println("The list of existing songs:");
            receiveTheListOfSong();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // user informs broker that he want to unregister so broker remove him
    // user closes his connection
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
        try {
            fis.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.close();
    }

    // open a connection with a broker(server)
    // initialize output-input stream
    @Override
    public void init() {
        try {
            requestSocket = new Socket(serverIP, Integer.parseInt(port));
            System.out.println(
                    "Server which Client try to connect has ServerIP: " + this.serverIP + " Port: " + this.port);// (debug)
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
            System.exit(1);
        } catch (IOException ioException) {
            System.err.println("You are trying to connect to an offline server.Check the server IP and port");
            System.exit(1);
        }
    }

    @Override
    public ArrayList<ArrayList<String>> getBrokersInfo() {
        return brokersInfo;
    }

    @SuppressWarnings("unchecked")
    private void receiveTheListOfSong() throws ClassNotFoundException, IOException {
        listOfSong = (HashSet<String>) in.readObject();
        System.out.println(listOfSong);
    }

    // ckeck if the random server is right if not disconnect and connect to right
    // server
    // ckeck if the random broker at start is the correct broker
    private boolean isTheRightBroker(String tempBrokerId) {
        for (ArrayList<String> element : brokersInfo) {
            if (element.get(0).equals(tempBrokerId)) {
                if (element.get(1).equals(this.port) && element.get(2).equals(this.serverIP)) {
                    return true;
                }
                setPort(element.get(1));
                setServerIP(element.get(2));
            }
        }
        return false;
    }

    public void findTheRightBroker() {
        String tempBrokerId = null;

        while (tempBrokerId == null)// ask user for an existing artist
        {
            tempBrokerId = answer.getArtistToBroker().get(getArtistNameFromUser());
        }
        if (isTheRightBroker(tempBrokerId)) {
            register();
        } else {
            try {
                out.writeObject("disconnect");
                out.flush();
                disconnect();
                init();
                connect();
                register();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Info object is a message which contains broker List with broker's attributes
    public void receiveInfoObject() {
        try {
            answer = (Info) in.readObject();
            brokersInfo.addAll(answer.getBrokers());
            System.out.println("The list with online brokers when consumer connect:");
            System.out.println(brokersInfo);// (debug)
            System.out.println("The list with all existing artist at system:");
            System.out.println(answer.getArtistToBroker());// (debug)
            this.connectForFirstTime = "false";
            attributes.set(2, "false");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // send user type(consumer) and his attributes
    @Override
    public void connect() {
        try {
            out.writeObject("ConsumerNode");
            out.writeObject(attributes);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login() {
        System.out.println("USER LOGIN");
        setEmailFromUser();
        setPasswordFromUser();
    }

    public boolean sendUserLoginInfo(){
        System.out.println();
        int count = 3;
        while(count != -1)
        {
            try {
                out.writeObject(this.loginEmail);
                out.writeObject(this.loginPassword);
                String valid =(String) in.readObject();
                if(valid.equals("Valid login"))
                {
                    return true;
                }
                else
                {
                    if(count == 0)
                    {
                        break;
                    }
                    System.out.println("Invalid login.Try to login again.");
                    System.out.println(count + " attempts remain");
                    setEmailFromUser();
                    setPasswordFromUser();
                    count =(int) in.readObject();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
        disconnect();
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
            if (k != -1)
            {
                path.delete(k-(Integer.toString(count).length()), k + path.length());
            }
            path.append(count);
            path.append(".mp3");
            Value temp = (Value)in.readObject();
            System.out.println("The part of song which Client receive save at " + path);
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

    //close a connection with server
    @Override
    public void disconnect() 
    {
        try 
        {
            in.close(); 
            out.close();
            requestSocket.close();
            System.out.println("Consumer end connection with server...");
        } 
        catch(IOException ioException) 
        {
            ioException.printStackTrace();
        }
    }

    public void setEmailFromUser()
    {
        System.out.println("Give your email address:");
        this.loginEmail = System.console().readLine();
    }

    public void setPasswordFromUser()
    {
        System.out.println("Give your password:");
        this.loginPassword = System.console().readLine();
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    //user gives an artist
    private String getArtistNameFromUser()
    {
        System.out.println("Give an existing artist from the list:");
        this.artistName = System.console().readLine();
        return this.artistName;
    }

    //user gives a song
    private String getSongNameFromUser()
    {
        System.out.println("Give an existing song from the list:");
        this.songName =  System.console().readLine();
        return this.songName;
    }

    //user gives his choice
    public String getUserChoice()
    {
        System.out.println("Press 0 for live streaming");
        System.out.println("Press anything else to download the song and disconnect");
        return System.console().readLine();
    }
}
