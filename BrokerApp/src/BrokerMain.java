public class BrokerMain
{
    public static void main(String[] args) 
    {
        BrokerNode broker = new BrokerNode("4321", "1", 2);
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