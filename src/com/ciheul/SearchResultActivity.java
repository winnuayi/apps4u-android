package com.ciheul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

public class SearchResultActivity extends FragmentActivity {

    // private static final String TAG = "SearchResultActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchresult);

        int sizePager = 0;
        if (SearchActivity.listApps.size() != 0) {
            if (SearchActivity.listApps.size() % 6 == 0)
                sizePager = SearchActivity.listApps.size() / 6;
            else
                sizePager = (SearchActivity.listApps.size() / 6) + 1;
        }

        ViewPagerAdapter adapterSearchResult = new ViewPagerAdapter(this, sizePager);

        ViewPager pager = (ViewPager) findViewById(R.id.searchresult_pager);
        pager.setAdapter(adapterSearchResult);

        PageIndicator indicator = (CirclePageIndicator) findViewById(R.id.searchresult_indicator);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new DetailOnPageChangeListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class ViewPagerAdapter extends PagerAdapter implements OnItemClickListener {

        private final Context context;
        private int sizePager;
        List<RecommendedItemsResponse> items;

        public ViewPagerAdapter(Context context, int sizePager) {
            this.context = context;
            this.sizePager = sizePager; // 30 items divided into 5 pagers
        }

        @Override
        public int getCount() {
            return sizePager;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public Object instantiateItem(View pager, int position) {
            // //Log.d(TAG, "position: " + position);
            ListView v = new ListView(context);

            items = new ArrayList<RecommendedItemsResponse>();

            int remaining = 6;
            int mod = SearchActivity.listApps.size() % 6;
            if (position == getCount() - 1 && mod != 0)
                remaining = mod;

            // //Log.d(TAG, "remaining: " + remaining);

            for (int i = position * 6; i < position * 6 + remaining; i++) {
                Map<String, String> map = new HashMap<String, String>();
                RecommendedItemsResponse item = SearchActivity.listApps.get(i);
                map.put("str", item.getName());
                items.add(item);
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
            RecommendedItemsResponse item = SearchActivity.listApps.get(currentPage * 6 + position);
            String marketUri = "market://details?id=" + item.getUid();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(marketUri));
            startActivity(intent);
        }
    }

    int currentPage;

    private class DetailOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            currentPage = position;
        }
    }

}
