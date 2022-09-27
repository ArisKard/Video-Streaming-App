package com.example.project_ds_2021;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public interface Node {

    public List<BrokerInfo> brokers = new ArrayList<>();

    public void init(int i);

    public List<BrokerInfo> getBrokers();

    public void connect();

    public void disconnect();


}
