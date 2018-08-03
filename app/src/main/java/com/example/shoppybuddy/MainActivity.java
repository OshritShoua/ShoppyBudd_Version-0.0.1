package com.example.shoppybuddy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.primitives.Chars;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRA_MESSAGE = "com.example.shoppybuddy.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this,R.xml.pref_main ,true );
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsPrefActivity.InitValuesFromSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void OnStartShoppingButtonClick(View view)
    {
        Intent intent = new Intent(this, CartReviewActivity.class);
        intent.putExtra("calling activity", getLocalClassName());
        startActivity(intent);
    }

    public void OnMyCartsButtonClick(View view)
    {
        Intent intent = new Intent(this, CartListActivity.class);
        intent.putExtra("calling activity", getLocalClassName());
        startActivity(intent);
    }

    public void OnSettingsButtonClick(View view)
    {
        startActivity(new Intent(this, SettingsPrefActivity.class));
    }
}
