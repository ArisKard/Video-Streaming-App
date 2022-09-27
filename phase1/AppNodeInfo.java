import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class AppNodeInfo implements Serializable { //Object που κρατά τα απαραίτητα στοιχεία ενός AppNode, ώστε να διαμοιράζονται

    private String ip;
    private int port;
    private BrokerInfo brokerResponsible;
    private String channelName;

    public AppNodeInfo(String channelName, BrokerInfo brokerResponsible, int port) {

        this.channelName = channelName;
        this.port = port;
        this.brokerResponsible = brokerResponsible;

        try{
            this.ip = InetAddress.getLocalHost().toString();
        } catch(UnknownHostException u){
            u.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public String getChannelName(){
        return channelName;
    }

    public BrokerInfo getBrokerResponsible(){
        return brokerResponsible;
    }

    public String getIp(){
        return ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelName);
    }

    @Override
    public boolean equals (Object app) {
        boolean result;

        if (!((AppNodeInfo) app).getChannelName().equals(this.getChannelName())) { //η σύγκριση 2 AppNodes γίνεται βάση του channel name τους
            result = false;
        } else {
            result = true;
        }
        return result;
    }
}
