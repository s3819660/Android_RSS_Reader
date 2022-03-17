package com.example.rss_reader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FeedItemAdapter extends RecyclerView.Adapter<FeedItemAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FeedItem> feedItems;

    public FeedItemAdapter(Context mContext, ArrayList<FeedItem> feedItems) {
        this.mContext = mContext;
        this.feedItems = feedItems;
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
        FeedItem feedItem = feedItems.get(i);
        viewHolder.mTextTitle.setText(feedItem.getTitle());
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextTitle = itemView.findViewById(R.id.title_text);

            itemView.findViewById(R.id.visit_button).setOnClickListener(view -> handleVisitButtonClick(itemView));
        }

        private void handleVisitButtonClick(View itemView) {
            Uri uri = Uri.parse(feedItems.get(getBindingAdapterPosition()).getLink());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            itemView.getContext().startActivity(intent);
        }
    }
}
