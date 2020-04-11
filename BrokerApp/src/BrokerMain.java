public class BrokerMain
{
    public static void main(String[] args) 
    {
        BrokerNode broker = new BrokerNode(args[0], args[1], Integer.parseInt(args[2]));
        new Thread()
        {
            public void run()
            {
                broker.openServer();
            }
        }.start();
        for(int i = 0; i < broker.getNumbersOfBroker()-1; i++)
        {
            broker.setConnectionServerIP();
            broker.setConnectionPort();
            broker.init();
            broker.connectWithBrokers();
        }
    }
}