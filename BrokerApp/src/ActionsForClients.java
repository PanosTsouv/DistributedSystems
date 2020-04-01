import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ActionsForClients extends Thread
{

    ObjectInputStream in;
    ObjectOutputStream out;
    BrokerNode brokerClient;
    BrokerNode brokerServer;
    ArtistName userArtistName;
    String userSongName;

	public ActionsForClients(Socket connection, BrokerNode brokerServer){
        try 
        {
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
                in.close();
                out.close();
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


    @SuppressWarnings("unchecked")
    private void publisherConnection(ArrayList<String> publisherInfo) throws IOException, ClassNotFoundException
    {
        HashMap<String, String> temp = (HashMap<String, String>) in.readObject();//artistID as key and BrokerID as value

        for(Map.Entry<String, String> entry : temp.entrySet()) 
        {
            brokerServer.getArtistToBrokers().put(entry.getKey(), entry.getValue());
            if (entry.getValue().equals(brokerServer.getBrokerID()))//if this specific artist has this broker id 
            {
                brokerServer.getArtistToPublisher().put(entry.getKey(), publisherInfo.get(0));
                brokerServer.getRegisteredPublishers().put(publisherInfo.get(0), publisherInfo);
            }
        }
        System.out.println("Server part of broker :: List with registered Publishers");
        System.out.println(brokerServer.getRegisteredPublishers());
        System.out.println("Server part of broker :: HashMap with Artist as key and Broker as value");
        System.out.println(brokerServer.getArtistToBrokers());
        System.out.println("Server part of broker :: HashMap with Artist as key and Publisher as value");
        System.out.println(brokerServer.getArtistToPublisher());
    }

    /////////////////////////////////////////////////
    ///////////////                   //////////////
    ////////////// CONSUMER HANDLER  //////////////
    /////////////                   //////////////
    /////////////////////////////////////////////

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
        String artist = (String) in.readObject();
        System.out.println(artist);

        userArtistName = new ArtistName(artist);
        initializeStreamsFromBrokersToSpecificPublisher();
        brokerClient.getOutAsClient().writeObject("List");
        brokerClient.getOutAsClient().writeObject(userArtistName);
        brokerClient.getOutAsClient().flush();
        ArrayList<String> temp =(ArrayList<String>)brokerClient.getInAsClient().readObject();
        brokerClient.disconnect();
        System.out.println(temp);

        out.writeObject(temp);
        out.flush();
        userSongName = (String) in.readObject();
        //doulevei
        initializeStreamsFromBrokersToSpecificPublisher();
        brokerClient.getOutAsClient().writeObject("Request");
        
        brokerClient.getOutAsClient().writeObject(userArtistName);
        brokerClient.getOutAsClient().writeObject(userSongName);
        brokerClient.getOutAsClient().flush();
        System.out.println("Server part of broker :: Forward request to publisher");
        Value tempval;
        int chunksSize = (int) brokerClient.getInAsClient().readObject();
        Value[] tempList = new Value[chunksSize];
        System.out.println(chunksSize);
        brokerServer.getSongsInCache().put(userSongName, tempList);
        System.out.println("Server part of broker :: Start sending chunks to consumers");
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
        String answerForUnregister = (String) in.readObject();
        if (answerForUnregister.equals("I want to unregister")) {
            brokerServer.getRegisteredUsers().remove(consumerInfo);
        }
        out.writeObject("You can disconnect");
        out.flush();
    }

    public void initializeStreamsFromBrokersToSpecificPublisher() 
    {
        String tempPublisherID = brokerServer.getArtistToPublisher().get(userArtistName.getArtistName());
        brokerServer.getRegisteredPublishers().get(tempPublisherID);
        brokerClient.setServerIP(brokerServer.getRegisteredPublishers().get(tempPublisherID).get(1));
        brokerClient.setPort(brokerServer.getRegisteredPublishers().get(tempPublisherID).get(2));
        brokerClient.init();
    }
}