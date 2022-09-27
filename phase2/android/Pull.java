package com.example.project_ds_2021;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.project_ds_2021.ui.main.PlaceholderFragment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Pull extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BrokerInfo b = PlaceholderFragment.getbPull();

                String vName = PlaceholderFragment.getVideoSelected();
                try {
                    Socket consumerConnection = new Socket(b.getIp(), b.getPort()+1); //νέα σύνδεση, ως consumer, στον Broker που
                    ObjectOutputStream consumerOut = new ObjectOutputStream(consumerConnection.getOutputStream()); //είναι υπέθυνος για το topic που έχουμε
                    ObjectInputStream consumerIn = new ObjectInputStream(consumerConnection.getInputStream()); //κάνει εγγραφή

                    consumerOut.writeInt(1); //στέλνω flag=1 για να ενημερώσω ότι θέλω να λάβω νέο video
                    consumerOut.flush();

                    consumerOut.writeObject(vName); //στέλνω τ'όνομα του video που θέλω να λάβω
                    consumerOut.flush();

                    int numOfChunks = consumerIn.readInt(); //λαμβάνω τον αριθμό των chunks
                    ArrayList<VideoFile> chunksReceived = new ArrayList<>();

                    for (int k = 0; k < numOfChunks; k++) {
                        VideoFile videofile = (VideoFile) consumerIn.readObject(); //βάζω τα VideoFiles που παίρνω στη λίστα
                        chunksReceived.add(videofile);
                    }

                    chunksReceived.sort(new VideoFileSorter()); //κάνω τη λίστα sort βάσει της αρίθμησης που έχει γίνει
                    playData(vName, chunksReceived); //γράφω τα chunks στο directory

                    consumerConnection.close();
                    consumerOut.close();
                    consumerIn.close();

                    ShowToastInIntentService(vName);

                    stopSelf();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    private void playData(String vName, ArrayList<VideoFile> chunksReceived){
        try {
            FileOutputStream outputVideo = new FileOutputStream(Environment.getExternalStorageDirectory() + "/project_ds_2021/toConsume/"+vName ); //το directory που θα γραφτεί το video

            for(VideoFile v : chunksReceived){
                outputVideo.write(v.getVideoFileChunk()); //γράφω το video
            }

            outputVideo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ShowToastInIntentService(final String videoName) {
        final Context MyContext = this;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast toast1 = Toast.makeText(MyContext, videoName + " downloaded!", Toast.LENGTH_LONG);
                toast1.show();
            }
        });
    };
}
