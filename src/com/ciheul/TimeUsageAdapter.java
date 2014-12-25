package com.ciheul;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TimeUsageAdapter extends ArrayAdapter<RFDModel> {

  private static final String TAG = "TimeUsageAdapter";

  private Context context;
  private List<RFDModel> listRfd;
  PackageManager packageManager;

  public TimeUsageAdapter(
      Context context, int resource, List<RFDModel> listRfd) {
    super(context, resource, listRfd);
    this.context = context;
    this.listRfd = listRfd;
    packageManager = (PackageManager) context.getPackageManager();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    ViewHolder holder;
    if (v == null) {
      LayoutInflater vi =
          (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = vi.inflate(R.layout.timeusage_row, null);

      holder = new ViewHolder();
      holder.appName = (TextView) v.findViewById(R.id.appName);
      holder.recencyValue = (TextView) v.findViewById(R.id.recencyValue);
      holder.frequencyValue = (TextView) v.findViewById(R.id.frequencyValue);
      holder.durationValue = (TextView) v.findViewById(R.id.durationValue);
      v.setTag(holder);
    } else {
      holder = (ViewHolder) v.getTag();
    }

    RFDModel rfd = listRfd.get(position);
    if (rfd != null) {
      // TextView appName = (TextView) v.findViewById(R.id.appName);
      // TextView recencyValue =
      // (TextView) v.findViewById(R.id.recencyValue);
      // TextView frequencyValue =
      // (TextView) v.findViewById(R.id.frequencyValue);
      // TextView durationValue =
      // (TextView) v.findViewById(R.id.durationValue);

      ApplicationInfo appInfo = null;
      try {
        appInfo = packageManager.getApplicationInfo(rfd.getAppName(), 0);
      } catch (NameNotFoundException e) {
      }

      if (appInfo != null) {
        holder.appName.setText((String) packageManager
          .getApplicationLabel(appInfo));
      }
      else
        holder.appName.setText(rfd.getAppName());

      // if (holder.recencyValue != null) {
      String lastTime = getActualRecency(rfd.getRecency());
      holder.recencyValue.setText("Last time\t: " + lastTime);

      // }

      // if (holder.frequencyValue != null) {
      holder.frequencyValue.setText("Launch    \t: "
          + rfd.getFrequency() + " times");
      // }

      // if (holder.durationValue != null) {
      String preciseDuration =
          getPreciseDuration((long) rfd.getDuration());
      holder.durationValue.setText("Duration\t: " + preciseDuration);
      // }
    }
    return v;
  }

  public static class ViewHolder {
    public TextView appName;
    public TextView recencyValue;
    public TextView frequencyValue;
    public TextView durationValue;
  }

  private String getActualRecency(int recency) {
    // get current time in epoch based on UTC
    Time now = new Time();
    now.setToNow();
    int nowEpoch = (int) (now.toMillis(false) / 1000);

    // find difference in seconds
    int difference = nowEpoch - recency;

    // within a minute
    if (difference < 60) {
      return String.valueOf(difference) + " seconds ago";
    }
    // within an hour
    else if (difference < 3600) {
      int minutes = difference / 60;
      if (minutes == 1)
        return minutes + " minute ago";
      else
        return minutes + " minutes ago";
    }
    // within a day
    else if (difference < 86400) {
      int hours = difference / 3600;
      if (hours == 1)
        return hours + " hour ago";
      else
        return hours + " hours ago";
    }
    // within a month (31 days)
    else if (difference < 2678400) {
      int days = difference / 86400;
      if (days == 1)
        return "yesterday";
      else
        return days + " days ago";
    }

    return "more than a month";
  }

  private String getPreciseDuration(long duration) {
    String hms = DateUtils.formatElapsedTime(duration); // 2 ms
    String[] token = hms.split(":"); // 7 ms
    if (token.length == 2) {
      int m = Integer.parseInt(token[0]); // 2 ms
      int s = Integer.parseInt(token[1]); // 2 ms
      if (m == 0) {
        // return s + (s == 1 ? " second" : " seconds");
        return s + "s";
      } else {
        return m + "m" + s + "s";
        // return m + (m == 1 ? " minute " : " minutes ") + s
        // + (s == 1 ? " second" : " seconds");
      }
    } else {
      int h = Integer.parseInt(token[0]); // 2 ms
      int m = Integer.parseInt(token[1]); // 2 ms
      int s = Integer.parseInt(token[2]); // 2 ms
      // return "test";
      return h + "h" + m + "m" + s + "s";
      // return h + (h == 1 ? " hour " : " hours ")
      // + m + (m == 1 ? " minute " : " minutes ")
      // + s + (s == 1 ? " second" : " seconds");
    }

  }

}
