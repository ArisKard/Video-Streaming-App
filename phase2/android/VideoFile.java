package com.example.project_ds_2021;
import java.util.ArrayList;
import java.io.Serializable;

public class VideoFile implements Serializable{
    static final long serialVersionUID = 45L;

    private String videoName;
    private String dateCreated;
    private String length;
    private String frameRate;
    private String frameWidth;
    private String frameHeight;


    private byte[] videoFileChunk;

    public String getName() {
        return videoName;
    }


    public byte[] getVideoFileChunk() {
        return videoFileChunk;
    }


    public VideoFile(String videoName, byte[] videoFileChunk){
        this.videoName = videoName;
        this.videoFileChunk = videoFileChunk;
    }

    public VideoFile(String videoName, String dateCreated, String length, String frameRate, String frameWidth, String frameHeight, byte[] videoFileChunk) {
        this.videoName = videoName;
        this.dateCreated = dateCreated;
        this.length = length;
        this.frameRate = frameRate;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.videoFileChunk = videoFileChunk;

    }

    public String getVideoName() {
        return videoName;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getLength() {
        return length;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public String getFrameWidth() {
        return frameWidth;
    }

    public String getFrameHeight() {
        return frameHeight;
    }
}
