/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bsq.bsquared;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import com.google.firebase.firestore.GeoPoint;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.AudioPlayerDelegate;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.PositionIndicator;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * This class encapsulates the properties and functionality of the Map view.It also triggers a
 * turn-by-turn navigation from HERE Burnaby office to Langley BC.There is a sample voice skin
 * bundled within the SDK package to be used out-of-box, please refer to the Developer's guide for
 * the usage.
 */
class MapFragmentView {
    private AndroidXMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private Button m_naviControlButton;
    private Map m_map;
    private NavigationManager m_navigationManager;
    private GeoBoundingBox m_geoBoundingBox;
    private Route m_route;
    private boolean m_foregroundServiceStarted;
    private MapRoute mapRoute;

    MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
        initNaviControlButton();
        initVoicePackagesButton();

        Button leave_btn = (Button) m_activity.findViewById(R.id.leave_button);
        leave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //do you really want to leave?
                stopForegroundService();
                m_navigationManager.stop();
                new androidx.appcompat.app.AlertDialog.Builder(m_activity)
                        .setTitle("Leave event")
                        .setMessage("Are you sure you want to leave this event?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //clear joined_event flag
                                SharedPreferences shared_pref = m_activity.getSharedPreferences("joined_event_id", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared_pref.edit();
                                editor.putString("joined_event_id", "");
                                editor.apply();

                                //clear joined_event_location
                                SharedPreferences shared_pref2 = m_activity.getSharedPreferences("joined_event_loc", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor2 = shared_pref2.edit();
                                editor2.putString("joined_event_loc", "");
                                editor2.apply();

                                m_activity.startActivity(new Intent(m_activity, EventScreen.class));
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager()
                .findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();

        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        String path = new File(m_activity.getExternalFilesDir(null), ".here-map-data")
                .getAbsolutePath();
        // This method will throw IllegalArgumentException if provided path is not writable
        com.here.android.mpa.common.MapSettings.setDiskCacheRootPath(path);
        if (m_mapFragment != null) {
            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                    if (error == Error.NONE) {
                        //map SHOULD be initialized
                        m_map = m_mapFragment.getMap();
                        if (ContextCompat.checkSelfPermission(
                                m_activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED) {
                            // You can use the API that requires the permission.
                        } else {
                            // You can directly ask for the permission.
                            int REQUEST_CODE=1;


                        }
                        Geocoder geocoder = new Geocoder(m_activity.getApplicationContext(), Locale.getDefault());
                        boolean gps_enabled = false;
                        boolean network_enabled = false;
                        Context mContext = m_activity.getApplicationContext();
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
                        m_map.setCenter(new GeoCoordinate(latitude,longitude),
                                Map.Animation.NONE);
                        m_map.setZoomLevel(13.2);
                        /*
                         * Get the NavigationManager instance.It is responsible for providing voice
                         * and visual instructions while driving and walking
                         */
                        m_navigationManager = NavigationManager.getInstance();
                        PositionIndicator position=m_mapFragment.getPositionIndicator();
                        position.setVisible(true);

                        m_activity.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        m_activity.findViewById(R.id.naviCtrlButton).setVisibility(View.VISIBLE);

                    } else {
                        new AlertDialog.Builder(m_activity).setMessage(
                                "Error : " + error.name() + "\n\n" + error.getDetails())
                                .setTitle(R.string.engine_init_error)
                                .setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                m_activity.finish();
                                            }
                                        }).create().show();
                    }
                }
            });
        }
    }

    private void createRoute() {
        /* Initialize a CoreRouter */
        CoreRouter coreRouter = new CoreRouter();

        /* Initialize a RoutePlan */
        RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption. HERE Mobile SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(false);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);

                /* Define waypoints for the route */
        /* START: 4350 Still Creek Dr */

        //double longitude = finalLoc.getLongitude();
        if (ContextCompat.checkSelfPermission(
                m_activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
        } else {
            // You can directly ask for the permission.
            int REQUEST_CODE=1;


        }
        Geocoder geocoder = new Geocoder(m_activity.getApplicationContext(), Locale.getDefault());
        boolean gps_enabled = false;
        boolean network_enabled = false;
        Context mContext = m_activity.getApplicationContext();
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
       // double latitude = 40.356000;
       // double longitude=-74.200000;
        GeoPoint p1 = null;
        RouteWaypoint startPoint = new RouteWaypoint(new GeoCoordinate(latitude, longitude));
        /* END: Langley BC */
        try {
        SharedPreferences get_pref = m_activity.getSharedPreferences("joined_event_loc", Context.MODE_PRIVATE);
       String joined_event_loc = get_pref.getString("joined_event_loc", "");
            //String joined_event_loc="Restaurant Nicholas, 160 State Route 35, Red Bank, NJ 07701, United States";
        List<Address> address;


           address = geocoder.getFromLocationName(joined_event_loc,5);
            Address location=address.get(0);

            p1 = new GeoPoint((double) (location.getLatitude() ),
                   (double) (location.getLongitude() ));


        }catch(Exception e){
            Log.e("righthere", e+"");
        }
         double latFinal=p1.getLatitude(); //call to DB to get string
        double lonFinal=p1.getLongitude();
        //double latFinal=50.00000;
       // double lonFinal=-30.00000;
      //  double latFinal = 40.3572, lonFinal=-74.1158;

        //Put this call in Map.onTransformListener if the animation(Linear/Bow)
        //is used in setCenter()
        m_map.setZoomLevel(13.2);

        RouteWaypoint destination = new RouteWaypoint(new GeoCoordinate(latFinal, lonFinal));

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(startPoint);
        routePlan.addWaypoint(destination);


        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter
                .calculateRoute(routePlan, new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int i) {
                        /* The calculation progress can be retrieved in this callback. */
                    }

                    @Override
                    public void onCalculateRouteFinished(
                            List<RouteResult> routeResults,
                            RoutingError routingError) {
                        /* Calculation is done.Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            if (routeResults.get(0).getRoute() != null) {

                                m_route = routeResults.get(0).getRoute();
                                /* Create a MapRoute so that it can be placed on the map */
                                  mapRoute = new MapRoute(
                                        routeResults.get(0).getRoute());

                                /* Show the maneuver number on top of the route */
                                mapRoute.setManeuverNumberVisible(true);

                                /* Add the MapRoute to the map */
                                m_map.addMapObject(mapRoute);

                                /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                                m_geoBoundingBox = routeResults.get(0).getRoute()
                                        .getBoundingBox();
                                m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION);

                                startNavigation();
                            } else {
                                Toast.makeText(m_activity,
                                        "Error:route results returned is not valid",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(m_activity,
                                    "Error:route calculation returned error code: "
                                            + routingError,
                                    Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }

    private void initNaviControlButton() {
        m_naviControlButton = m_activity.findViewById(R.id.naviCtrlButton);
        m_naviControlButton.setText(R.string.start_navi);
        m_naviControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * To start a turn-by-turn navigation, a concrete route object is required.We use
                 * the same steps from Routing sample app to create a route from 4350 Still Creek Dr
                 * to Langley BC without going on HWY.
                 *
                 * The route calculation requires local map data.Unless there is pre-downloaded map
                 * data on device by utilizing MapLoader APIs,it's not recommended to trigger the
                 * route calculation immediately after the MapEngine is initialized.The
                 * INSUFFICIENT_MAP_DATA error code may be returned by CoreRouter in this case.
                 *
                 */

                m_activity.findViewById(R.id.be_there).setVisibility(View.GONE);
                m_activity.findViewById(R.id.textView10).setVisibility(View.GONE);
                m_activity.findViewById(R.id.textView11).setVisibility(View.GONE);
                m_activity.findViewById(R.id.bg_color).setVisibility(View.GONE);

                if (m_route == null) {
                    createRoute();
                } else {
                    m_navigationManager.stop();
                    /*
                     * Restore the map orientation to show entire route on screen
                     */
                    m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE, 0f);
                    m_naviControlButton.setText(R.string.start_navi);
                    m_route = null;
                }
            }
        });
    }

    private void initVoicePackagesButton() {
        Button m_voicePackagesButton = m_activity.findViewById(R.id.voiceCtrlButton);
        m_voicePackagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(m_activity, VoiceSkinsActivity.class);
                m_activity.startActivity(intent);
            }
        });
    }

    /*
     * Android 8.0 (API level 26) limits how frequently background apps can retrieve the user's
     * current location. Apps can receive location updates only a few times each hour.
     * See href="https://developer.android.com/about/versions/oreo/background-location-limits.html
     * In order to retrieve location updates more frequently start a foreground service.
     * See https://developer.android.com/guide/components/services.html#Foreground
     */
    private void startForegroundService() {
        if (!m_foregroundServiceStarted) {
            m_foregroundServiceStarted = true;
            Intent startIntent = new Intent(m_activity, ForegroundService.class);
            startIntent.setAction(ForegroundService.START_ACTION);
            m_activity.getApplicationContext().startService(startIntent);
        }
    }

    private void stopForegroundService() {
        if (m_foregroundServiceStarted) {
            m_foregroundServiceStarted = false;
            Intent stopIntent = new Intent(m_activity, ForegroundService.class);
            stopIntent.setAction(ForegroundService.STOP_ACTION);
            m_activity.getApplicationContext().startService(stopIntent);
        }
    }

    private void startNavigation() {
        m_naviControlButton.setText(R.string.stop_navi);
        /* Configure Navigation manager to launch navigation on current map */
        m_navigationManager.setMap(m_map);

        /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */

        m_navigationManager.startNavigation(m_route);
        m_map.setTilt(60);
        startForegroundService();

        /*
         * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
         * the current location.If user gestures are expected during the navigation, it's
         * recommended to set the map update mode to NONE first. Other supported update mode can be
         * found in HERE Mobile SDK for Android (Premium) API doc
         */
        m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

        /*
         * NavigationManager contains a number of listeners which we can use to monitor the
         * navigation status and getting relevant instructions.In this example, we will add 2
         * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
         */
        addNavigationListeners();
    }

    private void addNavigationListeners() {

        /*
         * Register a NavigationManagerEventListener to monitor the status change on
         * NavigationManager
         */
        m_navigationManager.addNavigationManagerEventListener(
                new WeakReference<>(m_navigationManagerEventListener));

        /* Register a PositionListener to monitor the position updates */
        m_navigationManager.addPositionListener(new WeakReference<>(m_positionListener));

        /* Register a AudioPlayerDelegate to monitor TTS text */
        m_navigationManager.getAudioPlayer().setDelegate(m_audioPlayerDelegate);
    }

    private NavigationManager.PositionListener m_positionListener =
            new NavigationManager.PositionListener() {
                @Override
                public void onPositionUpdated(GeoPosition geoPosition) {
                    /* Current position information can be retrieved in this callback */
                }
            };

    private NavigationManager.NavigationManagerEventListener m_navigationManagerEventListener =
            new NavigationManager.NavigationManagerEventListener() {
                @Override
                public void onRunningStateChanged() {
                  //  Toast.makeText(m_activity, "Running state changed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNavigationModeChanged() {
                    //Toast.makeText(m_activity, "Navigation mode changed", Toast.LENGTH_SHORT)
                     //       .show();
                }

                @Override
                public void onEnded(NavigationManager.NavigationMode navigationMode) {
//                    Toast.makeText(m_activity, navigationMode + " was ended", Toast.LENGTH_SHORT)
//                            .show();
                    stopForegroundService();
                }

                @Override
                public void onMapUpdateModeChanged(NavigationManager.MapUpdateMode mapUpdateMode) {
                   // Toast.makeText(m_activity, "Map update mode is changed to " + mapUpdateMode,
                   //         Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onRouteUpdated(Route route) {
                    Toast.makeText(m_activity, "Route updated", Toast.LENGTH_SHORT).show();
                        // remove old MapRoute object from the map
                        m_map.removeMapObject(mapRoute);
                        // create a new MapRoute object
                        mapRoute = new MapRoute(route);
                        // display new route on the map
                        m_map.addMapObject(mapRoute);
                    }



                @Override
                public void onCountryInfo(String s, String s1) {
                    Toast.makeText(m_activity, "Country info updated from " + s + " to " + s1,
                            Toast.LENGTH_SHORT).show();
                }
            };

    private AudioPlayerDelegate m_audioPlayerDelegate = new AudioPlayerDelegate() {
        @Override public boolean playText(final String s) {
            m_activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    Toast.makeText(m_activity, s, Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }

        @Override public boolean playFiles(String[] strings) {
            return false;
        }
    };

    void onDestroy() {
        /* Stop the navigation when app is destroyed */
        if (m_navigationManager != null) {
            stopForegroundService();
            m_navigationManager.stop();
        }
    }
}

