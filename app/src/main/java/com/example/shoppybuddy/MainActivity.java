package com.example.shoppybuddy;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this,R.xml.pref_main ,true );
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