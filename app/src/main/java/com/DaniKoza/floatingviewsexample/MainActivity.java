package com.DaniKoza.floatingviewsexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.danikoza.floatingviewslib.FloatingViews;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingViews myFloatingBananas;

        myFloatingBananas = (FloatingViews) findViewById(R.id.floating_bananas_view);

        /* You must init your desired floating object after findViewByID */
        myFloatingBananas.init(R.drawable.ic_banana);

        findViewById(R.id.btn_pause).setOnClickListener(v -> myFloatingBananas.pause());
        findViewById(R.id.btn_resume).setOnClickListener(v -> myFloatingBananas.resume());

    }
}