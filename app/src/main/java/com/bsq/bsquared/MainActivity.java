///*
// * Copyright (c) 2011-2020 HERE Europe B.V.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.example.bsquared;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.widget.Toast;
//
//import com.example.bsquared.MapFragmentView;
//
///**
// * Main activity which launches map view and handles Android run-time requesting permission.
// */
//
//public class MainActivity extends AppCompatActivity {
//
//    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
//    private static final String[] RUNTIME_PERMISSIONS = {
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.INTERNET,
//            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.ACCESS_NETWORK_STATE
//    };
//
//    private MapFragmentView m_mapFragmentView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_has_joined);
//
//        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
//            setupMapFragmentView();
//        } else {
//            ActivityCompat
//                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
//        }
//    }
//
//    /**
//     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
//     * needs when the app is running.
//     */
//    private static boolean hasPermissions(Context context, String... permissions) {
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
//            for (String permission : permissions) {
//                if (ActivityCompat.checkSelfPermission(context, permission)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_CODE_ASK_PERMISSIONS: {
//                for (int index = 0; index < permissions.length; index++) {
//                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
//
//                        /*
//                         * If the user turned down the permission request in the past and chose the
//                         * Don't ask again option in the permission request system dialog.
//                         */
//                        if (!ActivityCompat
//                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
//                            Toast.makeText(this, "Required permission " + permissions[index]
//                                            + " not granted. "
//                                            + "Please go to settings and turn on for sample app",
//                                    Toast.LENGTH_LONG).show();
//                        } else {
//                            Toast.makeText(this, "Required permission " + permissions[index]
//                                    + " not granted", Toast.LENGTH_LONG).show();
//                        }
//                    }
//                }
//
//                setupMapFragmentView();
//                break;
//            }
//            default:
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//
//    private void setupMapFragmentView() {
//        // All permission requests are being handled. Create map fragment view. Please note
//        // the HERE Mobile SDK requires all permissions defined above to operate properly.
//        m_mapFragmentView = new MapFragmentView(this);
//    }
//
//    @Override
//    public void onDestroy() {
//        m_mapFragmentView.onDestroy();
//        super.onDestroy();
//    }
//}


