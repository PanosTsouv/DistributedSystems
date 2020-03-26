import java.io.*;
import java.util.ArrayList;

public class ConsumerHandlerThread extends Thread{
    
    ObjectOutputStream out;
    ObjectInputStream in;
    private ArrayList<String> consumerInfo;
    private BrokerNode broker;

    public ConsumerHandlerThread(ObjectOutputStream out, ObjectInputStream in, ArrayList<String> consumerInfo, BrokerNode broker) 
    {
            this.out = out;
            this.in = in;
            this.consumerInfo = consumerInfo;
            this.broker = broker;
    }

    public void run()
    {
        try 
        {
            Info dataForUser = new Info(broker.getBrokersInfo(), broker.getArtistToBrokers());
            out.writeObject(dataForUser);
            System.out.println("Consumer is connecting for the first time. Sending a list with brokers!");
            
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
            }
            catch(IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }
}