public class ConsumerMain {

    public static void main(String[] args) {
        ConsumerNode consumer = new ConsumerNode(args[0], args[1], args[2], args[3]);
        consumer.init();
        consumer.connect();
        consumer.receiveInfoObject();
        consumer.findTheRightBroker();
        consumer.songRequest();
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