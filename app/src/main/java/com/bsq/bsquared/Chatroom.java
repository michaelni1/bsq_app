package com.bsq.bsquared;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Chatroom extends AppCompatActivity {
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;

    EditText textsend;
    ChatAdapter chatadapter;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    private Integer msg_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences get_pref2 = getSharedPreferences("zipcode", Context.MODE_PRIVATE);
        final String zipcode = get_pref2.getString("zipcode", "");

        final CollectionReference chatref = db.collection(zipcode);

        setContentView(R.layout.nav_drawer_chatroom);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            View customView = getLayoutInflater().inflate(R.layout.act_bar_title, null);
            TextView customTitle = (TextView) customView.findViewById(R.id.actionbarTitle);
            String total = "Local to: " + zipcode + "; tap here to change!";
            customTitle.setText(total);

            customTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Chatroom.this);
                    builder.setTitle("Change zipcode");

                    final EditText input = new EditText(Chatroom.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String new_zip = input.getText().toString();

                            //get new lat and long for auto complete api
                            final Geocoder geocoder = new Geocoder(Chatroom.this);
                            try {
                                List<Address> addresses = geocoder.getFromLocationName(new_zip, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address address = addresses.get(0);

                                    //save longitude and latitude for use in create event/elsewhere maybe
                                    SharedPreferences shared_pref = getSharedPreferences("longitude", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = shared_pref.edit();
                                    editor.putString("longitude", String.valueOf(address.getLongitude()));
                                    editor.apply();

                                    SharedPreferences shared_pref2 = getSharedPreferences("latitude", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor2 = shared_pref2.edit();
                                    editor2.putString("latitude", String.valueOf(address.getLatitude()));
                                    editor2.apply();
                                }
                            } catch (IOException e) {
                                Log.e("ERROR",""+e);
                            }

                            SharedPreferences shared_pref2 = getSharedPreferences("zipcode", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor2 = shared_pref2.edit();
                            editor2.putString("zipcode", new_zip);
                            editor2.apply();

                            Intent intent = new Intent(Chatroom.this, Chatroom.class);
                            startActivity(intent);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
            });
            actionBar.setCustomView(customView);
        }

        //
        //
        // chatroom code
        //
        //

        SharedPreferences get_pref = getSharedPreferences("username", Context.MODE_PRIVATE);
        final String username = get_pref.getString("username", "");

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
        textsend = findViewById(R.id.textsend);
        findViewById(R.id.send).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (msg_count <= 3) {
                            Chat chat = new Chat(username, textsend.getText().toString(), false, new Date());
                            chatref.add(chat);
                            textsend.setText("");
                            ++msg_count;
                        }
                        else {
                            new AlertDialog.Builder(Chatroom.this)
                                    .setTitle("Spam activity detected")
                                    .setMessage("Please limit message send rate")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(Chatroom.this, EventScreen.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                    }
                });

        recyclerView = findViewById(R.id.chatrecyclerview);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        Query query = FirebaseFirestore.getInstance().collection(zipcode).orderBy("timestamp", Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<Chat> options = new FirestoreRecyclerOptions.Builder<Chat>().setQuery(query, Chat.class).build();

        chatadapter = new ChatAdapter(options);

        chatadapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.scrollToPosition(chatadapter.getItemCount() - 1);
            }
        });
        recyclerView.setAdapter(chatadapter);

        //
        //
        // navigation drawer code
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
                        Intent intent = new Intent(Chatroom.this, Chatroom.class);
                        startActivity(intent);
                        break;
                    case R.id.events:
                        Intent intent2 = new Intent(Chatroom.this, EventScreen.class);
                        startActivity(intent2);
                        break;
                    case R.id.add_event:
                        Intent intent3 = new Intent(Chatroom.this, CreateEvent.class);
                        startActivity(intent3);
                        break;
                    case R.id.hosted_event:
                        Intent intent4 = new Intent(Chatroom.this, JoinEvent.class);
                        startActivity(intent4);
                        break;
                    default:
                        return true;
                }
                return true;
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
        chatadapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        chatadapter.stopListening();
    }
}