package com.ciheul;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.ActivityManager;
import android.app.IActivityWatcher;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

public class UpdaterService extends Service {

    Context context;
    private static final String TAG = "UpdaterService";
    private boolean activityWatcherRunning = false;

    static ActivityManager.RunningTaskInfo prevTask = null;

    // ActivityWatcher variables
    Time start = null;
    Time end = null;
    Time temp = null;
    private int launcherActivityId = 0;

    RecommenderApplication recommenderApp;
    private SharedPreferences prefs;
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.i(TAG, "UpdaterService.onCreate()");

        recommenderApp = (RecommenderApplication) getApplication();

        if (!isActivityWatcherRunning()) {
            prevTask = null;
            runActivityWatcher();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        Log.i(TAG, "UpdaterService.onStartCommand()");
        // uploadTimeUsages();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((RecommenderApplication) getApplication()).setServiceRunning(false);
//        Log.i(TAG, "UpdaterService.onDestroy()");
    }

    // track user's behavior
    private IActivityWatcher.Stub activityWatcher = new IActivityWatcher.Stub() {
        public void activityResuming(int activityId) throws RemoteException {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo currentTask = tasks.get(0);

            // FILTER OUT
            if (currentTask.baseActivity.getPackageName().contains("launch") || launcherActivityId == activityId) {
                if (launcherActivityId == 0)
                    launcherActivityId = activityId;

                end = new Time();
                end.setToNow();

//                Log.i(TAG, end.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|stop |suppress launcher|activityId:"
//                        + activityId);

                if (start != null) {
                    // send time usage to SQLite
                    String appName = prevTask.baseActivity.getPackageName();
                    long startEpoch = start.toMillis(false) / 1000;
                    long endEpoch = end.toMillis(false) / 1000;
                    ((RecommenderApplication) getApplication())
                            .getTimeUsageData().insertTimeUsage(appName, startEpoch, endEpoch);
                }

                start = null;
                end = null;
                prevTask = null;
            }
            // FILTER OUT
            else if (currentTask.numRunning == 0) {
                end = new Time();
                end.setToNow();

//                Log.i(TAG, end.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|stop |no activity running|"
//                        + currentTask.numActivities + "|"
//                        + currentTask.numRunning);

                if (start != null) {
                    // send time usage to SQLite
                    String appName = prevTask.baseActivity.getPackageName();
                    long startEpoch = start.toMillis(false) / 1000;
                    long endEpoch = end.toMillis(false) / 1000;
                    ((RecommenderApplication) getApplication())
                            .getTimeUsageData().insertTimeUsage(appName, startEpoch, endEpoch);
                }

                start = null;
                end = null;
                prevTask = null;
            }
            // IN
            else if (prevTask == null) {
                start = new Time();
                start.setToNow();

//                Log.i(TAG, start.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|start|"
//                        + currentTask.baseActivity.getPackageName() + "|"
//                        + currentTask.topActivity.getShortClassName() + "|"
//                        + currentTask.numActivities + "|"
//                        + currentTask.numRunning);

                prevTask = currentTask;
            }
            // OUT
            else if ((prevTask.baseActivity.getPackageName().equals(currentTask.baseActivity.getPackageName())
                    && currentTask.numRunning <= 1
                    && currentTask.numActivities <= 1)) {
                end = new Time();
                end.setToNow();

//                Log.i(TAG, end.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|stop |backkey|"
//                        + currentTask.baseActivity.getPackageName() + "|"
//                        + currentTask.baseActivity.getShortClassName() + "|"
//                        + currentTask.numActivities + "|"
//                        + currentTask.numRunning);

                // send time usage to SQLite
                String appName = prevTask.baseActivity.getPackageName();
                long startEpoch = start.toMillis(false) / 1000;
                long endEpoch = end.toMillis(false) / 1000;
                ((RecommenderApplication) getApplication())
                        .getTimeUsageData().insertTimeUsage(appName, startEpoch, endEpoch);

                start = null;
                end = null;
                prevTask = null;
            }
            // MOVE
            else if (!prevTask.baseActivity.getPackageName().equals(currentTask.baseActivity.getPackageName())) {
                end = new Time();
                end.setToNow();

//                Log.i(TAG, end.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|stop |"
//                        + prevTask.baseActivity.getPackageName() + " moves -> "
//                        + currentTask.baseActivity.getPackageName() + "|"
//                        + prevTask.numActivities + "|"
//                        + prevTask.numRunning);

                // Send time usage to SQL
                String appName = prevTask.baseActivity.getPackageName();
                long startEpoch = start.toMillis(false) / 1000;
                long endEpoch = end.toMillis(false) / 1000;
                ((RecommenderApplication) getApplication())
                        .getTimeUsageData().insertTimeUsage(appName, startEpoch, endEpoch);

                start = end;
                prevTask = currentTask;

//                Log.i(TAG, start.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|start|"
//                        + currentTask.baseActivity.getPackageName() + "|"
//                        + currentTask.topActivity.getShortClassName() + "|"
//                        + currentTask.numActivities + "|"
//                        + currentTask.numRunning);
            }
            // STAY
            else {
                Time temp = new Time();
                temp.setToNow();

//                Log.i(TAG, temp.format("%a|%F|%T|%z|")
//                        + (System.currentTimeMillis() / 1000) + "|stay |"
//                        + currentTask.baseActivity.getPackageName() + "|"
//                        + currentTask.topActivity.getShortClassName() + "|"
//                        + currentTask.numActivities + "|"
//                        + currentTask.numRunning + "|"
//                        + activityId);
            }
        }

        public void closingSystemDialogs(String reason) {
            end = new Time();
            end.setToNow();
//            Log.i(TAG, end.format("%a|%F|%T|%z|")
//                    + (System.currentTimeMillis() / 1000) + "|stop |"
//                    + reason);

            // send time usage to SQLite
            String appName = prevTask.baseActivity.getPackageName();
            long startEpoch = start.toMillis(false) / 1000;
            long endEpoch = end.toMillis(false) / 1000;
            ((RecommenderApplication) getApplication())
                    .getTimeUsageData().insertTimeUsage(appName, startEpoch, endEpoch);
//            Log.i(TAG, "Time.toMillis:" + startEpoch);
            start = null;
            end = null;
            prevTask = null;
        }
    };

