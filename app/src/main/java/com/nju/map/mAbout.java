package com.nju.map;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

public class mAbout extends AppCompatActivity {
    private View myAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = this.getLayoutInflater();
        myAbout = inflater.inflate(R.layout.activity_m_about, null);

        aboutinit();
    }

    private void aboutinit() {
        setContentView(myAbout);
    }
}


