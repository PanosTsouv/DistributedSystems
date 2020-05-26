import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import Utils.ArtistName;
import Utils.Info;
import Utils.Value;

public class ActionsForClients extends Thread {

    ObjectInputStream in;
    ObjectOutputStream out;
    Socket connection;
    BrokerNode brokerClient;
    BrokerNode brokerServer;
    ArtistName userArtistName;
    String userSongName;

    //constructor - initialize connection attributes
    //the broker object (access server part of broker's attributes)
	public ActionsForClients(Socket connection, BrokerNode brokerServer){
        try 
        {
            this.connection = connection;
            this.brokerServer = brokerServer;
            this.brokerClient = new BrokerNode();
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } 
        catch(IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    //receive client type and his attributes
    //handle connection
    @SuppressWarnings("unchecked")
    public void run() 
    {
        try
        {
            try
            {
                String type = (String) in.readObject();
                ArrayList<String> info = (ArrayList<String>) in.readObject();
                if(type.equals("ConsumerNode")) 
                {
                    System.out.println("Server part of broker :: Consumer detected");
                    acceptConnectionConsumer(info);
                } 
                else if(type.equals("PublisherNode")) 
                {
                    System.out.println("Server part of broker :: Publisher detected");
                    acceptConnectionPublisher(info);
                }
                else 
                {
                    this.brokerServer.getBrokersInfo().add(info);
                    System.out.println("Server part of broker :: Receive another broker and add it to the list");
                }
            } 
            catch(ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                System.out.println("Server part of broker :: A request is done!!!Close thread.");
                System.out.println();
            }
            catch(IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }

    /////////////////////////////////////////////////
    ///////////////                   //////////////
    ////////////// PUBLISHER HANDLER //////////////
    /////////////                   //////////////
    /////////////////////////////////////////////

    //Handle publisher connection
    private void acceptConnectionPublisher(ArrayList<String> publisherInfo)
    {
        try
        {
            if(publisherInfo.get(3).equals("true")) 
            {
                publisherFirstTimeConnection();//implementation below
            }
            else
            {
                publisherConnection(publisherInfo);//implementation below
            }
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    //if publisher connect for first time,we just send him BrokerInfo(Server)
    private void publisherFirstTimeConnection() throws IOException
    {
        out.writeObject(brokerServer.getBrokersInfo());
        out.flush();
        System.out.println("Server part of broker :: Publisher is connecting for the first time. Sending a list with brokers!");
    }


    //receive a hashmap artistID as key and BrokerID as value from publisher
    //initialize broker hashmap artistID as key and BrokerID as value
    //add broker to register list
    @SuppressWarnings("unchecked")
    private void publisherConnection(ArrayList<String> publisherInfo) throws IOException, ClassNotFoundException
    {
        HashMap<String, String> temp = (HashMap<String, String>) in.readObject();//artistID as key and BrokerID as value

        boolean flagArtistExist = false;

        for(Map.Entry<String, String> entry : temp.entrySet()) 
        {
            for(Map.Entry<ArtistName, String> entry1 : brokerServer.getArtistToBrokers().entrySet()) 
            {
                if(entry1.getKey().getArtistName().equals(entry.getKey()))
                {
                    flagArtistExist = true;
                    break;
                }
            }
            if(!flagArtistExist)
            {
                brokerServer.getArtistToBrokers().put(new ArtistName(entry.getKey()), entry.getValue());
            }
            if (entry.getValue().equals(brokerServer.getBrokerID()))//if this specific artist has this broker id 
            {
                brokerServer.getArtistToPublisher().put(entry.getKey(), publisherInfo.get(0));
                brokerServer.getRegisteredPublishers().put(publisherInfo.get(0), publisherInfo);
            }
        }
        System.out.println("Server part of broker :: List with registered Publishers");
        System.out.println(brokerServer.getRegisteredPublishers());//debug
        System.out.println("Server part of broker :: HashMap with Artist as key and Broker as value");
        System.out.println(brokerServer.getArtistToBrokers());//debug
        System.out.println("Server part of broker :: HashMap with Artist as key and Publisher as value");
        System.out.println(brokerServer.getArtistToPublisher());//debug
    }

    /////////////////////////////////////////////////
    ///////////////                   //////////////
    ////////////// CONSUMER HANDLER  //////////////
    /////////////                   //////////////
    /////////////////////////////////////////////


    //IDEA for consumer handler
    //if consumer connect for first time we send him a info object(broker list -- hashmap with Artist to Broker)
    //wait for consumer to register(clients with info object know which broker is right for his request) - 
    // if consumer doesn't register,disconnect and client connect to right broker
    //Client give us an artist and broker return him a list with song of this artist
    //Client chooses a song and makes a request
    //Broker checks if client's login is valid if not he gives him 3 attemps to login again
    //broker pull request from cache without make request to publisher if cache has the song else forward the request
    @SuppressWarnings("unchecked")
    private void acceptConnectionConsumer(ArrayList<String> consumerInfo) throws IOException, ClassNotFoundException
    {
        if (consumerInfo.get(2).equals("true"))// ckeck if consumer connect for first time
        {
            System.out.println("Server part of broker :: A Info object sent");
            Info dataForUser = new Info(brokerServer.getBrokersInfo(), brokerServer.getArtistToBrokers());
            out.writeObject(dataForUser);
            out.flush();
        }
        String answer = (String) in.readObject();
        if(!answer.equals("register"))
        {
            return;
        }
        brokerServer.getRegisteredUsers().put(consumerInfo.get(3), consumerInfo);
        System.out.println("Server part of broker :: Print the registered users:");
        System.out.println(brokerServer.getRegisteredUsers());
        String artist = (String) in.readObject();
        userArtistName = new ArtistName(artist);
        initializeStreamsFromBrokersToSpecificPublisher();
        brokerClient.getOutAsClient().writeObject("List");
        brokerClient.getOutAsClient().writeObject(userArtistName);
        brokerClient.getOutAsClient().flush();
        HashSet<String> temp =(HashSet<String>)brokerClient.getInAsClient().readObject();
        System.out.println("Server part of broker :: Send a list with song of " + artist + " to " + consumerInfo.get(0));
        brokerClient.disconnect();

        out.writeObject(temp);
        out.flush();
        userSongName = (String) in.readObject();
        String answerForUnregister = "";
        int count = 0;
        // if(!temp.contains(userSongName))
        // {
        //     out.writeObject("You can disconnect");
        //     return;
        // }
        // if(!ckeckUserLogin(consumerInfo))
        // {
        //     out.writeObject("You can disconnect");
        //     return;
        // };
        while(!answerForUnregister.equals("I want to unregister"))
        {
            if(count>0) userSongName = answerForUnregister;
            if(brokerServer.getSongsInCache().get(userSongName) != null)
            {
                brokerServer.pull(userArtistName, userSongName, out);
            }
            else
            {
                initializeStreamsFromBrokersToSpecificPublisher();
                brokerClient.getOutAsClient().writeObject("Request");

                brokerClient.getOutAsClient().writeObject(userArtistName);
                brokerClient.getOutAsClient().writeObject(userSongName);
                brokerClient.getOutAsClient().flush();
                System.out.println("Client part of broker :: Forward request to publisher");
                Value tempval;
                int chunksSize = (int) brokerClient.getInAsClient().readObject();
                Value[] tempList = new Value[chunksSize];
                brokerServer.getSongsInCache().put(userSongName, tempList);
                System.out.println("Server part of broker :: Start sending " + chunksSize + " chunks to consumers");
                new Thread() {
                    public void run() {
                        brokerServer.pull(userArtistName, userSongName, out);
                    }
                }.start();
                for (int i = 0; i < chunksSize; i++) {
                    tempval = (Value) brokerClient.getInAsClient().readObject();
                    brokerServer.getSongsInCache().get(userSongName)[i] = tempval;
                }
                brokerClient.disconnect();
            }
        
            answerForUnregister = (String) in.readObject();
            count++;
        }
        brokerServer.getRegisteredUsers().remove(consumerInfo.get(3));
        
        out.writeObject("You can disconnect");
        out.flush();
    }

    //open a connection with a specific publisher
    public void initializeStreamsFromBrokersToSpecificPublisher() 
    {
        String tempPublisherID = brokerServer.getArtistToPublisher().get(userArtistName.getArtistName());
        brokerServer.getRegisteredPublishers().get(tempPublisherID);
        brokerClient.setServerIP(brokerServer.getRegisteredPublishers().get(tempPublisherID).get(1));
        brokerClient.setPort(brokerServer.getRegisteredPublishers().get(tempPublisherID).get(2));
        try 
        {
            brokerClient.init();
        }
        catch (UnknownHostException unknownHost)
        {
            System.err.println("You are trying to connect to an unknown host!");
        } 
        catch (IOException | NumberFormatException ioException)
        {
            System.err.println(ioException);
        }
    }

    ///check login info
    public boolean ckeckUserLogin(ArrayList<String> consumerInfo) {
        int count = 3;
        while(count != -1)
        {
            try {
                String tempUserEmail = (String) in.readObject();
                String tempUserPassword = (String) in.readObject();
                if (brokerServer.getRegisteredUsers().containsKey(tempUserEmail) && consumerInfo.get(3).equals(tempUserEmail) )
                {
                    if(brokerServer.getRegisteredUsers().get(tempUserEmail).get(1).equals(tempUserPassword))
                    {
                        out.writeObject("Valid login");
                        return true;
                    }
                }
                out.writeObject("Invalid login");
                count--;
                if(count != -1)
                {
                    out.writeObject(count);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                break;
            }
        }
        return false;
    }
}