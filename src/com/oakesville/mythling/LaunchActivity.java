package com.oakesville.mythling;

import com.oakesville.mythling.app.AppSettings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LaunchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSettings appSettings = new AppSettings(getApplicationContext());
        if (appSettings.isFireTv() && !appSettings.isInternalBackendHostVerified())
            startActivity(new Intent(this, WelcomeActivity.class));
        else
            startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}