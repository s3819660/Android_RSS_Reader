package com.example.rss_reader;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private static final String TAG = "CategoryAdapter";
    private Context mContext;
    private ArrayList<Category> categories;
    private OnCategoryClickListener onCategoryClickListener;

    public CategoryAdapter(Context mContext, ArrayList<Category> categories) {
        this.mContext = mContext;
        this.categories = categories;

        try {
            this.onCategoryClickListener = ((OnCategoryClickListener) mContext);
        } catch (ClassCastException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.category_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        Category category = categories.get(i);

        holder.imageView.setImageResource(category.getImageSrc());
        holder.textView.setText(category.getTitle());
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView imageView;
        private TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.category_card);
            imageView = itemView.findViewById(R.id.category_image);
            textView = itemView.findViewById(R.id.category_title_text);

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleCategoryClick(categories.get(getBindingAdapterPosition()));
                }
            });
        }

        private void handleCategoryClick(Category category) {
            Intent intent = new Intent();
            intent.putExtra("categoryUrl", category.getUrl());

            onCategoryClickListener.onCategoryClickListener(intent);
        }
    }

    // interface to call from activity
    public interface OnCategoryClickListener {
        void onCategoryClickListener(Intent intent);
    }
}
