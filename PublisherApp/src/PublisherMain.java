public class PublisherMain{
    public static void main(String[] args)
    {
        PublisherNode PB = new PublisherNode("1", "4321", "4501", "192.168.247.113");

        new Thread(){
            public void run()
            {
                PB.openServer();       
            }
        }.start(); 

        PB.init();
        PB.getBrokerList();
        PB.calculateBrokerArtistMap();
        PB.sendInitialDataToBrokers();
    }
}