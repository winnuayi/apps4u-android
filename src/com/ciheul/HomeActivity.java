package com.ciheul;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

//public class RecommenderActivity extends BaseActivity {
public class HomeActivity extends BaseActivity implements OnClickListener {
    // private static final String TAG = "RecommenderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // change header text programtically
        TextView view = (TextView) findViewById(R.id.headerActivity);
        view.setText("Apps4U");

        setContentView(R.layout.home);

        // home layout needs to be set first. otherwise, NPE occurs here
        TextView home_row1_column1 = (TextView) findViewById(R.id.home_row1_column1);
        TextView home_row1_column2 = (TextView) findViewById(R.id.home_row1_column2);
        TextView home_row2_column1 = (TextView) findViewById(R.id.home_row2_column1);
        TextView home_row2_column2 = (TextView) findViewById(R.id.home_row2_column2);

        home_row1_column1.setOnClickListener(this);
        home_row1_column2.setOnClickListener(this);
        home_row2_column1.setOnClickListener(this);
        home_row2_column2.setOnClickListener(this);

        // Log.i(TAG, "RecommenderActivity.onCreated()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Log.i(TAG, "RecommenderActivity.onResume()");
    }

    protected void onDestroy() {
        super.onDestroy();
        // Log.i(TAG, "RecommenderActivity.onDestroy()");
    }

    @Override
    public void onClick(View view) {
        switch (((TextView) view).getId()) {
        case R.id.home_row1_column1:
            Toast.makeText(this, "Menu > Empty or less result?", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, RecommenderActivity.class));
            break;
        case R.id.home_row1_column2:
            startActivity(new Intent(this, SearchActivity.class));
            break;
        case R.id.home_row2_column1:
            Toast.makeText(this, "Menu > Refresh", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SurpriseMeActivity.class));
            break;
        case R.id.home_row2_column2:
            startActivity(new Intent(this, TimeUsageActivity.class));
            break;
        }
    }

}