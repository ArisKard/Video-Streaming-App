package com.example.project_ds_2021;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server extends Service {
    private ServerSocket serverSocket;
    private Socket serverConnection;
    private ObjectOutputStream serverOut;
    private ObjectInputStream serverIn;

    ChannelName channelName;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        channelName = ConnectedActivity.getChannelName();

        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(ipAddress);
            serverSocket = new ServerSocket(16230, 50, addr); //περιμένω να λάβω αιτήματα για να κάνω push
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    try {
                        serverConnection = serverSocket.accept();

                        serverOut = new ObjectOutputStream(serverConnection.getOutputStream());
                        serverIn = new ObjectInputStream(serverConnection.getInputStream());

                        int flag = serverIn.readInt(); //flag για το αν πρέπει να στείλω πολλά, ήδη υπάρχοντα, ή ένα νέο
                        if (flag == 0) { //αν flag=1, κάνω push τουλάχιστον 1

                            Topic t = (Topic) serverIn.readObject(); //το topic που μου ζητείται
                            if (t.getType().equals("Channel")) { //αν είναι το κανάλι μου
                                serverOut.writeInt(channelName.getPublishedVideos().size()); //στέλνω το πλήθος των video που 'χω κάνει upload
                                serverOut.flush();

                                for (String vName : channelName.getPublishedVideos()) { //κάνω push κάθε video της λίστας
                                    push(vName);
                                }

                            } else if (t.getType().equals("Hashtag")) { //αν το topic είναι ένα hashtag
                                ArrayList<String> videosWithThisHashtag = channelName.getPublishedHashtagVideosMap().get(t.getName());
                                serverOut.writeInt(videosWithThisHashtag.size()); //στέλνω το πλήθος των video που 'χω κάνει upload με αυτό το hashtag
                                serverOut.flush();

                                for (String vName : videosWithThisHashtag) { //κάνω push κάθε video της λίστας
                                    push(vName);
                                }
                            }

                        } else if (flag == 1) { //αν flag=1, κάνω push μόνο ένα
                            String vName = (String) serverIn.readObject(); //τ'όνομα του video
                            push(vName);
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(serverConnection!=null){
            try {
                serverConnection.close(); //κλείνω τη σύνδεση
                if(serverOut != null) serverOut.close();
                if(serverIn != null) serverIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void push(String vName){
        try {
            ArrayList<VideoFile> videoChunks = generateChunks(vName); //τα generated chunks μπαίνουν σε μία λίστα από VideoFiles

            serverOut.writeInt(videoChunks.size()); //στείλε τον αριθμό των chunks
            serverOut.flush();

            serverOut.writeUTF(vName); //στείλε το όνομα του video
            serverOut.flush();

            for (VideoFile chunks : videoChunks) {
                serverOut.writeObject(chunks); //στείλε τα chunks ένα προς ένα
                serverOut.flush();
            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private ArrayList<VideoFile> generateChunks(String vName) throws IOException {

        int chunk = 512 * 1024; //μέγεθος του chunk se bytes

        FileInputStream inputVideo = new FileInputStream(Environment.getExternalStorageDirectory() + "/project_ds_2021/toProduce/" + vName); //directory για τα video προς δημοσίευση

        ArrayList<byte[]> chunkList = new ArrayList<>(); //λίστα των chunks

        int i = 0;
        while (inputVideo.available() != 0) {
            byte[] c = new byte[chunk];
            inputVideo.read(c, i*chunk ,chunk); //διάβαζει τόσα bytes και τα βάζει στη λίστα των chunks
            chunkList.add(c);
        }

        ArrayList<VideoFile> videoFileList = new ArrayList<>(); //λίστα των VideoFiles

        for (i = 0; i < chunkList.size(); i++) {
            // VideoFile videofile = new VideoFile((i + 1) + "_" + vName,dateCreated,length,null,frameWidth,frameHeight,chunkList.get(i)); //μετατρέπει τα chunks σε VideoFiles
            VideoFile videofile = new VideoFile((i + 1) + "_" + vName, chunkList.get(i));
            videoFileList.add(videofile); //αριθμώντας τα, ώστε όταν φτάσουν να γίνει sorting
        }

        inputVideo.close();
        return videoFileList;
    }
}
