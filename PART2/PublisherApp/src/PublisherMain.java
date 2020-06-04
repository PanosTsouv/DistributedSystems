public class PublisherMain{
    //@param args[0] : publisherID
    //@param args[1] : port of server part of publisher
    //@param args[2] : port of server that client part of publisher connect
    //@param args[3] : serverIP of server that client part of publisher connect
    //@param args[4] : start of range of first letter of artist
    //@param args[5] : end of range of first letter of artist
    public static void main(String[] args)
    {
        String path = System.getProperty("user.dir") + "/dataset1";
        PublisherNode PB = new PublisherNode(args[0], args[1], args[2], args[3], args[4], args[5], path, args[6].charAt(0), args[7].charAt(0), args[8]);

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