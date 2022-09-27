package com.example.project_ds_2021;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Upload extends AppCompatActivity {
    String[] tags;

    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        String videoName = getIntent().getExtras().getString("videoName");

        Button upload = (Button) findViewById(R.id.uploadButton);
        Button cancel = (Button) findViewById(R.id.cancelButton);

        TextInputLayout Tags = (TextInputLayout) findViewById(R.id.Tags);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Tags.getEditText().getText().toString().isEmpty()){
                    Toast.makeText(Upload.this, "Tags are empty!", Toast.LENGTH_LONG).show();
                }
                else{
                    tags = Tags.getEditText().getText().toString().split(" ");

                    Intent intent = new Intent();
                    intent.putExtra("Tags", tags);
                    intent.putExtra("videoName", videoName);
                    setResult(1, intent);
                    finish();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(2, intent);
                finish();
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
