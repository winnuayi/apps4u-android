package com.ciheul;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;

// Main Application holds states any time. It will not be missing.
// - holds the state of services. If services stops, turn it on.
// - holds TimeUsageData handler. Every activity can access them through here.
public class RecommenderApplication extends Application {

    // private static final String TAG = "RecommenderApplication";

    // check whether service is running
    private boolean serviceRunning = false;

    // holds adapter to connect with database
    private TimeUsageData timeUsageData;

    // holds preferences across activites
    private SharedPreferences prefs;

    // constant
    public static final String LATEST_ID = "latestID";
    public static final String ANDROID_ID = "androidID";
    public static final String TIME_OFFSET = "timeOffset";
    public static final String HAS_REGISTERED = "hasRegistered";

    // interval between uploading process
    // private static final int PERIOD = 10000;
    private static final int PERIOD = 3600000;
    public static final String HOST = "http://www.hms.mhn.de:12121/apps4u";
//     public static final String HOST = "http://192.168.0.103:8080/apps4u";

    private static final String URL = HOST + "/service/registration";

    @Override
    public void onCreate() {
        super.onCreate();
        // Log.i(TAG, "RecommenderApplication.onCreate()");

        // get database adapter
        timeUsageData = new TimeUsageData(this);

        // get shared preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // set 'cron' to upload time context to server
        Intent intent = new Intent(this, ContextUploaderReceiver.class);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
            AlarmManager.RTC, 
            SystemClock.elapsedRealtime(), 
            PERIOD,
            pendingIntent);

        // set unique ID for each device
        if (prefs.getString(ANDROID_ID, null) == null) {
            final TelephonyManager tm =
                    (TelephonyManager) getSystemService(
                    Context.TELEPHONY_SERVICE);

            final String tmDevice = "" + tm.getDeviceId();
            final String tmSerial = "" + tm.getSimSerialNumber();
            final String androidID = ""
                    + android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

            UUID deviceUUID = new UUID(androidID.hashCode(), ((long) tmDevice
                .hashCode() << 32)
                    | tmSerial.hashCode());

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(ANDROID_ID, deviceUUID.toString());
            editor.commit();
        }

        // set time Offset
        if (prefs.getInt(TIME_OFFSET, -1) == -1) {
            Time now = new Time();
            now.setToNow();
            // Log.i(TAG, "gmtoff:" + now.gmtoff);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(TIME_OFFSET, (int) now.gmtoff);
            editor.commit();
        }

        // register the user
        if (prefs.getBoolean("hasRegistered", false) == false) {
            // Log.i(TAG,
            // "RecommenderApplication attempts to register the user");
            registerUser();
            if (prefs.getBoolean(HAS_REGISTERED, false) == false) {
                // Log.i(TAG, "Registration is failed.");
            }
            else {
                // Log.i(TAG, "User has been registered");
            }
        }

        // wake up the updater service if it doesn't run because of app has been
        // crashed before
        if (!isServiceRunning()) {
            startService(new Intent(this, UpdaterService.class));
            setServiceRunning(true);
        }
    }

    public void registerUser() {
        try {
            JSONObject user = new JSONObject();
            user.put(ANDROID_ID, prefs.getString(ANDROID_ID, null));
            user.put(TIME_OFFSET, prefs.getInt(TIME_OFFSET, 0));

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);
            StringEntity entity = new StringEntity(user.toString());

            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept-Encoding", "gzip");

            long start = System.currentTimeMillis();

            HttpResponse response = (HttpResponse) httpClient.execute(httpPost);

            long duration = System.currentTimeMillis() - start;
            // Log.i(TAG, "Response received in " + duration + " ms");

            // TODO Response needs to be considered to write
            // the 'hasRegistered' preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(HAS_REGISTERED, true);
            editor.commit();
        } catch (UnknownHostException e) {
            // Log.i(TAG, HOST + " isn't recognized.");
        } catch (SocketException e) {
            // Log.i(TAG, "No HTTP Response from the server");
        } catch (NoHttpResponseException e) {
            // Log.i(TAG, "No HTTP Response from the server");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
    }

    public TimeUsageData getTimeUsageData() {
        return timeUsageData;
    }

    public SharedPreferences getPref() {
        return prefs;
    }

}
