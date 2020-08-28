package com.bsq.bsquared;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class EventScreen extends AppCompatActivity {
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;

    EventAdapter eventadapter;

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences get_pref2 = getSharedPreferences("zipcode", Context.MODE_PRIVATE);
        final String zipcode = get_pref2.getString("zipcode", "");

        //if delete was requested, delete here so user does not see disappearing chat msgs
        Intent intent = getIntent();
        final String event_delete = intent.getStringExtra("event_delete");
        if (event_delete != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            final CollectionReference eventref = db.collection("events");

            //delete all chat messages
            eventref.document(event_delete).collection("eventChat")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    document.getReference().delete();
                                }
                                //delete document itself
                                eventref.document(event_delete).delete();
                            }
                        }
                    });
        }

        setContentView(R.layout.nav_drawer_eventscreen);

        //
        //
        // event database code
        //
        //

        recyclerView = findViewById(R.id.eventrecyclerview);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(EventScreen.this, DividerItemDecoration.VERTICAL));

        Query query = FirebaseFirestore.getInstance().collection("events")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .whereEqualTo("zipcode", zipcode);

        FirestoreRecyclerOptions<Event> options = new FirestoreRecyclerOptions.Builder<Event>().setQuery(query, Event.class).build();

        eventadapter = new EventAdapter(options);

        eventadapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.scrollToPosition(eventadapter.getItemCount() - 1);
            }
        });
        recyclerView.setAdapter(eventadapter);

        //
        //
        // nav bar code
        //
        //

        dl = (DrawerLayout)findViewById(R.id.drawer_id);
        t = new ActionBarDrawerToggle(this, dl, R.string.Open, R.string.Close);

        dl.addDrawerListener(t);
        t.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        nv = (NavigationView)findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id) {
                    case R.id.chatroom:
                        Intent intent = new Intent(EventScreen.this, Chatroom.class);
                        startActivity(intent);
                        break;
                    case R.id.events:
                        Intent intent2 = new Intent(EventScreen.this, EventScreen.class);
                        startActivity(intent2);
                        break;
                    case R.id.add_event:
                        Intent intent3 = new Intent(EventScreen.this, CreateEvent.class);
                        startActivity(intent3);
                        break;
                    case R.id.hosted_event:
                        Intent intent4 = new Intent(EventScreen.this, JoinEvent.class);
                        startActivity(intent4);
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });

        //button click
        ImageView imgview = (ImageView) findViewById(R.id.add_event_screen);
        imgview.bringToFront();
        imgview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(EventScreen.this, CreateEvent.class);
                startActivity(myIntent);
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

    @Override
    protected void onStart() {
        super.onStart();
        eventadapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        eventadapter.stopListening();
    }
}