package com.example.rss_reader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firestore.admin.v1.Index;

import java.util.ArrayList;

public class SavedItemsActivity extends AppCompatActivity implements FeedItemAdapter.OnSavedItemListener {
    private static final String TAG = "SavedItemsActivity";

    private RecyclerView recyclerView;
    private FeedItemAdapter feedItemAdapter;
    private ArrayList<FeedItem> savedItems;

    FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_items);

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        getIntentData();

        initRecyclerView(this, savedItems);
    }

    private void getIntentData() {
        Intent intent = this.getIntent();
        savedItems = new ArrayList<>();
        savedItems = intent.getParcelableArrayListExtra("items");
//        for (FeedItem i:
//             savedItems) {
//            Log.d(TAG, i.toString());
//        }
        userEmail = intent.getStringExtra("email");
//        Log.d(TAG, userEmail);
    }

    private void initRecyclerView(Context context, ArrayList<FeedItem> itemList) {
        recyclerView = findViewById(R.id.saved_recycler_view);
//        feedItemAdapter = new FeedItemAdapter(context, itemList);
        feedItemAdapter = new FeedItemAdapter(context, itemList, savedItems, true);
        recyclerView.setAdapter(feedItemAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    @Override
    public void onSavedItemListener(Intent intent) {
        FeedItem unsavedItem = intent.getParcelableExtra("unsavedItem");
        Log.d(TAG, "unsavedItem=" + unsavedItem);
        unsaveFeedItemFromFirestore(unsavedItem);
    }

    @Override
    public void onUnsavedItemListener(Intent intent) {
        FeedItem unsavedItem = intent.getParcelableExtra("unsavedItem");
        Log.d(TAG, "unsavedItem=" + unsavedItem);
        unsaveFeedItemFromFirestore(unsavedItem);
    }

    private void unsaveFeedItemFromFirestore(FeedItem unsavedItem) {
        int removeIndex = savedItems.indexOf(unsavedItem);

        CollectionReference itemsRef = db.collection(userEmail);
        Query query = itemsRef.whereEqualTo("link", unsavedItem.getLink());
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    Log.d(TAG, "here" + document.toString());
                    itemsRef.document(document.getId()).delete();
                    try {
                        savedItems.remove(removeIndex);
                        feedItemAdapter.notifyItemRemoved(removeIndex);
                    } catch (IndexOutOfBoundsException e) {
                        Log.d(TAG, e.getMessage());
                    }
                }
                Log.d(TAG, "Item successfully deleted!");
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }
}