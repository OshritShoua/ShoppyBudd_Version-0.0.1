package com.example.shoppybuddy;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import java.io.File;

public class FullScreenImageActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);
        ImageView image = findViewById(R.id.fullscreen_image);
        String uri = getIntent().getStringExtra("path");
        File photo = new File(Environment.getExternalStorageDirectory(), uri);
        image.setImageURI(FileProvider.getUriForFile(FullScreenImageActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo));
    }
}