public class ConsumerMain{
    
    public static void main(String[] args) 
    {
        ConsumerNode consumer = new ConsumerNode("Panos", "123456789", "192.168.247.113", "4321");
        consumer.init();
        consumer.connect();
        consumer.receiveInfoObject();
        consumer.findTheRightBroker();
    }
}