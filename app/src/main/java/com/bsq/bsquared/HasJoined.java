package com.bsq.bsquared;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;


import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class HasJoined extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE

    };

    private MapFragmentView m_mapFragmentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_has_joined);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference eventref = db.collection("events");

        SharedPreferences get_pref2 = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
        final String joined_event_id = get_pref2.getString("joined_event_id", "");

        //check if event was automatically deleted by querying database
        eventref.whereEqualTo(FieldPath.documentId(), joined_event_id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                //reset joined event id
                                SharedPreferences shared_pref = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("joined_event_id", "");
                                editor.apply();

                                //reset event location
                                SharedPreferences shared_pref2 = getSharedPreferences("joined_event", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor2 = shared_pref2.edit();
                                editor2.putString("joined_event_loc", "");
                                editor2.apply();

                                new AlertDialog.Builder(HasJoined.this)
                                        .setTitle("We're sorry")
                                        .setMessage("The event has been closed by the host or deleted automatically after 24 hours")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(HasJoined.this, EventScreen.class);
                                                startActivity(intent);
                                            }
                                        })
                                        .show();
                            }
                        }
                    }
                });

        //listener for host closing event so everyone redirects to eventscreen
        eventref.document(joined_event_id).addSnapshotListener(HasJoined.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value.getBoolean("deleted") != null) {
                    Boolean deleted = value.getBoolean("deleted");
                    if (deleted) {
                        //reset joiner
                        SharedPreferences shared_pref = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = shared_pref.edit();
                        editor.putString("joined_event_id", "");
                        editor.apply();

                        //reset event location
                        SharedPreferences shared_pref2 = getSharedPreferences("joined_event", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor2 = shared_pref2.edit();
                        editor2.putString("joined_event_loc", "");
                        editor2.apply();

                        new AlertDialog.Builder(HasJoined.this)
                                .setTitle("We're sorry")
                                .setMessage("The event has been closed by the host or deleted automatically after 24 hours")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(HasJoined.this, EventScreen.class);
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    }
                }
            }
        });

        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            setupMapFragmentView();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
    //disable back button
    @Override
    public void onBackPressed() {}


    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                            + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                setupMapFragmentView();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setupMapFragmentView() {
        // All permission requests are being handled. Create map fragment view. Please note
        // the HERE Mobile SDK requires all permissions defined above to operate properly.
        m_mapFragmentView = new MapFragmentView(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}