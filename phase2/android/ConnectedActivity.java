package com.example.project_ds_2021;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.example.project_ds_2021.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ConnectedActivity extends AppCompatActivity {
    private ArrayList<String> brokerHashCodes = new ArrayList<>(); //η λίστα με τα hashes των Brokers
    private static HashMap<String, BrokerInfo> brokerHashCodesMap = new HashMap<>(); //map με key ένα hash και value τον αντίστοιχο Broker

    private String subscribedTo; //το topic στο οποίο έχει γίνει το subscription
    private boolean ifAlreadySubscribed = false; //το αν έχει γίνει ήδη subscription ή όχι
    private HashMap<String, ArrayList<String>> videosOfThisTopic = new HashMap<>(); //map με key ένα topic και value μία λίστα με τα video που έχω λάβει για αυτό το topic

    private static ChannelName channelName; //το κανάλι
    private String filepath; //το directory
    private BrokerInfo brokerResponsible; //ο Broker που είναι υπέυθυνος γι'αυτό το AppNode
    private AppNodeInfo appNodeInfo; //oι πληροφορίες για το AppNode

    private int port;
    private boolean running = true; //το αν τρέχει ή αν έχει γίνει disconnect απ'το χρήστη
    private boolean publisherMode = true; //το αν είναι σε publisher mode ή σε consumer mode

    private ServerSocket serverSocket;
    private Socket connection, consumerConnection, serverConnection; //η χρήση του κάθε Socket εξηγείται παρακάτω
    private static ObjectOutputStream out, consumerOut, serverOut;
    private static ObjectInputStream in, consumerIn, serverIn;

    public List<BrokerInfo> brokers = new ArrayList<>();
    String initIp;
    int initPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.initIp = getIntent().getStringExtra("IPinput").toString();
        this.initPort = Integer.parseInt(getIntent().getStringExtra("PortInput"));
        channelName =  new ChannelName(getIntent().getStringExtra("ChannelInput").toString());

        port = 2500; //παίρνει τυχαία ένα port ανάλογα με το input (εδώ 2500 ή 3500)

        Intent mIntent = new Intent(this, Server.class);
        startService(mIntent);

        new firstConnection(initIp, initPort).execute();

    }

    @Override
    protected void onDestroy() {
        new disconnect().execute();
        super.onDestroy();
    }

    public static ObjectOutputStream getOut(){
        return out;
    }

    public static ObjectInputStream getIn(){
        return in;
    }

    private class firstConnection extends AsyncTask<String, Void, Boolean> {
        String ip;
        int port;

        private firstConnection(String ip, int port){
            super();
            this.ip = ip;
            this.port = port;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground (String... args){
            Socket requestSocket = null;
            ObjectOutputStream objectOutputStream = null;
            ObjectInputStream objectInputStream = null;

            try {
                requestSocket = new Socket(ip, port);

                objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
                objectInputStream = new ObjectInputStream(requestSocket.getInputStream());


                objectOutputStream.writeInt(4);
                objectOutputStream.flush();

                brokers = (ArrayList<BrokerInfo>)objectInputStream.readObject();


                hashTheBrokers(); //παίρνει τα στοιχεία των Brokers ώστε να τα hashάρει
                brokerResponsible = hashTopic(channelName.getChannelName()); //βρίσκει τον υπέυθυνο Broker, κάνοντας hash στο channel name, ώστε να κάνει connection κατ'ευθείαν σε αυτόν

                WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                int ip = wifiInfo.getIpAddress();
                String ipAddress = Formatter.formatIpAddress(ip);

                appNodeInfo = new AppNodeInfo(getChannelName().getChannelName(), brokerResponsible, port, ipAddress); //φτιάχνει ένα αντικείμενο AppNodeInfo που κρατά τις πληροφορίες για τον τρέχοντα AppNode
            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
                return false;
            } catch (IOException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
                return false;
            } finally {
                try{
                    objectOutputStream.close();
                    objectInputStream.close();
                    requestSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){
            new connect().execute();
        }
    }

    private void hashTheBrokers(){
        for(BrokerInfo brokerInfo: brokers){
            brokerHashCodes.add(hash(brokerInfo.getIp() + brokerInfo.getPort()));
            brokerHashCodesMap.put(hash(brokerInfo.getIp() + brokerInfo.getPort()), brokerInfo);

        }

        Collections.sort(brokerHashCodes); //τα κάνει sort
    }

    private BrokerInfo hashTopic(String topic){
        for(int i=0; i<brokerHashCodes.size(); i++){

            if(hash(topic).compareTo(brokerHashCodes.get(i)) < 0){ //κάνει το compare, κι αν το hash του channel name είναι μικρότερο απ'το hash του Broker, επιστρέφει τον υπεύθυνο Broker
                return brokerHashCodesMap.get(brokerHashCodes.get(i));
            }

        }
        return brokerHashCodesMap.get(brokerHashCodes.get(0)); //αλλιώς επιστρέφει τον Broker με το μικρότερο hash
    }

    private String hash(String input){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(10);


            return hashText;
        } catch(NoSuchAlgorithmException e){
            throw new RuntimeException();
        }
    }

    private class connect extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground (String... args){
            try {
                connection = new Socket(brokerResponsible.getIp(), brokerResponsible.getPort()); //κάνει τη σύνδεση στον κατάλληλο Broker
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());

                out.writeInt(0); //στέλνει flag=0 για να του πει ότι δέχεται σύνδεση απο AppNode
                out.flush();

                out.writeObject(appNodeInfo); //στέλνει τα στοιχεία του στον Broker
                out.flush();
            } catch (IOException exc) {
                exc.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){
            setContentView(R.layout.activity_connected);
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getApplication(), getSupportFragmentManager());
            ViewPager viewPager = findViewById(R.id.view_pager);
            viewPager.setAdapter(sectionsPagerAdapter);
            TabLayout tabs = findViewById(R.id.tabs);
            tabs.setupWithViewPager(viewPager);
            LinearLayout tabStrip = ((LinearLayout)tabs.getChildAt(0));
            for(int i = 0; i < tabStrip.getChildCount(); i++) {
                tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                });
            }
        }
    }

    private class disconnect extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground (String... args){
            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(3));
                out.flush();

                in.close();
                out.close();
                connection.close();
            } catch (IOException | ClassNotFoundException exc) {
                exc.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){

        }
    }

    public static ChannelName getChannelName(){
        return channelName;
    }

    public int getPort() { return port; }

    public ArrayList<String> getBrokerHashCodes() { return brokerHashCodes; }

    public HashMap<String, BrokerInfo> getBrokerHashCodesMap() { return brokerHashCodesMap; }






}