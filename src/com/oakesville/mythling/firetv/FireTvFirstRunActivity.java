package com.oakesville.mythling.firetv;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

import android.app.Activity;
import android.os.Bundle;

public class FireTvFirstRunActivity extends Activity {

    private AppSettings appSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = new AppSettings(getApplicationContext());
        setContentView(R.layout.firetv_first_run);
    }

}
