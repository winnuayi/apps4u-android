package com.ciheul;

//import kankan.wheel.R;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends BaseActivity implements OnClickListener {

    // private static final String TAG = "SearchActivity";

    private final String URL = RecommenderApplication.HOST + "/service/search";

    // private SimpleGestureFilter detector;

    public static List<RecommendedItemsResponse> listApps;
    public static int totalResult;

    private int NUM_OF_RECOMMENDATIONS = 18;
    private RecommenderApplication recommenderApp;
    private SharedPreferences prefs;

    private Spinner spinnerCategory;
    private Spinner spinnerRatingValue;
    // WheelView wheelRatingValue;
    private Spinner spinnerRatingCount;
    private Spinner spinnerInstallsMin;
    private Button buttonSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MAIN LAYOUT
        setContentView(R.layout.search);

        recommenderApp = (RecommenderApplication) getApplication();
        prefs = recommenderApp.getPref();

        // HEADER
        // change header text programtically
        TextView view = (TextView) findViewById(R.id.headerActivity);
        view.setText("Search");

        // SPINNER: category
        spinnerCategory = (Spinner) findViewById(R.id.search_spinner_categories);

        ArrayAdapter<CharSequence> adapterCategories = ArrayAdapter.createFromResource(this,
                R.array.search_array_categories, android.R.layout.simple_spinner_item);
        adapterCategories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapterCategories);
        spinnerCategory.setSelection(15);

        // SPINNER: category
        spinnerRatingValue = (Spinner) findViewById(R.id.search_spinner_rating_value);

        DecimalFormat df = new DecimalFormat("#.#");
        List<Double> listRatingValue = new ArrayList<Double>();
        for (double i = 1.0; i <= 5.0; i = i + 0.1) {
            listRatingValue.add(Double.parseDouble(df.format(i)));
        }
        ArrayAdapter<Double> adapterRatingValue = new ArrayAdapter<Double>(this, android.R.layout.simple_spinner_item,
                listRatingValue);
        adapterRatingValue.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRatingValue.setAdapter(adapterRatingValue);
        spinnerRatingValue.setSelection(30);

        // // WHEEL: rating value
        // DecimalFormat df = new DecimalFormat("#.#");
        // List<Double> listRatingValue = new ArrayList<Double>();
        // for (double i = 5.0; i >= 1; i = i - 0.1) {
        // listRatingValue.add(Double.parseDouble(df.format(i)));
        // }
        // ratingValue = listRatingValue.toArray(new
        // Double[listRatingValue.size()]);
        //
        // ArrayWheelAdapter<Double> adapterRatingValue =
        // new ArrayWheelAdapter<Double>(
        // this, ratingValue);
        //
        // wheelRatingValue = (WheelView)
        // findViewById(R.id.search_wheel_rating_value);
        // wheelRatingValue.setVisibility(3);
        // wheelRatingValue.setViewAdapter(adapterRatingValue);
        // wheelRatingValue.setCurrentItem(10);

        // SPINNER: rating_count
        spinnerRatingCount = (Spinner) findViewById(R.id.search_spinner_rating_count);
        ArrayAdapter<CharSequence> adapterRatingCount = ArrayAdapter.createFromResource(this,
                R.array.search_array_rating_count, android.R.layout.simple_spinner_item);
        adapterRatingCount.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRatingCount.setAdapter(adapterRatingCount);
        // spinnerRatingCount.setSelection(3);

        // SPINNER: installs_min
        spinnerInstallsMin = (Spinner) findViewById(R.id.search_spinner_installs_min);
        ArrayAdapter<CharSequence> adapterInstallsMin = ArrayAdapter.createFromResource(this,
                R.array.search_array_installs_min, android.R.layout.simple_spinner_item);
        adapterInstallsMin.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInstallsMin.setAdapter(adapterInstallsMin);
        // spinnerInstallsMin.setSelection(8);

        // BUTTON: search
        buttonSearch = (Button) findViewById(R.id.search_button_search);
        buttonSearch.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        listApps = getSearchItems();

        if (SearchActivity.listApps.size() != 0) {
            String text = String.valueOf(listApps.size()) + " of " + SearchActivity.totalResult
                    + " results have been shuffled.";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SearchResultActivity.class));
        } else {
            String text = String.valueOf(listApps.size()) + " results";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    private List<RecommendedItemsResponse> getSearchItems() {
        List<RecommendedItemsResponse> listItems = new ArrayList<RecommendedItemsResponse>();
        try {
            // request model
            JSONObject user = new JSONObject();
            user.put(RecommenderApplication.ANDROID_ID, prefs.getString(RecommenderApplication.ANDROID_ID, null));
            user.put("numOfRecommendations", NUM_OF_RECOMMENDATIONS);
            user.put("page", 0);

            String temp0 = spinnerRatingValue.getSelectedItem().toString();
            user.put("minRatingValue", temp0);
            user.put("maxRatingValue", temp0);

            String temp = spinnerRatingCount.getSelectedItem().toString();
            temp = temp.replace(".", "");
            user.put("minRatingCount", Long.parseLong(temp));
            user.put("maxRatingCount", 10 * 7);

            String temp2 = spinnerInstallsMin.getSelectedItem().toString();
            temp2 = temp2.replace(".", "");
            user.put("minInstalls", Long.parseLong(temp2));

            user.put("category", spinnerCategory.getSelectedItem());

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
            // //Log.d(TAG, jsonString);
            JSONObject json = new JSONObject(jsonString);
            // //Log.i(TAG, "size: " + json.getInt("size"));

            this.totalResult = json.getInt("size");

            JSONArray listJsonItems = json.getJSONArray("recommendedItems");

            for (int i = 0; i < listJsonItems.length(); i++) {
                JSONObject jsonElement = listJsonItems.getJSONObject(i);
                RecommendedItemsResponse element = new RecommendedItemsResponse();

                // element.setAppLink(jsonElement.getString("app_link"));
                element.setCategory(jsonElement.getString("category"));
                // element.setDevName(jsonElement.getString("dev_name"));
                // element.setFileSize(jsonElement.getString("file_size"));
                element.setImgSrc(jsonElement.getString("img_src"));
                // element.setInstallsMin(jsonElement.getInt("installs_min"));
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
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listItems;
    }

    public List<RecommendedItemsResponse> getList() {
        return listApps;
    }

}
