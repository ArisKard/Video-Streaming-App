package com.example.project_ds_2021;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class Player extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        String videoName = getIntent().getExtras().getString("videoName");

        int videoWidth = 0;
        int videoHeight = 0;

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(Environment.getExternalStorageDirectory() + "/project_ds_2021/toConsume/" + videoName);
            mp.prepare();

            videoWidth = mp.getVideoWidth();
            videoHeight = mp.getVideoHeight();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(videoName);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();

        VideoView videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setVideoPath(Environment.getExternalStorageDirectory() + "/project_ds_2021/toConsume/" + videoName);

        ViewGroup.LayoutParams params= videoView.getLayoutParams();
        params.width = screenWidth;
        params.height = (int) (((float)videoHeight / (float)videoWidth) * (float)screenWidth);
        videoView.setLayoutParams(params);

        videoView.start();
        Button playPause = (Button) findViewById(R.id.playPause);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(playPause.getText().equals("Pause")){
                    videoView.pause();
                    playPause.setText("Play");
                }
                else{
                    videoView.start();
                    playPause.setText("Pause");
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
