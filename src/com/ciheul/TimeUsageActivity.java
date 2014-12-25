package com.ciheul;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.ciheul.SimpleGestureFilter.SimpleGestureListener;

/**
 * An activity that shows users' time usage for recency, frequency, duration.
 * 
 * @author Winnu Ayi Satria
 * 
 */
public class TimeUsageActivity extends BaseActivity implements OnItemSelectedListener, SimpleGestureListener {

    // private static final String TAG = "TimeUsageActivity";
    private SimpleGestureFilter detector;

    SimpleCursorAdapter adapter;
    ListView listTimeUsage;
    Cursor cursor;
    TimeUsageData timeUsageData;
    Spinner spinner;

    // Receiver to wake up when UpdaterService gets new time usages
    // It refreshes the time usage list by requerying the cursor
    // DEPRECATED
    class TimelineReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Log.d(TAG, "onReceived");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MAIN LAYOUT
        setContentView(R.layout.timeusage);

        // HEADER
        // change header text programtically
        TextView view = (TextView) findViewById(R.id.headerActivity);
        view.setText("Time Usages");

        // SPINNER
        spinner = (Spinner) findViewById(R.id.sortRfdSpinner);
        spinner.setOnItemSelectedListener(this);

        // Create an ArrayAdapter using the string array
        // and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort_rfd_array,
                R.layout.spinner_rfd_layout);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // GESTURE
        detector = new SimpleGestureFilter(this, this);

        // Log.i(TAG, "TimeUsageActivity.onCreated()");
    }

    /**
     * Show time usages (at the previous spinner selected).
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Log.i(TAG, "TimeUsageActivity.onResume()");

        // period. not used for a while
        Calendar start = new GregorianCalendar(2012, Calendar.JULY, 24);
        Calendar end = new GregorianCalendar(2012, Calendar.JULY, 25);

        // get TimeUsageData
        timeUsageData = ((RecommenderApplication) getApplication()).getTimeUsageData();

        // fields _id as primary key always need to be queried
        Map<String, int[]> mapRfd = timeUsageData.getRFD(start, end);

        List<RFDModel> listRfd = sortRfdBy(mapRfd, spinner.getSelectedItemPosition());

        // use our own adapter to put the rfd information to layout
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new TimeUsageAdapter(this, android.R.layout.simple_list_item_1, listRfd));
    }

    /**
     * Sort map to array by RFD.
     */
    private List<RFDModel> sortRfdBy(Map<String, int[]> unsorted, int sortBy) {

        // for (Map.Entry<String, int[]> entry : unsorted.entrySet()) {
        // String appName = entry.getKey();
        // int[] rfd = entry.getValue();
        // //Log.i(TAG, appName + "|" + rfd[0] + "|" + rfd[1] + "|" + rfd[2]);
        // }

        // to make it sortable, create a new map {appName : r/f/d}
        Map<String, Integer> simple = new HashMap<String, Integer>();
        for (Map.Entry<String, int[]> entry : unsorted.entrySet()) {
            int[] rfd = entry.getValue();
            simple.put(entry.getKey(), rfd[sortBy]);
        }

        // transform to a list
        List list = new LinkedList(simple.entrySet());

        // sort list based on comparator
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return -((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        // for performance reason, RFD is constructed to ArrayList
        // map is slower than array
        List<RFDModel> listRfd = new ArrayList<RFDModel>();

        // construct the sorted appName sorted by RFD
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            String appName = String.valueOf(entry.getKey());
            int[] rfd = unsorted.get(appName);

            RFDModel rfdEntry = new RFDModel();
            rfdEntry.setAppName(appName);
            rfdEntry.setRecency(rfd[0]);
            rfdEntry.setFrequency(rfd[1]);
            rfdEntry.setDuration(rfd[2]);

            listRfd.add(rfdEntry);

            // Log.i(TAG, appName + "|" + rfd[0] + "|" + rfd[1] + "|" + rfd[2]);
        }

        return listRfd;
    }

    /**
     * When spinner is selected, show time usage based on what is selected.
     * Recency, frequency, or duration.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // period. not used for a while
        Calendar start = new GregorianCalendar(2012, Calendar.JULY, 24);
        Calendar end = new GregorianCalendar(2012, Calendar.JULY, 25);

        // fields _id as primary key always need to be queried
        Map<String, int[]> mapRfd = timeUsageData.getRFD(start, end);

        List<RFDModel> listRfd = sortRfdBy(mapRfd, position);

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new TimeUsageAdapter(this, android.R.layout.simple_list_item_1, listRfd));

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // nothing
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
        // Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
    }

}