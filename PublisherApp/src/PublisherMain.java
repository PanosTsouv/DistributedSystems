public class PublisherMain{
    public static void main(String[] args)
    {
        String path = System.getProperty("user.dir") + "/dataset1";
        PublisherNode PB = new PublisherNode("1", "4321", "4501", "192.168.1.3", path, 'A', 'J');

        new Thread(){
            public void run()
            {
                PB.openServer();       
            }
        }.start(); 

        PB.init();
        PB.getBrokerList();
        PB.calculateBrokerArtistMap();
        for(int i = 0; i <  PB.getBrokersInfo().size(); i++)
        {
            PB.setServerIP(PB.getBrokersInfo().get(i).get(2));
            PB.setPort(PB.getBrokersInfo().get(i).get(1));
            PB.init();
            PB.connect();
        }
    }
}