    private void runActivityWatcher() {
//        Log.i(TAG, "UpdaterService.runActivityWatcher()");
        // access hidden API
        Class ActivityManagerNative = null;
        try {
            ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
        } catch (ClassNotFoundException e) {
//            Log.i(TAG, "Abstract class ActivityManagerNative is not found!");
        }

        // access hidden API
        Class IActivityManager = null;
        try {
            IActivityManager = Class.forName("android.app.IActivityManager");
        } catch (ClassNotFoundException e) {
//            Log.i(TAG, "Class IActivityManager is not found!");
        }

        Method getDefault = null;
        try {
            getDefault = ActivityManagerNative.getMethod("getDefault", null);
        } catch (SecurityException e) {
//            Log.i(TAG, "Security reason!");
        } catch (NoSuchMethodException e) {
//            Log.i(TAG, "Method getDefault is not found!");
        }

        Object activityManager = null;
        try {
            activityManager = IActivityManager.cast(getDefault.invoke(ActivityManagerNative, null));
        } catch (IllegalArgumentException e) {
//            Log.i(TAG, "Illegal Argument Exception!");
        } catch (IllegalAccessException e) {
//            Log.i(TAG, "IllegalAccessException!");
        } catch (InvocationTargetException e) {
//            Log.i(TAG, "InvocationTargetException!");
        }

        Method registerActivityWatcher = null;
        try {
            registerActivityWatcher = activityManager.getClass().getMethod("registerActivityWatcher", IActivityWatcher.class);
        } catch (SecurityException e) {
//            Log.i(TAG, "Security reason!");
        } catch (NoSuchMethodException e) {
//            Log.i(TAG, "Method registerActivityWatcher is not found!");
        }

        try {
            registerActivityWatcher.invoke(activityManager, activityWatcher);
        } catch (IllegalArgumentException e) {
//            Log.i(TAG, "Illegal Argument Exception!");
        } catch (IllegalAccessException e) {
//            Log.i(TAG, "IllegalAccessException!");
        } catch (InvocationTargetException e) {
//            Log.i(TAG, "InvocationTargetException!");
        }

        // without this, activitywatcher become useless
        // maybe this can be moved to onCreated() ?
        context = this.getApplicationContext();
    }

    public boolean isActivityWatcherRunning() {
        return activityWatcherRunning;
    }

    public void setActivityWatcherRunning(boolean activityWatcherRunning) {
        this.activityWatcherRunning = activityWatcherRunning;
    }
 
}