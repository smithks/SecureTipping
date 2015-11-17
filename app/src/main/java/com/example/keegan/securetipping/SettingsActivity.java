package com.example.keegan.securetipping;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created activity to hold settings fragment to include toolbar.
 * Created by Keegan on 11/12/2015.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_toolbar);
        getFragmentManager().beginTransaction().replace(R.id.preference_frame,new SettingsFragment()).commit();


    }
}
