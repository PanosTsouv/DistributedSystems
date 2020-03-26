import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PublisherHandlerThread extends Thread {
    ObjectOutputStream out;
    ObjectInputStream in;
    private ArrayList<String> publisherInfo;
    private BrokerNode broker;

    public PublisherHandlerThread(ObjectOutputStream out, ObjectInputStream in, ArrayList<String> publisherInfo, BrokerNode broker) {
        this.out = out;
        this.in = in;
        this.publisherInfo = publisherInfo;
        this.broker = broker;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        if(publisherInfo.get(3).equals("true")) 
        {
            try
            {
                out.writeObject(broker.getBrokersInfo());
                out.flush();
                System.out.println("Publisher is connecting for the first time. Sending a list with brokers!");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    in.close();
                    out.close();
                }
                catch(IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }
        } 
        else 
        {
            try
             {
                HashMap<String, String> temp = (HashMap<String, String>) in.readObject();

                for(Map.Entry<String, String> entry : temp.entrySet()) 
                {
                    broker.getArtistToBrokers().put(entry.getKey(), entry.getValue());
                    if (entry.getValue().equals(broker.getBrokerID()))
                    {
                        broker.getArtistToPublisher().put(entry.getKey(), publisherInfo.get(0));
                    }
                }
                System.out.println(broker.getArtistToBrokers());
                System.out.println(broker.getArtistToPublisher());
            } 
            catch (IOException | ClassNotFoundException e) 
            {
                e.printStackTrace();
            }
        }
    }
}