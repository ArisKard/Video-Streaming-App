import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BrokerInfo implements Serializable { //Object που κρατά τα απαραίτητα στοιχεία ενός Broker, ώστε να διαμοιράζονται


    private String ip;
    private int port;
    private String brokerName;

    public BrokerInfo(String brokerName, String ip, int port) {

        this.port = port;
        this.brokerName = brokerName;
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public String getBrokerName(){
        return brokerName;
    }

    public String getIp(){
        return ip;
    }
}
