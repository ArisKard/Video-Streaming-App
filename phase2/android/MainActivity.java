package com.example.project_ds_2021;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    String internalPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File f = new File(Environment.getExternalStorageDirectory() + "/project_ds_2021");
        if(!f.isDirectory()){
            f.mkdirs();
            f = new File(f + "/toConsume");
            f.mkdirs();
            f = new File(Environment.getExternalStorageDirectory() + "/project_ds_2021/toProduce");
            f.mkdirs();
        }


        TextInputLayout IPinput = (TextInputLayout) findViewById(R.id.IPinput);
        TextInputLayout PortInput = (TextInputLayout) findViewById(R.id.PortInput);
        TextInputLayout ChannelInput = (TextInputLayout) findViewById(R.id.ChannelInput);

        Button connect = (Button) findViewById(R.id.ConnectButton);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IPinput.getEditText().getText().toString().isEmpty() ||
                   PortInput.getEditText().getText().toString().isEmpty() ||
                   ChannelInput.getEditText().getText().toString().isEmpty()){
                    Toast.makeText(MainActivity.this, "Wrong Input!", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent i = new Intent(MainActivity.this, ConnectedActivity.class);
                    i.putExtra("IPinput", IPinput.getEditText().getText().toString());
                    i.putExtra("PortInput", PortInput.getEditText().getText().toString());
                    i.putExtra("ChannelInput", ChannelInput.getEditText().getText().toString());
                    finish();  //Kill the activity from which you will go to next activity
                    startActivity(i);
                }
            }
        });
    }

    // Hide keyboard when blank is clicked
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}