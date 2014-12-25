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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

public class RecommenderActivity extends FragmentActivity {

    // private static final String TAG = "RecommenderActivity";

    private final String URL = RecommenderApplication.HOST + "/service/recommendation";

    public static final int NIGHT = 0;
    public static final int MORNING = 1;
    public static final int AFTERNOON = 2;
    public static final int EVENING = 3;
    public static final int NO_CONTEXT = -1;

    private RecommenderApplication recommenderApp;
    private SharedPreferences prefs;

    private int currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int numOfRecommenderPagers = 5;

        // MAIN LAYOUT
        setContentView(R.layout.recommender);

        recommenderApp = (RecommenderApplication) getApplication();
        prefs = recommenderApp.getPref();

        // HEADER
        // change header text programtically
        // TextView view = (TextView) findViewById(R.id.headerActivity);
        // view.setText("Search");

        RecommenderPagerAdapter adapterRecommender = new RecommenderPagerAdapter(this, numOfRecommenderPagers);

        ViewPager pager = (ViewPager) findViewById(R.id.recommender_pager);
        pager.setAdapter(adapterRecommender);

        PageIndicator indicator = (TitlePageIndicator) findViewById(R.id.recommender_indicator);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new DetailOnPageChangeListener());
    }

    private class RecommenderPagerAdapter extends PagerAdapter implements OnItemClickListener {

        private final String[] TITLE = new String[] { "24 Hours", "0:00 - 6:00", "6:00 - 12:00", "12:00 - 18:00",
                "18:00 - 24:00" };

        private final Context context;
        private int numOfRecommenderPagers;
        private int NUM_OF_RECOMMENDATIONS = 10;

        private List<RecommendedItemsResponse> items;
        private List<RecommendedItemsResponse> itemsNoTimeContext;
        private List<RecommendedItemsResponse> itemsNight;
        private List<RecommendedItemsResponse> itemsMorning;
        private List<RecommendedItemsResponse> itemsAfternoon;
        private List<RecommendedItemsResponse> itemsEvening;

        public RecommenderPagerAdapter(Context context, int numOfRecommenderPagers) {
            this.context = context;
            this.numOfRecommenderPagers = numOfRecommenderPagers;

            itemsNight = getRecommendations(NIGHT);
            itemsMorning = getRecommendations(MORNING);
            itemsAfternoon = getRecommendations(AFTERNOON);
            itemsEvening = getRecommendations(EVENING);
            itemsNoTimeContext = getRecommendations(NO_CONTEXT);
        }

        @Override
        public int getCount() {
            return numOfRecommenderPagers;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public Object instantiateItem(View pager, int position) {
            // //Log.d(TAG, "position: " + position);
            ListView v = new ListView(context);

            switch (position) {
            case 0:
                items = itemsNoTimeContext;
                break;
            case 1:
                items = itemsNight;
                break;
            case 2:
                items = itemsMorning;
                break;
            case 3:
                items = itemsAfternoon;
                break;
            case 4:
                items = itemsEvening;
                break;
            }

            v.setAdapter(new SurpriseMeAdapter(context, android.R.layout.simple_list_item_1, items));

            v.setOnItemClickListener(this);
            ((ViewPager) pager).addView(v, 0);

            return v;
        }

        @Override
        public void destroyItem(View pager, int position, Object view) {
            ((ViewPager) pager).removeView((ListView) view);
        }

        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
            RecommendedItemsResponse item;
            switch (currentPage) {
            case 0:
                items = itemsNoTimeContext;
                break;
            case 1:
                items = itemsNight;
                break;
            case 2:
                items = itemsMorning;
                break;
            case 3:
                items = itemsAfternoon;
                break;
            case 4:
                items = itemsEvening;
                break;
            }

            item = items.get(position);
            String marketUri = "market://details?id=" + item.getUid();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(marketUri));
            startActivity(intent);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLE[position % TITLE.length];
        }

        private List<RecommendedItemsResponse> getRecommendations(int timeContext) {

            List<RecommendedItemsResponse> listItems = new ArrayList<RecommendedItemsResponse>();
            try {
                // request model
                JSONObject user = new JSONObject();

                user.put(RecommenderApplication.ANDROID_ID, prefs.getString(RecommenderApplication.ANDROID_ID, null));

                user.put("timeContext", timeContext);
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

                // Log.d(TAG, jsonString);

                JSONObject json = new JSONObject(jsonString);

                // //Log.i(TAG, "size: " + json.getInt("size"));

                JSONArray listJsonItems = json.getJSONArray("recommendedItems");

                for (int i = 0; i < listJsonItems.length(); i++) {
                    // String uid = listJsonItems.getString(i);
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
                // //Log.i(TAG, "Response received in " + duration + " ms");
            } catch (UnknownHostException e) {
                // //Log.i(TAG, URL + " isn't recognized.");
            } catch (SocketException e) {
                // //Log.i(TAG, "No HTTP Response from the server");
            } catch (NoHttpResponseException e) {
                // //Log.i(TAG, "No HTTP Response from the server");
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
    }

    private class DetailOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            currentPage = position;
        }
    }

    // all activities has the same menu view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recommender_menu, menu);
        return true;
    }

    // all menu view has the same behavior
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.recommender_menu_why:
            Toast.makeText(
                    this,
                    "Empty or less result happens because the app is still personalizing you. Just continue using your smartphone like usual. You could see your personalized data in Time Usages.",
                    Toast.LENGTH_LONG).show();
            break;
        }
        return true;
    }
}
