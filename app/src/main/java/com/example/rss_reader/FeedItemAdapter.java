package com.example.rss_reader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.service.autofill.ImageTransformation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedItemAdapter extends RecyclerView.Adapter<FeedItemAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FeedItem> items;
    private ArrayList<FeedItem> savedItems;
    private OnSavedItemListener onSavedItemListener;
    private boolean isSaved;

    public FeedItemAdapter(Context mContext, ArrayList<FeedItem> items, boolean isSaved) {
        this.mContext = mContext;
        this.items = items;
        this.isSaved = isSaved;

        try {
            this.onSavedItemListener = ((OnSavedItemListener) mContext);
        } catch (ClassCastException e) {
            Log.d("FeedItemAdapter", e.getMessage());
        }
    }

    public FeedItemAdapter(Context mContext, ArrayList<FeedItem> items, ArrayList<FeedItem> savedItems, boolean isSaved) {
        this.mContext = mContext;
        this.items = items;
        this.isSaved = isSaved;
        this.savedItems = savedItems;

        try {
            this.onSavedItemListener = ((OnSavedItemListener) mContext);
        } catch (ClassCastException e) {
            Log.d("FeedItemAdapter", e.getMessage());
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
        View view = inflater.inflate(R.layout.feed_item_row, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedItemAdapter.ViewHolder viewHolder, int i) {
        FeedItem feedItem = items.get(i);
        viewHolder.textTitle.setText(feedItem.getTitle());

        new DownloadImageFromInternet(viewHolder.imageView).execute(getImageUrlString(feedItem.getDescription()));


        for (FeedItem item:
             savedItems) {
            if (item.getLink().equalsIgnoreCase(feedItem.getLink())) {
                viewHolder.saveButton.setVisibility(View.GONE);
                viewHolder.unsaveButton.setVisibility(View.VISIBLE);
                return;
            }
        }
    }

    private String getImageUrlString(String description) {
        final Pattern pattern = Pattern.compile("src=\"(.+?)\"", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(description);
        matcher.find();
//        Log.d("getImageUrlString", matcher.group(1)); // Prints String I want to extract
        return matcher.group(1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private Button saveButton;
        private Button unsaveButton;
        private ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            new DownloadImageFromInternet(itemView.findViewById(R.id.image_view)).execute("");

            textTitle = itemView.findViewById(R.id.title_text);
            saveButton = itemView.findViewById(R.id.save_button);
            unsaveButton = itemView.findViewById(R.id.unsave_button);
            imageView = itemView.findViewById(R.id.image_view);

            itemView.findViewById(R.id.visit_button).setOnClickListener(view ->
                    handleVisitButtonClick(itemView));

            unsaveButton.setOnClickListener(view ->
                    handleUnsaveButtonClick());
            saveButton.setOnClickListener(view ->
                    handleSaveButtonClick());

            if (!isSaved) {
                unsaveButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
                return;
            }
            saveButton.setVisibility(View.GONE);
            unsaveButton.setVisibility(View.VISIBLE);

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
            unsaveButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.VISIBLE);

            onSavedItemListener.onUnsavedItemListener(intent);
        }
    }

    // Async task to download RSS news item image
    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;

            Toast.makeText(mContext, "Loading image...", Toast.LENGTH_SHORT).show();
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
