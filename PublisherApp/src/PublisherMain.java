public class PublisherMain{
    public static void main(String[] args)
    {
        String path = System.getProperty("user.dir") + "/dataset1";
        PublisherNode PB = new PublisherNode(args[0], args[1], args[2], args[3], path, args[4].charAt(0), args[5].charAt(0));

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