package com.bsq.bsquared;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.Objects;

public class CreateEvent extends AppCompatActivity {
    ImageView img_view;
    RadioGroup age_opt;
    AutoCompleteTextView loc_edit;
    EditText desc_edit;

    private NavigationView nv;
    private ActionBarDrawerToggle t;
    private DrawerLayout dl;

    String age_group_in = "";
    String loc_in = "";
    String desc_in = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences get_pref2 = getSharedPreferences("zipcode", Context.MODE_PRIVATE);
        final String zipcode = get_pref2.getString("zipcode", "");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference eventref = db.collection("events");

        //check if event already created
        SharedPreferences get_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
        String is_host_string = get_pref.getString("created_event_id", "NULL");

        SharedPreferences get_pref3 = getSharedPreferences("joined_event", Context.MODE_PRIVATE);
        final String joined_event = get_pref3.getString("joined_event", "");

        setContentView(R.layout.nav_drawer_create_event);

        //check if host has an event hosted on database
        //check if event still exists
        if (!is_host_string.equals("NULL")) {
            eventref.whereEqualTo(FieldPath.documentId(), is_host_string)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    new AlertDialog.Builder(CreateEvent.this)
                                            .setTitle("Whoopsies")
                                            .setMessage("You can only create or join one event at a time, sorry!")
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    Intent intent = new Intent(CreateEvent.this, EventScreen.class);
                                                    startActivity(intent);
                                                }
                                            })
                                            .show();
                                } else {
                                    SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = shared_pref.edit();
                                    editor.putString("created_event_id", "NULL");
                                    editor.apply();
                                }
                            }
                        }
                    });
        }
        else if (!joined_event.equals("")) {
            new AlertDialog.Builder(CreateEvent.this)
                    .setTitle("Whoopsies")
                    .setMessage("You can only create or join one event at a time, sorry!")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(CreateEvent.this, EventScreen.class);
                            startActivity(intent);
                        }
                    })
                    .show();
        }

        img_view = (ImageView) findViewById(R.id.add_event);

        //listener for radio button selection
        age_opt = (RadioGroup) findViewById(R.id.age_group);
        age_opt.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checked_btn = group.findViewById(checkedId);
                String raw_id = getResources().getResourceEntryName(checked_btn.getId());
                if (raw_id.equals("youth")) {
                    age_group_in = "youth";
                }
                else if (raw_id.equals("young_adult")) {
                    age_group_in = "young adults";
                }
                else if (raw_id.equals("adult")) {
                    age_group_in = "adults";
                }
                else if (raw_id.equals("all_ages")) {
                    age_group_in = "all ages";
                }
            }
        });

        //listener for desc input
        desc_edit = (EditText) findViewById(R.id.describe_event);
        desc_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                desc_in = desc_edit.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        //listener for location input
        loc_edit = (AutoCompleteTextView) findViewById(R.id.event_location);
        final ArrayAdapter<String> adapter = new LocAutoSuggestAdapter(CreateEvent.this, android.R.layout.simple_list_item_1);
        loc_edit.setAdapter(adapter);
        loc_edit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                loc_in = adapter.getItem(i);
                InputMethodManager inputManager = (InputMethodManager)CreateEvent.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(CreateEvent.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        loc_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                loc_in = "";
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        //add event button listener
        img_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (desc_in.equals("")) {
                    Toast.makeText(CreateEvent.this, "Please add a description.", Toast.LENGTH_LONG).show();
                } else if (loc_in.equals("")) {
                    Toast.makeText(CreateEvent.this, "Please select an address from the auto-completed addresses.", Toast.LENGTH_LONG).show();
                } else if (age_group_in.equals("")) {
                    Toast.makeText(CreateEvent.this, "Please select an age group.", Toast.LENGTH_LONG).show();
                }
                //all fields are populated
                else {
                    String use_id = eventref.document().getId();
                    Event event = new Event(desc_in, loc_in, age_group_in, use_id, zipcode, new Date());

                    eventref.document(use_id).set(event);

                    SharedPreferences shared_pref = getSharedPreferences("event_id", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = shared_pref.edit();
                    editor.putString("created_event_id", use_id);
                    editor.apply();

                    Intent myIntent = new Intent(CreateEvent.this, JoinEvent.class);
                    startActivity(myIntent);
                }
            }
        });

        //navbar
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
                        Intent intent = new Intent(CreateEvent.this, Chatroom.class);
                        startActivity(intent);
                        break;
                    case R.id.events:
                        Intent intent2 = new Intent(CreateEvent.this, EventScreen.class);
                        startActivity(intent2);
                        break;
                    case R.id.add_event:
                        Intent intent3 = new Intent(CreateEvent.this, CreateEvent.class);
                        startActivity(intent3);
                        break;
                    case R.id.hosted_event:
                        Intent intent4 = new Intent(CreateEvent.this, JoinEvent.class);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(t.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}