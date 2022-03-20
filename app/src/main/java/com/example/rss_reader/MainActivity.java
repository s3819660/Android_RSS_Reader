package com.example.rss_reader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements FeedItemAdapter.OnSavedItemListener, CategoryAdapter.OnCategoryClickListener {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    FirebaseFirestore db;
    String userEmail;

    ArrayList<String> titles;
    ArrayList<String> descriptions;
    ArrayList<String> links;
    ArrayList<String> pubDates;
    ArrayList<Category> categories;

    private RecyclerView recyclerView;
    private FeedItemAdapter feedItemAdapter;
    private ArrayList<FeedItem> feedItems;
    private ArrayList<FeedItem> savedItems;
    private int lastSavedItemIndex;

    private SignInButton signInButton;
    private SearchView searchView;
    private TextView categoryText;
    private ImageView menuButton;
    private String urlString;
    private String categoryString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize feed url
        urlString = "https://vnexpress.net/rss/the-gioi.rss";
        categoryString = "WORLD";

        // Get views
        getViews();
    }

    // Get all view components
    private void getViews() {
        // Initialize array lists
        titles = new ArrayList<>();
        descriptions = new ArrayList<>();
        links = new ArrayList<>();
        pubDates = new ArrayList<>();

        feedItems = new ArrayList<>();
        savedItems = new ArrayList<>();

        categories = new ArrayList<>();
        initCategories();

        // Get reference to views
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setColorScheme(SignInButton.COLOR_DARK);
        TextView textView = (TextView) signInButton.getChildAt(0);
        textView.setText(R.string.sign_in_with_google);
        signInButton.setOnClickListener(view -> signIn());

        // URL edit text
        searchView = findViewById(R.id.rss_search_view);
        // Perform set on query text listener event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                handleQuerySubmit(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        // Recycler view for feed
        recyclerView = findViewById(R.id.recycler_view);

        // Popup menu
        menuButton = findViewById(R.id.menu_icon);
        menuButton.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, menuButton);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.saved_menu_item)
                    handleSavedListButtonClick();
                else if (item.getItemId() == R.id.sign_out_menu_item)
                    handleSignOutButtonClick();

                return true;
            });

            popupMenu.show();//showing popup menu
        });

        // Category
        categoryText = findViewById(R.id.category_text);
        categoryText.setText(categoryString);
    }

    private void initCategories() {
        categories.add(new Category("World", "https://vnexpress.net/rss/the-gioi.rss"
                , R.drawable.world));
        categories.add(new Category("Politics", "https://vnexpress.net/rss/thoi-su.rss"
                , R.drawable.politics));
        categories.add(new Category("Business", "https://vnexpress.net/rss/kinh-doanh.rss"
                , R.drawable.business));
        categories.add(new Category("Tech", "https://vnexpress.net/rss/so-hoa.rss"
                , R.drawable.tech));
        categories.add(new Category("Science", "https://vnexpress.net/rss/khoa-hoc.rss"
                , R.drawable.science));
        categories.add(new Category("Health", "https://vnexpress.net/rss/suc-khoe.rss"
                , R.drawable.health));
        categories.add(new Category("Sports", "https://vnexpress.net/rss/suc-khoe.rss"
                , R.drawable.sports));
        categories.add(new Category("Entertainment", "https://vnexpress.net/rss/giai-tri.rss"
                , R.drawable.entertainment));
        categories.add(new Category("Food", "https://thanhnien.vn/rss/video/mon-ngon-350.rss"
                , R.drawable.food));

        RecyclerView categoryRecyclerView = findViewById(R.id.category_recycler_view);
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, categories);
        categoryRecyclerView.setAdapter(categoryAdapter);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    }

    // Initialize Firestore and Firebase authentication
    private void initServices() {
        new ProcessInBackground().execute();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        // Init user email
        if ((mAuth.getCurrentUser() != null)) {
            userEmail = mAuth.getCurrentUser().getEmail();
            loadSavedFeedItems(false);

            signInButton.setVisibility(View.GONE);

            return;
        }

        userEmail = "";

        // Set visibility
        menuButton.setVisibility(View.GONE);
    }

    private void signIn() {
        Log.d(TAG, "signing In");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
                Toast.makeText(this, "Successfully logged in", Toast.LENGTH_SHORT).show();

                userEmail = account.getEmail();
                loadSavedFeedItems(false);

                // Set visibility
                signInButton.setVisibility(View.GONE);
                menuButton.setVisibility(View.VISIBLE);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Log in failed", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        updateUI(null);
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Sign in services
        initServices();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            signInButton.setVisibility(View.GONE);

            // Set visibility
            menuButton.setVisibility(View.VISIBLE);
        } else {
            // Set visibility
            menuButton.setVisibility(View.GONE);

            signInButton.setVisibility(View.VISIBLE);
        }
    }

    private void loadSavedFeedItems(boolean isRedirected) {
        savedItems.clear();
        db.collection(userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int i = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            i++;
//                            Log.d(TAG, document.getId() + " => " + document.getData());
                            savedItems.add(document.toObject(FeedItem.class));
//                            Log.d(TAG, "i=" + i);

                            if (i == task.getResult().size())
                                lastSavedItemIndex = Integer.parseInt(document.getId());
                        }

//                        for (FeedItem item :
//                                savedItems) {
//                            Log.d(TAG, item.toString());
//                        }

                        if (isRedirected)
                            startSavedItemsActivity();

                        initRecyclerView(MainActivity.this, feedItems);

                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startSavedItemsActivity() {
        Intent intent = new Intent(this, SavedItemsActivity.class);
        intent.putParcelableArrayListExtra("items", savedItems);
        intent.putExtra("email", userEmail);
        // Adds the FLAG_ACTIVITY_NO_HISTORY flag
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void handleSavedListButtonClick() {
        loadSavedFeedItems(true);
    }

    private void handleSignOutButtonClick() {
        // Firebase sign out
        mAuth.signOut();

        // Google signout
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(getBaseContext(), "You have signed out", Toast.LENGTH_SHORT).show();
            updateUI(null);
        });
    }

    @Override
    public void onSavedItemListener(Intent intent) {
        FeedItem savedItem = intent.getParcelableExtra("savedItem");
        saveFeedItemToFirestore(savedItem);
    }

    @Override
    public void onUnsavedItemListener(Intent intent) {
        FeedItem unsavedItem = intent.getParcelableExtra("unsavedItem");
//        Log.d(TAG, "unsavedItem=" + unsavedItem);
        unsaveFeedItemFromFirestore(unsavedItem);
    }

    private void saveFeedItemToFirestore(FeedItem savedItem) {
        try {
            if (!userEmail.equals("")) {
                Map<String, Object> data = new HashMap<>();
                data.put("title", savedItem.getTitle());
                data.put("description", savedItem.getDescription());
                data.put("link", savedItem.getLink());
                data.put("pubDate", savedItem.getPubDate());

                db.collection(userEmail).document((lastSavedItemIndex + 1 + ""))
                        .set(data)
                        .addOnSuccessListener(documentReference -> {
//                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            Toast.makeText(getBaseContext(), "Saved item " + savedItem.getTitle() + " " + savedItem.getLink(), Toast.LENGTH_SHORT).show();

                            loadSavedFeedItems(false);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error adding document", e);
                            Toast.makeText(getBaseContext(), "Cannot save item", Toast.LENGTH_SHORT).show();
                        });
            } else {
                signIn();
            }
        } catch (NullPointerException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private void unsaveFeedItemFromFirestore(FeedItem unsavedItem) {
        CollectionReference itemsRef = db.collection(userEmail);
        Query query = itemsRef.whereEqualTo("link", unsavedItem.getLink());
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
//                    Log.d(TAG, "here" + document.toString());
                    itemsRef.document(document.getId()).delete();
                }
                Log.d(TAG, "Item successfully deleted!");
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void handleQuerySubmit(String query) {
        urlString = query;
        new ProcessInBackground().execute();
    }

    @Override
    public void onCategoryClickListener(Intent intent) {
        urlString = intent.getStringExtra("categoryUrl");
        categoryString = intent.getStringExtra("categoryTitle");
        categoryText.setText(categoryString.toUpperCase(Locale.ROOT));

        new ProcessInBackground().execute();
    }

    // Async task to retrieve RSS feed
    public class ProcessInBackground extends AsyncTask<Integer, Void, Exception> {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading feed....");
            progressDialog.show();
        }

        @Override
        protected Exception doInBackground(Integer... integers) {
//            Log.d(TAG, "getRSSFeed=" + urlString);

            return getRSSFeed(urlString);
        }

        protected Exception getRSSFeed(String urlString) {
            try {
                feedItems.clear();
                URL url = new URL(urlString);

//                Log.d(TAG, "getRSSFeed=" + urlString);

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

                // Don't provide support for xml namespace
                factory.setNamespaceAware(false);

                XmlPullParser xpp = factory.newPullParser();

                // encoding = UTF_8
                xpp.setInput(getInputStream(url), "UTF_8");

                boolean insideItem = false;

                // Which tag/where we are in the document
                int eventType = xpp.getEventType();

                String title = "";
                String description = "";
                String link = "";
                String pubDate = "";

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            insideItem = true;
                        } else if (xpp.getName().equalsIgnoreCase("title")) {
                            if (insideItem) {
                                title = xpp.nextText();
                                titles.add(title);
                            }
                        } else if (xpp.getName().equalsIgnoreCase("description")) {
                            if (insideItem) {
                                description = xpp.nextText();
                                descriptions.add(description);
                            }
                        } else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                            if (insideItem) {
                                pubDate = xpp.nextText();
                                pubDates.add(pubDate);
                            }
                        } else if (xpp.getName().equalsIgnoreCase("link")) {
                            if (insideItem) {
                                link = xpp.nextText();
                                links.add(link);
                                feedItems.add(new FeedItem(title, description, link, pubDate));
                            }
                        }

//                        loadSavedFeedItems(false);
                    } else if (eventType == XmlPullParser.END_DOCUMENT && xpp.getName().equalsIgnoreCase("item")) {
                        insideItem = false;
                    }

                    eventType = xpp.next();
                }
            } catch (MalformedURLException e) {
                exception = e;
            } catch (XmlPullParserException e) {
                exception = e;
            } catch (IOException e) {
                exception = e;
            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);

            if (userEmail.isEmpty())
                initRecyclerView(MainActivity.this, feedItems);
            else loadSavedFeedItems(false);

            progressDialog.dismiss();
        }
    }

    private void initRecyclerView(Context context, ArrayList<FeedItem> itemList) {
        feedItemAdapter = new FeedItemAdapter(context, itemList, savedItems, false);
        recyclerView.setAdapter(feedItemAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }
}