package Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Info implements Serializable {
    
    private static final long serialVersionUID = -589233052403477203L;
    
    private ArrayList<ArrayList<String>> brokersInfo;
    private HashMap<ArtistName, String> artistToBroker;

    public Info(){}

    public Info(ArrayList<ArrayList<String>> brokersInfo, HashMap<ArtistName, String> artistToBroker)
    {
        this.brokersInfo = brokersInfo;
        this.artistToBroker = artistToBroker;
    }

    public ArrayList<ArrayList<String>> getBrokers()
    {
        return this.brokersInfo;
    }

    public HashMap<ArtistName, String> getArtistToBroker()
    {
        return this.artistToBroker;
    }
}