package com.bsq.bsquared;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    String TAG = MainActivity.class.getSimpleName();
    Button btnverifyCaptcha;
    String SITE_KEY = "6LfYFr0ZAAAAAAxB-X0KotuWtVIT9lPL_uWa3Pmq";
    String SECRET_KEY = "6LfYFr0ZAAAAAH_MmgGO6v0mpUaubozu-aebM8jN";
    RequestQueue queue;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference adj_ref = db.collection("adjectives");
    CollectionReference ani_ref = db.collection("animals");
    CollectionReference user_ref = db.collection("usernames");

    final String[] username = new String[1];
    Button gen_user;
    Button auto_loc;
    String zipcode;

    Boolean created_user = false;
    Boolean auto_loc_bool = false;

    public String zip_from_string(String s){
        for (int x=0;x<s.length();x++){
            try{
                int curr=Integer.parseInt(s.substring(x,x+5));
                return s.substring(x,x+5);
            }catch(Exception e){
            }
        }
        return "00000";
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
        } else {
            // You can directly ask for the permission.
            int REQUEST_CODE=1;
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE);
        }

        SharedPreferences get_pref = getSharedPreferences("first_time", Context.MODE_PRIVATE);
        String first_time = get_pref.getString("first_time", "");

        SharedPreferences get_pref2 = getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
        String joined_event_id = get_pref2.getString("joined_event_id", "");

        //if not first time
        if (!first_time.equals("")) {
            //if already joined event
            if (!joined_event_id.equals("")) {
                Intent intent = new Intent(MainActivity.this, HasJoined.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(MainActivity.this, Chatroom.class);
                startActivity(intent);
            }
        }

        setContentView(R.layout.captcha);

        TextView textView = (TextView) findViewById(R.id.tas);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        auto_loc = (Button) findViewById(R.id.enter_zip);
        auto_loc.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                auto_loc.setVisibility(View.INVISIBLE);

                TextView zip_text = (TextView) findViewById(R.id.zip_text);
                zip_text.setVisibility(View.VISIBLE);
                try {
                    if (ContextCompat.checkSelfPermission(
                            MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                        // You can use the API that requires the permission.
                    } else {
                        // You can directly ask for the permission.
                        int REQUEST_CODE=1;
                        requestPermissions(
                                new String[] { Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_CODE);

                    }
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    boolean gps_enabled = false;
                    boolean network_enabled = false;
                    Context mContext = MainActivity.this;
                    LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

                    gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    Location net_loc = null, gps_loc = null, finalLoc = null;

                    if (gps_enabled)
                        gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (network_enabled)
                        net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    if (gps_loc != null && net_loc != null) {

                        //smaller the number more accurate result will
                        if (gps_loc.getAccuracy() > net_loc.getAccuracy())
                            finalLoc = net_loc;
                        else
                            finalLoc = gps_loc;

                        // I used this just to get an idea (if both avail, its upto you which you want to take as I've taken location with more accuracy)

                    } else {

                        if (gps_loc != null) {
                            finalLoc = gps_loc;
                        } else if (net_loc != null) {
                            finalLoc = net_loc;
                        }
                    }
                    double longitude = finalLoc.getLongitude();
                    double latitude = finalLoc.getLatitude();

                    //save longitude and latitude for use in create event/elsewhere maybe
                    SharedPreferences shared_pref = getSharedPreferences("longitude", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = shared_pref.edit();
                    editor.putString("longitude", String.valueOf(longitude));
                    editor.apply();

                    SharedPreferences shared_pref2 = getSharedPreferences("latitude", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor2 = shared_pref2.edit();
                    editor2.putString("latitude", String.valueOf(latitude));
                    editor2.apply();

                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    //zipcode=addresses.get(0).getPostalCode();
                    zipcode=addresses.get(0).getAddressLine(0);
                }catch(Exception e){
                    zipcode = e+"";
                    Log.e("ERROR",""+e);
                }

                String total = "You're set to " + zip_from_string(zipcode);
                zip_text.setText(total);
                findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);

                auto_loc_bool = true;
                if (created_user && auto_loc_bool) {
                    start_captcha();
                }
            }
        });

        gen_user = (Button) findViewById(R.id.gen_user);
        gen_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gen_user.setVisibility(View.INVISIBLE);
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

                final String[] rand_adj = new String[1];
                final String[] rand_ani = new String[1];

                final Random r = new Random();
                int adj_num = r.nextInt(1925);
                adj_ref.whereEqualTo("num", adj_num)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        rand_adj[0] = document.getString("adj");
                                    }
                                    int ani_num = r.nextInt(598);
                                    ani_ref.whereEqualTo("num", ani_num)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            rand_ani[0] = document.getString("ani");
                                                        }
                                                        username[0] = rand_adj[0] + rand_ani[0];

                                                        check_username();
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }
        });

        queue = Volley.newRequestQueue(getApplicationContext());
    }

    public void check_username() {
        user_ref.whereEqualTo("username", username[0])
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                Random r = new Random();
                                int add_on = r.nextInt(999999);
                                username[0] += add_on;
                                //recursive
                                check_username();
                            } else {
                                findViewById(R.id.loadingPanel).setVisibility(View.GONE);

                                //add username to database
                                Map<String, String> user_data = new HashMap<>();
                                user_data.put("username", username[0]);
                                user_ref.add(user_data);

                                //show username
                                TextView display = (TextView) findViewById(R.id.show_user);
                                String total = "Songs will be sung of... \n" + username[0];
                                display.setText(total);

                                //save username
                                SharedPreferences shared_pref = getSharedPreferences("username", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("username", username[0]);
                                editor.apply();

                                created_user = true;
                                if (created_user && auto_loc_bool) {
                                    start_captcha();
                                }
                            }
                        }
                    }
                });
    }

    public void start_captcha() {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        SafetyNet.getClient(this).verifyWithRecaptcha(SITE_KEY)
                .addOnSuccessListener(this, new OnSuccessListener<SafetyNetApi.RecaptchaTokenResponse>() {
                    @Override
                    public void onSuccess(SafetyNetApi.RecaptchaTokenResponse response) {
                        if (!response.getTokenResult().isEmpty()) {
                            handleSiteVerify(response.getTokenResult());
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            Log.d(TAG, "Error message: " +
                                    CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()));
                        } else {
                            Log.d(TAG, "Unknown type of error: " + e.getMessage());
                        }
                    }
                });
    }

    protected void handleSiteVerify(final String responseToken){
        String url = "https://www.google.com/recaptcha/api/siteverify";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getBoolean("success")) {
                                SharedPreferences shared_pref = getSharedPreferences("first_time", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("first_time", "false");
                                editor.apply();

                                SharedPreferences shared_pref2 = getSharedPreferences("zipcode", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor2 = shared_pref2.edit();
                                editor2.putString("zipcode", zip_from_string(zipcode));
                                editor2.apply();

                                Intent my_intent = new Intent(MainActivity.this, Chatroom.class);
                                startActivity(my_intent);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), String.valueOf(jsonObject.getString("error-codes")), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception ex) {
                            Log.d(TAG, "JSON exception: " + ex.getMessage());

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error message: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("secret", SECRET_KEY);
                params.put("response", responseToken);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                50000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }
    //disable back button
    @Override
    public void onBackPressed() {}
}
