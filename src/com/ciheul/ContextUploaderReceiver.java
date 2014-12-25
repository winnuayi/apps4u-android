package com.ciheul;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ContextUploaderReceiver extends BroadcastReceiver {

    private static final String TAG = "ContextUploaderReceiver";

    private static final String URL = RecommenderApplication.HOST + "/service/timeusage";

    RecommenderApplication recommenderApp;

    @Override
    public void onReceive(Context context, Intent intent) {
        // //Log.i(TAG, "ContextUploaderReceiver.onReceive()");
        uploadTimeUsages(context);
    }

    public void uploadTimeUsages(Context context) {
        // Log.i(TAG, "ContextUploaderReceiver.uploadTimeUsages()");

        try {
            recommenderApp = (RecommenderApplication) context.getApplicationContext();

            // get the checkpoint from previous schedule
            SharedPreferences pref = recommenderApp.getPref();
            int latestID = pref.getInt(recommenderApp.LATEST_ID, -1);
            String androidID = pref.getString(recommenderApp.ANDROID_ID, "");

            // get whether the user has been registered
            boolean hasRegistered = pref.getBoolean(recommenderApp.HAS_REGISTERED, false);

            // Log.i(TAG, "hasRegistered = " + hasRegistered);
            // Log.i(TAG, "latestID      = " + latestID);

            // check whether server is alive. this is to reduce unnecessary
            // computation
            // and cost when uploading a chunk of data that obviously will be
            // dropped
            // because the destination server is down
            if (!isServerAlive()) {
                // Log.i(TAG, "Server is alive.");

                // try to register
                if (hasRegistered == false) {
                    // Log.i(TAG, "ContextUploader try to register user.");
                    recommenderApp.registerUser();
                    hasRegistered = pref.getBoolean(recommenderApp.HAS_REGISTERED, false);
                }

                // if the user has been registered
                if (hasRegistered == true) {
                    JSONObject timeUsages = recommenderApp.getTimeUsageData()
                            .getTimeUsagesToUpload(latestID, androidID);

                    // upload if there is at least one new entry
                    if (timeUsages != null) {
                        // get the latestID now but commit later.
                        // to ensure, during the uploading process, there is no
                        // new
                        // entry inserted to SQLite
                        latestID = recommenderApp.getTimeUsageData().getNewLatestID();
                        // Log.i(TAG, "New latestID=" + latestID);

                        // //Log.i(TAG, timeUsages.toString());

                        DefaultHttpClient httpClient = new DefaultHttpClient();
                        HttpPost httpPost = new HttpPost(URL);
                        StringEntity entity = new StringEntity(timeUsages.toString());

                        httpPost.setEntity(entity);
                        httpPost.setHeader("Accept", "application/json");
                        httpPost.setHeader("Content-type", "application/json");
                        httpPost.setHeader("Accept-Encoding", "gzip");

                        long start = System.currentTimeMillis();

                        HttpResponse response = (HttpResponse) httpClient.execute(httpPost);

                        long duration = System.currentTimeMillis() - start;
                        // Log.i(TAG, "Response received in " + duration +
                        // " ms");

                        // updating the checkpoint for uploading in the next
                        // schedule
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putInt(recommenderApp.LATEST_ID, latestID);
                        editor.commit();
                    } else {
                        // Log.i(TAG, "No new entry.");
                    }
                } else {
                    // Log.i(TAG, "User has not been registered.");
                }
            } else {
                // Log.i(TAG, "Server is down.");
            }

        } catch (NoHttpResponseException e) {
            // Log.i(TAG, "No HTTP Response from the server");
        } catch (HttpHostConnectException e) {
            // Log.i(TAG, "The network is slow or server is down.");
        } catch (UnsupportedEncodingException e) {
            // Log.i(TAG, "UnsupportedEncodingException.");
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isServerAlive() {
        boolean isAlive = false;
        Socket socket = null;

        try {
            socket = new Socket(RecommenderApplication.HOST, 12405);
            isAlive = true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        } finally {
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e) {
                }
        }
        return isAlive;
    }

}
