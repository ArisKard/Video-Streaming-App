package com.example.project_ds_2021;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ChannelName implements Serializable {
    static final long serialVersionUID = 43L;

    private String channelName; //το όνομα του καναλιού
    private ArrayList<String> publishedHashTags = new ArrayList<>(); //τα hashtags που έχουν γίνει published από το κανάλι αυτό
    private ArrayList<String> publishedVideos = new ArrayList<>(); //τα video που έχουν γίνει published από το κανάλι αυτό
    private HashMap<String, ArrayList<String>> publishedHashtagVideosMap = new HashMap<>(); //map με key ένα hashtag και value μία λίστα με τα video που έχουν γίνει published απ'αυτό το κανάλι

    public ChannelName(String name){

        channelName = name;
    }

    public String getChannelName(){
        return channelName;
    }

    public ArrayList<String> getPublishedHashTags() {
        return publishedHashTags;
    }

    public ArrayList<String> getPublishedVideos() {
        return publishedVideos;
    }

    public HashMap<String, ArrayList<String>> getPublishedHashtagVideosMap() {
        return publishedHashtagVideosMap;
    }
}
