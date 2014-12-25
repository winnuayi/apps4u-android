package com.ciheul;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SurpriseMeAdapter extends ArrayAdapter<RecommendedItemsResponse> {

    private Context context;
    private List<RecommendedItemsResponse> listItems;
    RecommendedItemsResponse item;
    BitmapDownloaderTask downloader;

    public SurpriseMeAdapter(Context context, int resource, List<RecommendedItemsResponse> listItems) {
        super(context, resource, listItems);
        this.context = context;
        this.listItems = listItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        SurpriseViewHolder holder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.surpriseme_row, parent, false);

            holder = new SurpriseViewHolder();
            holder.icon = (ImageView) v.findViewById(R.id.surpriseme_icon);
            holder.name = (TextView) v.findViewById(R.id.surpriseme_name);
            holder.category = (TextView) v.findViewById(R.id.surpriseme_category);
            holder.rating = (TextView) v.findViewById(R.id.surpriseme_rating_value);
            v.setTag(holder);
        } else {
            holder = (SurpriseViewHolder) v.getTag();
        }

        item = listItems.get(position);
        if (item != null) {
            if (cancelPotentialDownload(item.getImgSrc(), holder.icon)) {
                BitmapDownloaderTask task = new BitmapDownloaderTask(holder.icon);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                holder.icon.setImageDrawable(downloadedDrawable);
                task.execute(item.getImgSrc());
            }

            downloader = new BitmapDownloaderTask(holder.icon);
            downloader.execute(item.getImgSrc());

            holder.name.setText(item.getName());
            holder.category.setText("Category\t: " + item.getCategory());
            holder.rating.setText("Rating\t\t\t: " + String.valueOf(item.getRatingValue()) + " ("
                    + String.valueOf(item.getRatingCount()) + " users)");
        }

        return v;
    }

    public static class SurpriseViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView category;
        public TextView rating;
    }

    public class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private String url;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        protected Bitmap doInBackground(String... params) {
            url = params[0];
            try {
                return BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Bitmap result) {
            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with
                // it
                if (this == bitmapDownloaderTask) {
                    imageView.setImageBitmap(result);
                }
            }
        }

    }

    static class DownloadedDrawable extends ColorDrawable {

        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.GREEN);
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }

    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

}
