package com.bsq.bsquared;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.Objects;

public class JoinEvent extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;

    EditText textsend_join;
    ChatAdapter chatadapter;

    private Integer msg_count = 0;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    final CollectionReference eventref = db.collection("events");

    Boolean is_host = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences get_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
        final String is_host_string = get_pref.getString("created_event_id", "NULL");

        SharedPreferences get_pref2 = getSharedPreferences("username", Context.MODE_PRIVATE);
        final String username = get_pref2.getString("username", "");

        final Intent intent = getIntent();
        final String event_id = intent.getStringExtra("event_id");

        final String loc_in = intent.getStringExtra("loc");
        final String desc = intent.getStringExtra("desc");

        final String event_id_use;
        //if event_id is null, you came from the navbar. if it isn't null, you clicked on the event in eventscreen
        event_id_use = event_id == null ? is_host_string : event_id;

        //
        //
        // begin activity code
        //
        //

        //did not create or join event in eventscreen
        if (event_id == null && is_host_string.equals("NULL")) {
            setContentView(R.layout.nav_drawer_did_not);
            show_navbar();
        }
        //is joiner
        else if (event_id != null && !is_host_string.equals(event_id_use)) {
            setContentView(R.layout.nav_drawer_join_event);
            show_chatroom(is_host, username, event_id_use);
            show_navbar();

            //listener for host closing event/automatic deletion so everyone redirects to eventscreen
            eventref.document(event_id_use).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (value.getBoolean("deleted") != null) {
                        Boolean deleted = value.getBoolean("deleted");
                        if (deleted) {
                            //if deleted automatically, reset local event_id
                            if (is_host_string.equals(event_id_use)) {
                                SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("created_event_id", "NULL");
                                editor.apply();
                            }

                            //if you're on this activity, otherwise stay where you are
                            //server side function handles automatic deletion, so no need to redirect intent
                            if (active) {
                                Intent intent = new Intent(JoinEvent.this, EventScreen.class);
                                intent.putExtra("event_delete", event_id_use);
                                startActivity(intent);
                            }
                        }
                    }
                }
            });

            //set layout and set event details
            RelativeLayout rl = (RelativeLayout) findViewById(R.id.joiner_view);
            rl.setVisibility(View.VISIBLE);

            String approx_loc_input = get_approx_loc(loc_in);
            TextView approx_loc = (TextView) findViewById(R.id.approx_loc_input);
            approx_loc.setText(approx_loc_input);

            TextView desc_view = (TextView) findViewById(R.id.desc_input);
            desc_view.setText(desc);

            final TextView current_ppl = (TextView) findViewById(R.id.num_ppl_input_joiner);
            eventref.document(event_id_use).addSnapshotListener(JoinEvent.this, new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (value.get("num_ppl") != null) {
                        String num_ppl = value.get("num_ppl").toString();
                        if (num_ppl.equals("1")) {
                            String input = num_ppl + " person so far";
                            current_ppl.setText(input);
                        } else {
                            String input = num_ppl + " people so far";
                            current_ppl.setText(input);
                        }
                    }
                }
            });

            //join button has been clicked
            Button join_btn = (Button) findViewById(R.id.join_btn);
            join_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences get_pref = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                    String joined_event_id = get_pref.getString("joined_event_id", "");

                    //cannot have already joined event or hosted event
                    if (joined_event_id.equals("") && is_host_string.equals("NULL")) {
                        InterstitialAd ad = AdManager.getAd();
                        ad.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                //begin loading new ad already lol
                                AdManager admanager = new AdManager(JoinEvent.this);

                                //save joined event id
                                SharedPreferences shared_pref3 = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor3 = shared_pref3.edit();
                                editor3.putString("joined_event_id", event_id_use);
                                editor3.apply();

                                //save event location
                                SharedPreferences shared_pref2 = getSharedPreferences("joined_event_loc", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor2 = shared_pref2.edit();
                                editor2.putString("joined_event_loc", loc_in);
                                editor2.apply();

                                eventref.document(event_id_use).update("num_ppl", FieldValue.increment(1));

                                Intent intent = new Intent(JoinEvent.this, HasJoined.class);
                                startActivity(intent);
                            }
                            //bad user connection probably
                            @Override
                            public void onAdFailedToLoad(LoadAdError adError) {
                                //save event location
                                SharedPreferences shared_pref2 = getSharedPreferences("joined_event_loc", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor2 = shared_pref2.edit();
                                editor2.putString("joined_event_loc", loc_in);
                                editor2.apply();

                                //save joined event id
                                SharedPreferences shared_pref3 = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor3 = shared_pref3.edit();
                                editor3.putString("joined_event_id", event_id_use);
                                editor3.apply();

                                eventref.document(event_id_use).update("num_ppl", FieldValue.increment(1));

                                Intent intent = new Intent(JoinEvent.this, HasJoined.class);
                                startActivity(intent);
                            }
                        });
                        ad.show();
                    }
                    else {
                        Toast.makeText(JoinEvent.this, "You can only join or create one event at a time!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        //is host
        else {
            is_host = true;
            setContentView(R.layout.nav_drawer_join_event);
            show_navbar();

            //check if host has an event hosted on database
            //check if event still exists
            eventref.whereEqualTo(FieldPath.documentId(), is_host_string)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    //clear event_id
                                    SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = shared_pref.edit();
                                    editor.putString("created_event_id", "NULL");
                                    editor.apply();

                                    setContentView(R.layout.nav_drawer_did_not);
                                    show_navbar();
                                }
                                else {
                                    show_chatroom(is_host, username, event_id_use);
                                    run_activity(is_host_string, event_id_use);
                                }
                            }
                        }
                    });
        }
    }

    void show_navbar() {
        //
        //
        // navbar code
        //
        //

        dl = (DrawerLayout) findViewById(R.id.drawer_id);
        t = new ActionBarDrawerToggle(JoinEvent.this, dl, R.string.Open, R.string.Close);

        dl.addDrawerListener(t);
        t.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        nv = (NavigationView) findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.chatroom:
                        Intent intent = new Intent(JoinEvent.this, Chatroom.class);
                        startActivity(intent);
                        break;
                    case R.id.events:
                        Intent intent2 = new Intent(JoinEvent.this, EventScreen.class);
                        startActivity(intent2);
                        break;
                    case R.id.add_event:
                        Intent intent3 = new Intent(JoinEvent.this, CreateEvent.class);
                        startActivity(intent3);
                        break;
                    case R.id.hosted_event:
                        Intent intent4 = new Intent(JoinEvent.this, JoinEvent.class);
                        startActivity(intent4);
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });
    }

    void show_chatroom(Boolean is_host, final String username, final String event_id_use) {
        //
        //
        // chatroom code
        //
        //

        final Handler handler = new Handler();
        //check for bot behavior
        handler.postDelayed(new Runnable() {
            public void run() {
                if (msg_count > 0) {
                    --msg_count;
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);

        textsend_join = findViewById(R.id.textsend_join);
        final Boolean finalIs_host = is_host;
        findViewById(R.id.send_join).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (msg_count <= 3) {
                            Chat chat = new Chat(username, textsend_join.getText().toString(), finalIs_host, new Date());
                            eventref.document(event_id_use).collection("eventChat").add(chat);
                            textsend_join.setText("");

                            ++msg_count;
                        } else {
                            new AlertDialog.Builder(JoinEvent.this)
                                    .setTitle("Spam activity detected")
                                    .setMessage("Please limit message send rate")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(JoinEvent.this, EventScreen.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                    }
                });

        recyclerView = findViewById(R.id.joinrecyclerview);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        FirebaseFirestore.getInstance().collection("events").whereEqualTo(FieldPath.documentId(), event_id_use)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Query query = null;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                query = document.getReference().collection("eventChat").orderBy("timestamp", Query.Direction.ASCENDING);
                            }
                            FirestoreRecyclerOptions<Chat> options = new FirestoreRecyclerOptions.Builder<Chat>()
                                    .setQuery(query, Chat.class)
                                    .setLifecycleOwner(JoinEvent.this)
                                    .build();

                            chatadapter = new ChatAdapter(options);

                            chatadapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                                @Override
                                public void onItemRangeInserted(int positionStart, int itemCount) {
                                    recyclerView.scrollToPosition(chatadapter.getItemCount() - 1);
                                }
                            });
                            recyclerView.setAdapter(chatadapter);
                        }
                    }
                });
    }

    void run_activity(final String is_host_string, final String event_id_use) {
        //listener for host closing event/automatic deletion so everyone redirects to eventscreen
        eventref.document(event_id_use).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value.getBoolean("deleted") != null) {
                    Boolean deleted = value.getBoolean("deleted");
                    if (deleted) {
                        //if deleted automatically, reset local event_id
                        if (is_host_string.equals(event_id_use)) {
                            SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = shared_pref.edit();
                            editor.putString("created_event_id", "NULL");
                            editor.apply();
                        }

                        //if you're on this activity, otherwise stay where you are
                        //server side function handles automatic deletion, so no need to redirect intent
                        if (active) {
                            Intent intent = new Intent(JoinEvent.this, EventScreen.class);
                            intent.putExtra("event_delete", event_id_use);
                            startActivity(intent);
                        }
                    }
                }
            }
        });

        //set layout
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.host_view);
        rl.setVisibility(View.VISIBLE);

        final TextView current_ppl = (TextView) findViewById(R.id.num_ppl_input_host);
        eventref.document(event_id_use).addSnapshotListener(JoinEvent.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value.get("num_ppl") != null) {
                    String num_ppl = value.get("num_ppl").toString();
                    if (num_ppl.equals("1")) {
                        String input = num_ppl + " person so far";
                        current_ppl.setText(input);
                    } else {
                        String input = num_ppl + " people so far";
                        current_ppl.setText(input);
                    }
                }
            }
        });

        //host has clicked close event
        Button close_btn = (Button) findViewById(R.id.close_event);
        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(JoinEvent.this)
                        .setTitle("Close event")
                        .setMessage("Are you sure you want to delete this event?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //reset local event_id
                                SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("created_event_id", "NULL");
                                editor.apply();

                                eventref.document(event_id_use).update("deleted", true);
                                //let listener start new activity and then let new activity eventscreen delete
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(t.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static boolean active = false;

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    public String get_approx_loc(String input) {
        StringBuilder sb = new StringBuilder(input);
        //remove the country
        for (int i = sb.length() - 1; i >= 0; --i) {
            if (sb.charAt(i) == ',') {
                //we're past the country
                sb.deleteCharAt(i);
                break;
            }
            sb.deleteCharAt(i);
        }
        //remove zipcode
        for (int i = sb.length() - 1; i >= 0; --i) {
            if (sb.charAt(i) == ' ') {
                //we're past the zip
                sb.deleteCharAt(i);
                break;
            }
            sb.deleteCharAt(i);
        }
        //get approx_location, which is at the end just now state and city
        StringBuilder approx_loc = new StringBuilder();
        int comma_count = 0;
        for (int i = sb.length() - 1; i >= 0; --i) {
            if (sb.charAt(i) == ',') {
                ++comma_count;
            }
            if (comma_count == 2) {
                break;
            }
            approx_loc.append(sb.charAt(i));
        }
        return approx_loc.reverse().toString();
    }
}