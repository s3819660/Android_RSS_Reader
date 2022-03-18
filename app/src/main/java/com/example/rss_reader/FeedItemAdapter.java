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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;

public class FeedItemAdapter extends RecyclerView.Adapter<FeedItemAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FeedItem> items;
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

    @NonNull
    @Override
    public FeedItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View heroView = inflater.inflate(R.layout.feed_item_row, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(heroView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FeedItemAdapter.ViewHolder viewHolder, int i) {
        FeedItem feedItem = items.get(i);
        viewHolder.textTitle.setText(feedItem.getTitle());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private Button saveButton;
        private Button unsaveButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            new DownloadImageFromInternet(itemView.findViewById(R.id.image_view)).execute("https://pbs.twimg.com/profile_images/630285593268752384/iD1MkFQ0.png");

            textTitle = itemView.findViewById(R.id.title_text);
            saveButton = itemView.findViewById(R.id.save_button);
            unsaveButton = itemView.findViewById(R.id.unsave_button);

            itemView.findViewById(R.id.visit_button).setOnClickListener(view ->
                    handleVisitButtonClick(itemView));

            if (!isSaved) {
                unsaveButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener(view ->
                        handleSaveButtonClick());
                return;
            }

            saveButton.setVisibility(View.GONE);
            unsaveButton.setVisibility(View.VISIBLE);
            unsaveButton.setOnClickListener(view ->
                    handleUnsaveButtonClick());
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

            onSavedItemListener.onSavedItemListener(intent);
        }
    }

    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;
            Toast.makeText(mContext, "Please wait, it may take a few minute...", Toast.LENGTH_SHORT).show();
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

    public interface OnSavedItemListener {
        public void onSavedItemListener(Intent intent);
    }
}
