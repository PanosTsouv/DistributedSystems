import java.io.*;
import java.util.ArrayList;

//@out@in Every thread has his own streams to accept and send messages
//@consumer Info Message from consumer(user) with important info
//@broker Broker object(server) to call broker's methods

public class ConsumerHandlerThread extends Thread{
    
    ObjectOutputStream out;
    ObjectInputStream in;
    private ArrayList<String> consumerInfo;
    private BrokerNode broker;
    String answer;
    ArtistName userArtistName;
    String userSongName;

    //Constructor initialize thread's attributes
    public ConsumerHandlerThread(ObjectOutputStream out, ObjectInputStream in, ArrayList<String> consumerInfo, BrokerNode broker) 
    {
            this.out = out;
            this.in = in;
            this.consumerInfo = consumerInfo;
            this.broker = broker;
    }

    public void run()
    {
        Info dataForUser;//this indo object contain the info from publichers which we sent to consumers
        try 
        {
            if(consumerInfo.get(2).equals("true"))//ckeck if consumer connect for first time
            {
                dataForUser = new Info(broker.getBrokersInfo(), broker.getArtistToBrokers());
                out.writeObject(dataForUser);//send Info object to consumers
                System.out.println("Server part of broker :: Consumer is connecting for the first time. Sending a list with brokers!");
                System.out.println("Server part of broker :: Server is waiting user's answer if he want to register");
                handleRequest();
            }  
            else//if not he will register because he know what he want from Info that we send
            {
                handleRequest();
            }
        }
        catch(IOException  e)
        {
            e.printStackTrace();
        }
    }

    //if user wnat register,save him,take ArtistName and songName and call broker method pull
    public void handleRequest()
    {
        try
        {
            answer = (String)in.readObject();
            if(answer.equals("i want to register"))
            {
                broker.getRegisteredUsers().add(this.consumerInfo);
                userArtistName = (ArtistName) in.readObject();
                userSongName = (String)in.readObject();
                System.out.println("Pull " + userArtistName + " " + userSongName);
                broker.pull(userArtistName, userSongName);
            
                answer = (String)in.readObject();
                if(answer.equals("I want to unregister"))
                {
                    broker.getRegisteredUsers().remove(this.consumerInfo);
                }
                out.writeObject("You can disconnect");
            }
        }
        catch(IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}