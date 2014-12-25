package com.ciheul;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove default title bar and create custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        // our main layout
        setContentView(R.layout.timeusage);

        // append the title bar above the main layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ensure service always runs
        if (!((RecommenderApplication) getApplication()).isServiceRunning()) {
            startService(new Intent(this, UpdaterService.class));
            ((RecommenderApplication) getApplication()).setServiceRunning(true);
        }
    }

    // // all activities has the same menu view
    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    // getMenuInflater().inflate(R.menu.menu, menu);
    // return true;
    // }
    //
    // // all menu view has the same behavior
    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    // switch (item.getItemId()) {
    // case R.id.menuTimeUsage:
    // startActivity(new Intent(this, TimeUsageActivity.class));
    // break;
    // case R.id.menuRecommendedApps:
    // startActivity(new Intent(this, HomeActivity.class));
    // break;
    // }
    // return true;
    // }

}
