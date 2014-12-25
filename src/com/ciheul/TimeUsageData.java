package com.ciheul;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.Time;

public class TimeUsageData {

//    private static final String TAG = "TimeUsageData";

    // database information
    private static final String DATABASE = "recommender.db";
    private static final String DB_PATH = "/data/data/com.ciheul/databases/" + DATABASE;
    private static final String TIME_TABLE = "timeusage";
    private static final int VERSION = 1;

    // table structure
    public static final String T_ID = "_id";
    public static final String T_START = "start";
    public static final String T_END = "end";
    public static final String T_DURATION = "duration";
    public static final String T_APPNAME = "appname";

    
    public static final int NO_SORT = 0;
    public static final int SORTBY_RECENCY = 1;
    public static final int SORTBY_FREQUENCY = 2;
    public static final int SORTBY_DURATION = 3; 
    
    
    // query
//    private static final String[] FIELDS = { T_ID, T_APPNAME, "SUM(" + T_DURATION + ")" };
    private static final String[] LAUNCH_FIELDS = { T_APPNAME };

    private final DbHelper dbHelper;

    private class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            super(context, DATABASE, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //Log.i(TAG, "DbHelper.onCreate() | Creating database: " + DATABASE);
            // to query records at certain date/time, convert datetime value to
            // epoch time
            // then select between two epoch time. faster.
            // it needs not declaring autoincrement explicitly as it is already
            // it needs not insert _id, just put other fields, it increments
            // automatically
            db.execSQL("create table " + TIME_TABLE + " ("
                    + T_ID + " integer primary key, "
                    + T_START + " integer, "
                    + T_END + " integer, "
                    + T_DURATION + " integer, "
                    + T_APPNAME + " text)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        }
    }

    // constructor
    public TimeUsageData(Context context) {
        //Log.i(TAG, "TimeUsageData.constructor()");
        this.dbHelper = new DbHelper(context);
    }

    // insert user's behaviour in term of app usage
    public void insertTimeUsage(String appName, long start, long end) {
        //Log.d(TAG, "SQL INSERT:" + start + "|" + end + "|" + appName);

        long duration = end - start;

        ContentValues model = new ContentValues();
        model.put(TimeUsageData.T_START, start);
        model.put(TimeUsageData.T_END, end);
        model.put(TimeUsageData.T_DURATION, duration);
        model.put(TimeUsageData.T_APPNAME, appName);

        SQLiteDatabase db = this.dbHelper.getWritableDatabase();
        try {
            // to support Android 2.1 Eclair
            db.insert(TIME_TABLE, null, model);
            
            // for Android >= 2.2
//            db.insertWithOnConflict(TIME_TABLE, null, model,
//                    SQLiteDatabase.CONFLICT_IGNORE);
        } finally {
            db.close();
        }
    }

    // get time usages that have not yet uploaded to server starting from
    // "latestID+1"
    public JSONObject getTimeUsagesToUpload(int latestID, String androidID) {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();

        String where;
        if (latestID != -1) {
            where = "_id > " + latestID;
        }
        else {
            where = null;
        }

        Cursor cursor = db.query(TIME_TABLE, null, where, null, null, null, null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        else {
            try {
                JSONObject timeUsages = new JSONObject();
                List<JSONObject> listTimeUsages = new ArrayList<JSONObject>();

                cursor.moveToFirst();
                while (cursor.isAfterLast() != true) {
                    JSONObject timeUsage = new JSONObject();

                    // our TimeUsage model
                    timeUsage.put("startEpoch", cursor.getInt(1));
                    timeUsage.put("endEpoch", cursor.getInt(2));
                    timeUsage.put("appName", cursor.getString(4));

                    listTimeUsages.add(timeUsage);

                    cursor.moveToNext();
                }

                timeUsages.put("androidID", androidID);
                // timeUsages.put("timeOffset", timeOffset);
                timeUsages.put("timeUsages", new JSONArray(listTimeUsages));

                cursor.close();

                return timeUsages;

            } catch (JSONException e) {
                //Log.i(TAG, "JSONException.");
                return null;
            }
        }
    }

    // total duration using all apps in certain period
    public Map<String, int[]> getRFD(Calendar start, Calendar end) {
        // the epoch time is already included with offset with UTC.
        // no need to add GMT offset
        // long startEpoch = (start.getTimeInMillis() / 1000);
        // long endEpoch = (end.getTimeInMillis() / 1000);

        // String period = "start > " + startEpoch + " and end < " + endEpoch;
        // return db.query(TIME_TABLE, FIELDS, null, null, T_APPNAME, null,
        // "SUM(" + T_DURATION + ") DESC");        
                       
        Map<String, int[]> rfd = new HashMap<String, int[]>();
        
        String[] FIELDS = {T_APPNAME, T_END, T_DURATION};
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();        
        Cursor cursor = db.query(TIME_TABLE, FIELDS, null, null, null, null, T_APPNAME);
        
        cursor.moveToFirst();
        while (cursor.isAfterLast() != true) {
//            //Log.i(TAG, cursor.getString(0) + "|"
//                  + cursor.getInt(1) + "|"
//                  + cursor.getInt(2));
            
            String appName = cursor.getString(0);
            int endEpoch = cursor.getInt(1);
            int duration = cursor.getInt(2);
            
            // if app has not been inserted
            if (!rfd.containsKey(appName)) {
                int[] initial = {0, 0, 0};
                rfd.put(appName, initial);
            }
            
            // current RFD for an app
            int[] values = rfd.get(appName);
                        
            // which one is recent
            if (endEpoch > values[0]) {
                values[0] = endEpoch;
            }
            
            values[1] += 1;
            values[2] += duration;
            
            rfd.put(appName, values);
            
            cursor.moveToNext();
        }
        
        // close the connection
        cursor.close();
        
        // DEBUG
//        for (Map.Entry<String, int[]> entry : rfd.entrySet()) {
//            int[] values = entry.getValue();
//            //Log.i(TAG, entry.getKey() + "\t" + values[0] + "|" + values[1] + "|" + values[2]);
//        }                       
        
        return rfd;
    }

    // frequency using an app
    public int getFrequency(String packageName) {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();
        String WHERE = T_APPNAME + " = '" + packageName + "'";
        Cursor cursor = db.query(TIME_TABLE, LAUNCH_FIELDS, WHERE, null, null, null, T_APPNAME);
        int frequency = cursor.getCount();
        cursor.close();
        return frequency;
    }

    // the difference between now and latest time using an app
    public int getRecency(String packageName) {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();

        String WHERE = T_APPNAME + " = '" + packageName + "'";
        String ORDER_BY = T_ID + " DESC";
        String[] FIELD = { T_END };

        Cursor cursor = db.query(TIME_TABLE, FIELD, WHERE, null, null, null, ORDER_BY);

        cursor.moveToFirst();
        int latest = cursor.getInt(0);

        cursor.close();

        Time now = new Time();
        now.setToNow();

        return ((int) (now.toMillis(false) / 1000) - latest);
    }

    public boolean isDatabase() {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
            db.close();
            return true;
        } catch (SQLiteException e) {
            //Log.i(TAG, "Database does not exist!");
            return false;
        }
    }

    public int getNewLatestID() {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();

        String[] FIELD = { T_ID };
        String WHERE = T_ID + "=(select max(" + T_ID + ") from " + TIME_TABLE + ")";

        Cursor cursor = db.query(TIME_TABLE, FIELD, WHERE, null, null, null, null);

        cursor.moveToFirst();
        int latestID = cursor.getInt(0);

        cursor.close();

        return latestID;
    }
}
