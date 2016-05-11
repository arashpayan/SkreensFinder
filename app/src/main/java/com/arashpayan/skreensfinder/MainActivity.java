package com.arashpayan.skreensfinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Explorer.ExplorerListener {

    private StringBuilder logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        logText = new StringBuilder("onStart\n");
        updateLog();
        Explorer.getExplorer().setListener(this).startExploring(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Explorer.getExplorer().setListener(null).stopExploring();
    }

    @Override
    public void serviceFound(SkreensService ss) {
        logText.append(String.format("device found: %s\n", ss.toString()));
        updateLog();
    }

    @Override
    public void serviceRemoved(String serviceName) {
        logText.append(String.format("device removed: %s\n", serviceName));
        updateLog();
    }

    private void updateLog() {
        TextView logView = (TextView) findViewById(R.id.log_textview);
        if (logView == null) {
            throw new RuntimeException("where's the log view?");
        }
        logView.setText(logText.toString());
    }
}
