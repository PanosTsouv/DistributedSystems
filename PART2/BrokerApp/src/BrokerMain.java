import java.io.IOException;
import java.net.UnknownHostException;

//@param args[0] : server port
//@param args[1] : brokerID
//@param args[2] : totalNumberOfBrokers
public class BrokerMain
{
    public static void main(String[] args) 
    {
        BrokerNode broker = new BrokerNode(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4], args[5]);
        new Thread()
        {
            public void run()
            {
                broker.openServer();
            }
        }.start();
        for(int i = 0; i < broker.getNumbersOfBroker()-1; i++)
        {
            while(true)
            {
                try
                {
                    broker.setConnectionServerIP();
                    broker.setConnectionPort();
                    broker.init();
                    break;
                }
                catch(UnknownHostException unknownHost)
                {
                    System.err.println("You are trying to connect to an unknown host!");
                    System.out.println("Give us again connection settings");
                }
                catch(IOException ioException)
                {
                    System.err.println("You are trying to connect to an offline server or connection settings are wrong.Check the server IP and port");
                    System.out.println("Give us again connection settings");
                }
                catch(NumberFormatException numberFormatException)
                {
                    System.err.println("Port should be number");
                    System.out.println("Give us again connection settings");
                }
            }
            broker.connectWithBrokers();
        }
    }
}