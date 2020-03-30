public class PublisherMain{
    public static void main(String[] args)
    {
        String path = System.getProperty("user.dir") + "/dataset1";
        PublisherNode PB = new PublisherNode("1", "4321", "4501", "192.168.247.113", path, 'A', 'J');

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