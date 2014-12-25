package com.ciheul;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ciheul.SimpleGestureFilter.SimpleGestureListener;

public class SurpriseMeActivity extends BaseActivity implements OnClickListener, OnItemClickListener,
        SimpleGestureListener {

    private SimpleGestureFilter detector;

    // private static final String TAG = "SurpriseMeActivity";

    private int NUM_OF_RECOMMENDATIONS = 5;

    private final String URL = RecommenderApplication.HOST + "/service/surpriseme";

    private RecommenderApplication recommenderApp;
    private SharedPreferences prefs;
    private List<RecommendedItemsResponse> listApps;

    private Button buttonSurpriseMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MAIN LAYOUT
        setContentView(R.layout.surpriseme);

        // HEADER
        // change header text programtically
        TextView view = (TextView) findViewById(R.id.headerActivity);
        view.setText("Surprise Me!");

        recommenderApp = (RecommenderApplication) getApplication();
        prefs = recommenderApp.getPref();

        detector = new SimpleGestureFilter(this, this);

        // BUTTON: search
        buttonSurpriseMe = (Button) findViewById(R.id.surpriseme_button_surpriseme);
        buttonSurpriseMe.setOnClickListener(this);

        // Log.i(TAG, "SupriseMeActivity.onCreated()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        listApps = getSurpriseItems();

        // use our own adapter to put the rfd information to layout
        ListView listView = (ListView) findViewById(R.id.surpriseme_list);
        listView.setAdapter(new SurpriseMeAdapter(this, android.R.layout.simple_list_item_1, listApps));

        listView.setOnItemClickListener(this);

    }

    private List<RecommendedItemsResponse> getSurpriseItems() {
        List<RecommendedItemsResponse> listItems = new ArrayList<RecommendedItemsResponse>();
        try {
            // Log.i(TAG, "Sending Surprise Me! request.");

            JSONObject user = new JSONObject();
            user.put(RecommenderApplication.ANDROID_ID, prefs.getString(RecommenderApplication.ANDROID_ID, null));
            user.put("timeContext", -1);
            user.put("numOfRecommendations", NUM_OF_RECOMMENDATIONS);

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);
            StringEntity entity = new StringEntity(user.toString());

            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept-Encoding", "gzip");

            // long start = System.currentTimeMillis();

            HttpResponse response = (HttpResponse) httpClient.execute(httpPost);
            String jsonString = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);

            JSONObject json = new JSONObject(jsonString);
            // //Log.i(TAG, "size: " + json.getInt("size"));
            JSONArray listJsonItems = json.getJSONArray("recommendedItems");

            // for (int i = 0; i < listJsonItems.length(); i++) {
            // JSONObject jsonElement = listJsonItems.getJSONObject(i);
            // Map<String, Object> element = new HashMap<String, Object>();
            // Iterator iterator = jsonElement.keys();
            // while (iterator.hasNext()) {
            // String key = (String) iterator.next();
            // Object value = jsonElement.get(key);
            // element.put(key, value);
            // }
            // listItems.add(element);
            // }

            for (int i = 0; i < listJsonItems.length(); i++) {
                JSONObject jsonElement = listJsonItems.getJSONObject(i);
                RecommendedItemsResponse element = new RecommendedItemsResponse();

                element.setCategory(jsonElement.getString("category"));
                element.setImgSrc(jsonElement.getString("img_src"));
                element.setName(jsonElement.getString("name"));
                element.setRatingCount(jsonElement.getInt("rating_count"));
                element.setRatingValue(jsonElement.getDouble("rating_value"));
                element.setUid(jsonElement.getString("uid"));

                listItems.add(element);
            }
            // long duration = System.currentTimeMillis() - start;
            // Log.i(TAG, "Response received in " + duration + " ms");
        } catch (UnknownHostException e) {
            // Log.i(TAG, URL + " isn't recognized.");
        } catch (SocketException e) {
            // Log.i(TAG, "No HTTP Response from the server");
        } catch (NoHttpResponseException e) {
            // Log.i(TAG, "No HTTP Response from the server");
        } catch (JSONException e) {
            // e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // e.printStackTrace();
        } catch (ClientProtocolException e) {
            // e.printStackTrace();
        } catch (IOException e) {
            // e.printStackTrace();
        }

        return listItems;
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View arg1, int position, long id) {
        RecommendedItemsResponse item = listApps.get(position);
        String marketUri = "market://details?id=" + item.getUid();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(marketUri));
        startActivity(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    @Override
    public void onSwipe(int direction) {
        switch (direction) {

        case SimpleGestureFilter.SWIPE_RIGHT:
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            break;
        }
    }

    @Override
    public void onDoubleTap() {
    }

    @Override
    public void onClick(View view) {
        onResume();
    }

    // all activities has the same menu view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surpriseme_menu, menu);
        return true;
    }

    // all menu view has the same behavior
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.surpriseme_refresh:
            onResume();
            break;
        }
        return true;
    }
}