package com.example.project_ds_2021;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import com.example.project_ds_2021.BrokerInfo;

public class AppNodeInfo implements Serializable { //Object που κρατά τα απαραίτητα στοιχεία ενός AppNode, ώστε να διαμοιράζονται
    static final long serialVersionUID = 41L;

    private String ip;
    private int port;
    private BrokerInfo brokerResponsible;
    private String channelName;

    public AppNodeInfo(String channelName, BrokerInfo brokerResponsible, int port, String ip) {
        this.channelName = channelName;
        this.port = port;
        this.brokerResponsible = brokerResponsible;
        this.ip = ip;
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
