package com.example.rss_reader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedItemAdapter extends RecyclerView.Adapter<FeedItemAdapter.ViewHolder> {
    private static final String TAG = "FeedItemAdapter";
    private Context mContext;
    private ArrayList<FeedItem> items;
    private ArrayList<FeedItem> savedItems;
    private OnSavedItemListener onSavedItemListener;
    private boolean isSaved;

    public FeedItemAdapter(Context mContext, ArrayList<FeedItem> items, ArrayList<FeedItem> savedItems, boolean isSaved) {
        this.mContext = mContext;
        this.items = items;
        this.isSaved = isSaved;
        this.savedItems = savedItems;

        try {
            this.onSavedItemListener = ((OnSavedItemListener) mContext);
        } catch (ClassCastException e) {
            Log.d(TAG, e.getMessage());
        }

//        for (FeedItem item:
//                savedItems) {
//            Log.d("onBindViewHolder", "save" + item.toString());
//        }
//
//        for (FeedItem item:
//                items) {
//            Log.d("onBindViewHolder", "item" + item.toString());
//        }
    }

    @NonNull
    @Override
    public FeedItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.feed_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedItemAdapter.ViewHolder viewHolder, int i) {
        FeedItem feedItem = items.get(i);

        // Feed item details
        String title = feedItem.getTitle();
        viewHolder.titleText.setText(title);
        viewHolder.sourceText.setText(extractSourceString(feedItem.getLink()));
        viewHolder.pubdateText.setText(calculateTimeDifference(feedItem.getPubDate()));
        String description = extractDescription(feedItem.getDescription());
        viewHolder.descriptionText.setText(description);
        // Set dynamic number of lines for description
        if (title.length() > 60 && description != null) {
            String text = description.substring(0, 32) + "...";
            viewHolder.descriptionText.setText(text);
        }

        // Get feed item image
        new DownloadImageFromInternet(viewHolder.imageView).execute(extractImageUrlString(feedItem.getDescription()));

        // Check if item was bookmarked
        for (FeedItem item :
                savedItems) {
            if (item.getLink().equalsIgnoreCase(feedItem.getLink())) {
                viewHolder.bookmarkImage.setVisibility(View.GONE);
                viewHolder.unbookmarkImage.setVisibility(View.VISIBLE);

//                viewHolder.saveButton.setVisibility(View.GONE);
//                viewHolder.unsaveButton.setVisibility(View.VISIBLE);
                return;
            }
        }
    }

    private String calculateTimeDifference(String startDateStr) {

        // SimpleDateFormat converts the
        // string format to date object
        SimpleDateFormat formatter
                = new SimpleDateFormat("EEE, " +
                "dd MMM yyyy HH:mm:ss zzz", Locale.US);

        // Try Block
        try {

            // parse method is used to parse
            // the text from a string to
            // produce the date
//            Log.d(TAG, "startDateStr=" + startDateStr);

            Date date1 = formatter.parse(startDateStr);
            Date date2 = new Date();

            if (date1 == null)
                return null;

            // Calucalte time difference
            // in milliseconds
            long differenceInTime
                    = date2.getTime() - date1.getTime();

            // Calucalte time difference in
            // seconds, minutes, hours, years,
            // and days
            long differenceInSeconds
                    = (differenceInTime
                    / 1000)
                    % 60;

            long differenceInMinutes
                    = (differenceInTime
                    / (1000 * 60))
                    % 60;

            long differenceInHours
                    = (differenceInTime
                    / (1000 * 60 * 60))
                    % 24;

            long differenceInYears
                    = (differenceInTime
                    / (1000L * 60 * 60 * 24 * 365));

            long differenceInDays
                    = (differenceInTime
                    / (1000 * 60 * 60 * 24))
                    % 365;

            return (differenceInYears > 0 ? (differenceInYears + " year" + (differenceInYears > 1 ? "s ago" : " ago"))
                    : differenceInDays > 0 ? (differenceInDays + " day" + (differenceInDays > 1 ? "s ago" : " ago"))
                    : differenceInHours > 0 ? (differenceInHours + " hour" + (differenceInHours > 1 ? "s ago" : " ago"))
                    : differenceInMinutes > 0 ? (differenceInMinutes + " minute" + (differenceInMinutes > 1 ? "s ago" : " ago"))
                    : (differenceInSeconds + " second" + (differenceInSeconds > 1 ? "s" : "")));
        }

        // Catch the Exception
        catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractSourceString(String link) {
        try {
            URL url = new URL(link);

            String hostName = url.getHost().split("\\.")[0];

            if (hostName.isEmpty())
                return null;

            return hostName.startsWith("www") ? hostName.substring(4) : hostName;
        } catch (MalformedURLException e) {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    private String extractImageUrlString(String description) {
        try {
            final Pattern pattern = Pattern.compile("src=\"(.+?)\"", Pattern.DOTALL);
            final Matcher matcher = pattern.matcher(description);
            matcher.find();
//        Log.d("getImageUrlString", matcher.group(1));
            return matcher.group(1);
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    private String extractDescription(String infoStr) {
        try {
            final Pattern pattern = Pattern.compile("</br>(.+)", Pattern.DOTALL);
            final Matcher matcher = pattern.matcher(infoStr);
            matcher.find();
//            Log.d("extractDescription", matcher.group(1));

            return (matcher.group(1) != null ? matcher.group(1) : null);
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView sourceText;
        private TextView pubdateText;
        private TextView titleText;
        private TextView descriptionText;
        //        private Button saveButton;
//        private Button unsaveButton;
        private ImageView imageView;
        private ImageView bookmarkImage;
        private ImageView unbookmarkImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            new DownloadImageFromInternet(itemView.findViewById(R.id.image_view)).execute("");

            sourceText = itemView.findViewById(R.id.source_text);
            pubdateText = itemView.findViewById(R.id.pubdate_text);
            titleText = itemView.findViewById(R.id.title_text);
            descriptionText = itemView.findViewById(R.id.description_text);
//            saveButton = itemView.findViewById(R.id.save_button);
//            unsaveButton = itemView.findViewById(R.id.unsave_button);
            imageView = itemView.findViewById(R.id.image_view);
            bookmarkImage = itemView.findViewById(R.id.bookmark_icon);
            unbookmarkImage = itemView.findViewById(R.id.unbookmark_icon);

            // Click title to visit
            titleText.setOnClickListener(view ->
                    handleVisitButtonClick(itemView));

            unbookmarkImage.setOnClickListener(view ->
                    handleUnsaveButtonClick());
            bookmarkImage.setOnClickListener(view ->
                    handleSaveButtonClick());

//            unsaveButton.setOnClickListener(view ->
//                    handleUnsaveButtonClick());
//            saveButton.setOnClickListener(view ->
//                    handleSaveButtonClick());

            if (!isSaved) {
                unbookmarkImage.setVisibility(View.GONE);
                bookmarkImage.setVisibility(View.VISIBLE);

//                unsaveButton.setVisibility(View.GONE);
//                saveButton.setVisibility(View.VISIBLE);
                return;
            }
            bookmarkImage.setVisibility(View.GONE);
            unbookmarkImage.setVisibility(View.VISIBLE);

//            saveButton.setVisibility(View.GONE);
//            unsaveButton.setVisibility(View.VISIBLE);

        }

        private void handleVisitButtonClick(View itemView) {
            Uri uri = Uri.parse(items.get(getBindingAdapterPosition()).getLink());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            itemView.getContext().startActivity(intent);
        }

        private void handleSaveButtonClick() {

            FeedItem item = items.get(getBindingAdapterPosition());

            Intent intent = new Intent();
            intent.putExtra("savedItem", item);

            onSavedItemListener.onSavedItemListener(intent);
        }

        private void handleUnsaveButtonClick() {
            FeedItem item = items.get(getBindingAdapterPosition());

            Intent intent = new Intent();
            intent.putExtra("unsavedItem", item);

            // Visible save button
            unbookmarkImage.setVisibility(View.GONE);
            bookmarkImage.setVisibility(View.VISIBLE);
//            unsaveButton.setVisibility(View.GONE);
//            saveButton.setVisibility(View.VISIBLE);

            onSavedItemListener.onUnsavedItemListener(intent);
        }
    }

    // Async task to download RSS news item image
    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;

//            Toast.makeText(mContext, "Loading image...", Toast.LENGTH_SHORT).show();
        }

        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(imageURL).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    // interface to call from activity
    public interface OnSavedItemListener {
        void onSavedItemListener(Intent intent);

        void onUnsavedItemListener(Intent intent);
    }
}
