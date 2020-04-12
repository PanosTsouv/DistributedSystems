public class ConsumerMain {

    //@param args[0] : Consumer username
    //@param args[1] : Consumer password
    //@param args[2] : server IP
    //@param args[3] : server port
    //@param args[4] : Consumer email
    public static void main(String[] args) {
        ConsumerNode consumer = new ConsumerNode(args[0], args[1], args[2], args[3], args[4]);
        consumer.login();
        consumer.init();
        consumer.connect();
        consumer.receiveInfoObject();
        consumer.findTheRightBroker();
        consumer.songRequest();
        if(!consumer.sendUserLoginInfo())
        {
            System.out.println("Invalid login");
            System.exit(1);
        }
        if (consumer.getUserChoice().equals("0")) 
        {
            consumer.userSelectLive();
        }
        else 
        {
            consumer.userSelectOffline();
        }
        consumer.unregister();
    }
}