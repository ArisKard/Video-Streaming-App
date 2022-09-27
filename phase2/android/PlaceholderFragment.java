package com.example.project_ds_2021.ui.main;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.project_ds_2021.BrokerInfo;
import com.example.project_ds_2021.ChannelName;
import com.example.project_ds_2021.ConnectedActivity;
import com.example.project_ds_2021.MainActivity;
import com.example.project_ds_2021.Player;
import com.example.project_ds_2021.Pull;
import com.example.project_ds_2021.R;
import com.example.project_ds_2021.Server;
import com.example.project_ds_2021.Topic;
import com.example.project_ds_2021.Upload;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends ListFragment {
    private static boolean fromThird = false;

    private boolean alreadySubscribed = false;

    private String subscribedTo = "";

    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    private Spinner s;
    private ArrayAdapter<String> adapter;

    private PlaceholderFragment fragment =  this;

    private ChannelName channelName;

    private String[] arraySpinner = null;

    private static BrokerInfo bPull;
    private static String videoSelected;

    ArrayList<String> vNames = new ArrayList<>();

    private View root;

    private LayoutInflater inflater;

    private ViewGroup container;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        channelName = ((ConnectedActivity)getActivity()).getChannelName();
        if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
            root = inflater.inflate(R.layout.fragment_list_dropdown, container, false);

            s = (Spinner) root.findViewById(R.id.spinner);

            s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    subscribedTo = s.getSelectedItem().toString();

                    new getVideos().execute();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {

                }

            });

            s.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        new getTags().execute();
                    }
                    return false;
                }
            });
        }
        else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2){
            updateView("toProduce");
        }
        else{
            updateView("toConsume");
        }


        return root;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(getArguments().getInt(ARG_SECTION_NUMBER) == 1){
            videoSelected = l.getAdapter().getItem(position).toString();

            Intent mIntent = new Intent(requireActivity(), Pull.class);
            requireActivity().startService(mIntent);
        }
        else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2){
            Intent i = new Intent(getActivity(), Upload.class);
            Bundle b = new Bundle();
            b.putString("videoName", l.getAdapter().getItem(position).toString());
            i.putExtras(b);
            startActivityForResult(i, 1);
        }
        else{
            Intent i = new Intent(getActivity(), Player.class);
            Bundle b = new Bundle();
            b.putString("videoName", l.getAdapter().getItem(position).toString());
            i.putExtras(b);
            startActivity(i);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { // get tags and call uploadVideo
            if(resultCode == 1) {
                String[] tags = data.getStringArrayExtra("Tags");
                String videoName = data.getStringExtra("videoName");

                new uploadVideo(videoName, tags).execute();
            }
        }
    }

    @Override
    public void setMenuVisibility(boolean isvisible) {
        super.setMenuVisibility(isvisible);
        if (isvisible){
            if(getArguments().getInt(ARG_SECTION_NUMBER) == 1){ //consumer
                new switchToConsumer().execute();

                fromThird = false;
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2){ //publisher
                if(!fromThird){
                    new switchToPublisher().execute();
                }
                updateView("toProduce");
                fromThird = false;
            }
            else{ // player
                updateView("toConsume");
                fromThird = true;
            }

        }else {

        }
    }

    private void updateView(String folder){
        ArrayList<String> myList = new ArrayList<String>();

        File file = new File(Environment.getExternalStorageDirectory() + "/project_ds_2021/" + folder);

        File[] list = file.listFiles();

        for (File f : list) {
            myList.add(f.getName());
        }

        ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, myList);  // pass List to ArrayAdapter

        this.setListAdapter(ad);

        root = inflater.inflate(R.layout.fragment_list, container, false);
    }

    private class switchToPublisher extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute(){super.onPreExecute();}

        @Override
        protected Boolean doInBackground (String... args){
            out = ((ConnectedActivity)getActivity()).getOut();
            in = ((ConnectedActivity)getActivity()).getIn();
            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(2));
                out.flush();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){ }
    }

    private class switchToConsumer extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute(){ super.onPreExecute(); }

        @Override
        protected Boolean doInBackground (String... args){
            out = ((ConnectedActivity)getActivity()).getOut();
            in = ((ConnectedActivity)getActivity()).getIn();
            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(2));
                out.flush();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){
            alreadySubscribed = false;
            new getTags().execute();
        }
    }

    private class getTags extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute(){ super.onPreExecute(); }

        @Override
        protected Boolean doInBackground (String... args){
            out = ((ConnectedActivity)getActivity()).getOut();
            in = ((ConnectedActivity)getActivity()).getIn();

            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(1));
                out.flush();

                message = (String) in.readObject();

                CopyOnWriteArrayList<Topic> topicsList = (CopyOnWriteArrayList<Topic>) in.readObject(); //λίστα με τα υπάρχοντα topics
                CopyOnWriteArrayList<Topic> temp = new CopyOnWriteArrayList<>(); //προσωρινή λίστα για να γίνει φιλτράρισμα
                                                                                //και να μην παρουσιαστεί το channel name μου
                arraySpinner = null;

                for (int i = 0; i < topicsList.size(); i++) {
                    if (!topicsList.get(i).getName().equals(channelName.getChannelName())) {
                        temp.add(topicsList.get(i));
                    }
                }

                arraySpinner = new String[temp.size()];
                for(int i = 0; i < temp.size(); i++){
                    arraySpinner[i] = temp.get(i).getName() + " | " + temp.get(i).getType();
                }

                if(temp.size() != 0){
                    subscribedTo = arraySpinner[0]; //και κάνω subscribe

                    Topic topic = new Topic(temp.get(0).getName(), temp.get(0).getType());
                    out.writeObject(topic);
                    out.flush();

                    bPull = (BrokerInfo) in.readObject(); //Broker υπέυθυνος για το topic που επέλεξα

                    vNames = (ArrayList<String>) in.readObject(); //λάβε τη λίστα με τα video
                }
                else{
                    arraySpinner = new String[0];
                    subscribedTo = "";

                    vNames = new ArrayList<>();

                    Topic topic = new Topic("", "");
                    out.writeObject(topic);
                    out.flush();
                }

                if(vNames == null){
                    vNames = new ArrayList<String>();
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){
            adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, arraySpinner);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            s.setAdapter(adapter);

            ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, (List<String>) vNames);  // pass List to ArrayAdapter
            fragment.setListAdapter(ad);
            root = inflater.inflate(R.layout.fragment_list, container, false);
        }
    }

    private class getVideos extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute(){ super.onPreExecute(); }

        @Override
        protected Boolean doInBackground (String... args){
            out = ((ConnectedActivity)getActivity()).getOut();
            in = ((ConnectedActivity)getActivity()).getIn();
            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(1));
                out.flush();

                message = (String) in.readObject();

                CopyOnWriteArrayList<Topic> topicsList = (CopyOnWriteArrayList<Topic>) in.readObject(); //λίστα με τα υπάρχοντα topics
                CopyOnWriteArrayList<Topic> temp = new CopyOnWriteArrayList<>(); //προσωρινή λίστα για να γίνει φιλτράρισμα
                //και να μην παρουσιαστεί το channel name μου
                arraySpinner = null;

                for (int i = 0; i < topicsList.size(); i++) {
                    if (!topicsList.get(i).getName().equals(channelName.getChannelName())) {
                        temp.add(topicsList.get(i));
                    }
                }

                arraySpinner = new String[temp.size()];
                String name = subscribedTo.split(" ")[0];
                String type = "";
                for(int i = 0; i < temp.size(); i++){
                    if(name.equals(temp.get(i).getName())){
                        type = temp.get(i).getType();
                    }
                }

                if(temp.size() != 0){
                    Topic topic = new Topic(name, type);
                    out.writeObject(topic);
                    out.flush();

                    bPull = (BrokerInfo) in.readObject(); //Broker υπέυθυνος για το topic που επέλεξα

                    vNames = (ArrayList<String>) in.readObject(); //λάβε τη λίστα με τα video
                }
                else{
                    arraySpinner = new String[0];
                    subscribedTo = "";

                    vNames = new ArrayList<>();

                    Topic topic = new Topic("", "");
                    out.writeObject(topic);
                    out.flush();
                }

                if(vNames==null){
                    vNames = new ArrayList<>();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){
            ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, (List<String>) vNames);  // pass List to ArrayAdapter
            fragment.setListAdapter(ad);
            root = inflater.inflate(R.layout.fragment_list, container, false);
        }
    }


    private class uploadVideo extends AsyncTask<String, Void, Boolean> {
        private String videoName;
        private String[] tags;

        private uploadVideo(String videoName, String[] tags){
            super();
            this.videoName = videoName;
            this.tags = tags;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();

        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected Boolean doInBackground (String... args){
            out = ((ConnectedActivity)getActivity()).getOut();
            in = ((ConnectedActivity)getActivity()).getIn();
            try {
                String message = (String) in.readObject();

                out.writeObject(String.valueOf(1));
                out.flush();

                message = (String) in.readObject();

                File file = new File(videoName);

                channelName.getPublishedVideos().add(videoName);

                boolean flag = true;
                for(String tag : tags){
                    if(flag){
                        out.writeObject(tag); //send hashtag
                        out.flush();

                        out.writeObject(videoName); //send video name
                        out.flush();

                        BrokerInfo b = hashTopic(tag); // returns which broker is responsible for this tag

                        out.writeObject(b);
                        out.flush();

                        flag = false;
                    }
                    else{
                        message = (String) in.readObject();

                        out.writeObject(String.valueOf(1));
                        out.flush();

                        message = (String) in.readObject();

                        out.writeObject(tag);
                        out.flush();

                        BrokerInfo b = hashTopic(tag);

                        out.writeObject(b);
                        out.flush();
                    }
                    if (!channelName.getPublishedHashTags().contains(tag)) { //αν δεν υπάρχει
                        channelName.getPublishedHashTags().add(tag); //βαλ'το στη λίστα

                        channelName.getPublishedHashtagVideosMap().putIfAbsent(tag, new ArrayList<>());
                        channelName.getPublishedHashtagVideosMap().get(tag).add(videoName); //και στο map
                    }
                }

                message = (String) in.readObject();

                out.writeObject(String.valueOf(2));
                out.flush();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean th){

        }
    }

    private BrokerInfo hashTopic(String topic){
        ArrayList<String> brokerHashCodes = ((ConnectedActivity)getActivity()).getBrokerHashCodes();
        HashMap<String, BrokerInfo> brokerHashCodesMap = ((ConnectedActivity)getActivity()).getBrokerHashCodesMap();
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

    public static BrokerInfo getbPull(){
        return bPull;
    }

    public static String getVideoSelected(){
        return videoSelected;
    }
}