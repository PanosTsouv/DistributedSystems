import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Info implements Serializable
{
    private static final long serialVersionUID = -589233052403477203L;
    private ArrayList<ArrayList<String>> brokersInfo;
    private HashMap<String,String> artistToBroker;

    public Info(ArrayList<ArrayList<String>> brokersInfo, HashMap<String,String> artistToBroker)
    {
        this.brokersInfo = brokersInfo;
        this.artistToBroker = artistToBroker;
    }

    public ArrayList<ArrayList<String>> getBrokers()
    {
        return this.brokersInfo;
    }

    public HashMap<String,String> getArtistToBroker()
    {
        return this.artistToBroker;
    }

}