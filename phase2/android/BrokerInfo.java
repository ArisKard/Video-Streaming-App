package com.example.project_ds_2021;

import java.io.Serializable;

public class BrokerInfo implements Serializable { //Object που κρατά τα απαραίτητα στοιχεία ενός Broker, ώστε να διαμοιράζονται
    private String ip;
    private int port;
    private String brokerName;
    static final long serialVersionUID = 42L;

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
