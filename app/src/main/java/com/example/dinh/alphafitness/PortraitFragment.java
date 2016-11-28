package com.example.dinh.alphafitness;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class PortraitFragment extends Fragment {
    private static final String TAG = "PortraitFragment";

    public static int BACKGROUND_INTERVAL = 10 * 1000; //every 10 seconds

    int userWeight = 0;

    //Broadcast receiver
    MyReceiver receiver;

    //Google Map
    private static final int LOCATION_PERMISSION_ID = 101;
    MapView mMapView;
    private GoogleMap mMap;

    //Locations
    private int workoutId = 0;
    private ArrayList<LatLng> points = new ArrayList<>();

    private boolean isRunning = false;
    Button startStopWorkoutButton;

    //Timer
    Timer mTimer;
    long startTime = 0;
    private Handler mTimerHandler = new Handler();
    TextView textViewDuration;
    TextView textViewDistance;

    //Bound Service
    IMyAidlInterface service;
    MyServiceConnection connection;

    public PortraitFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_portrait, container, false);

        //share preferences
        SharedPreferences sharedpreferences = rootView.getContext().getSharedPreferences(MainActivity.SHARE_PREFERENCES, Context.MODE_PRIVATE);
        userWeight = sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100);

        // init StepCounter remote service
        initRemoteService();

        textViewDistance = (TextView) rootView.findViewById(R.id.textViewDistance);
        textViewDuration = (TextView) rootView.findViewById(R.id.textViewDuration);

        startStopWorkoutButton = (Button) rootView.findViewById(R.id.startStopWorkout);
        startStopWorkoutButton.setBackgroundColor(Color.GREEN);

        startStopWorkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (isRunning == false) {
                isRunning = true;
                startStopWorkoutButton.setText("Stop Workout");
                startStopWorkoutButton.setEnabled(false);

                try {
                    startStepCounterService();
                    service.startWorkout();

                    startStopWorkoutButton.setBackgroundColor(Color.RED);

                    points.clear();
                    redrawLine();

                    startTimer();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                startStopWorkoutButton.setEnabled(true);
            } else {
                isRunning = false;
                startStopWorkoutButton.setText("Start Workout");
                startStopWorkoutButton.setEnabled(false);

                //AlarmManager scheduler = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                //Intent intent = new Intent(mContext, StepCounterService.class );
                //PendingIntent scheduledIntent = PendingIntent.getService(mContext.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                //scheduler.cancel(scheduledIntent);


                try {
                    service.stopWorkout();
                    stopStepCounterService();

                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if(mTimer != null)
                    mTimer.cancel();

                startStopWorkoutButton.setBackgroundColor(Color.GREEN);
                startStopWorkoutButton.setEnabled(true);
            }
            }
        });

        ImageButton profileButton = (ImageButton) rootView.findViewById(R.id.imageButtonProfile);
        profileButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });

        startStopWorkoutButton.setEnabled(false);


        // Get data from ContentProvider
        loadData(rootView);

        mMapView = (MapView) rootView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
            PortraitFragment.this.mMap = mMap;

            // For showing a move to my location button
            if (ContextCompat.checkSelfPermission(PortraitFragment.this.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) PortraitFragment.this.getContext(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
                return;
            }

            //mMap.setMyLocationEnabled(true);
            /*
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {

                    PortraitFragment.this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 15));
                }
            });
            */

            // For dropping a marker at a point on the Map
            //LatLng sydney = new LatLng(-34, 151);
            //LatLng sydney = new LatLng(37.270423, -121.841553);
            //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));

            // For zooming automatically to the location of the marker
            //CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(16).build();
            //mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            //points.add(new LatLng(37.270423, -121.841553));
            //points.add(new LatLng(37.270423, -121.861553));

            redrawLine();

            }
        });


        //Receiver
        IntentFilter filter_stepUpdate = new IntentFilter(MainActivity.ACTION_BORADCAST_STEP_UPDATE);
        receiver = new MyReceiver();
        rootView.getContext().registerReceiver(receiver, filter_stepUpdate);

        return rootView;
    }

    /**
     * Load last locations from last Workout (ContentProvider)
     * @param rootView view
     */
    private void loadData(View rootView) {
        ContentResolver cr = rootView.getContext().getContentResolver();

        Cursor workoutCursor = cr.query(WorkoutProvider.WORKOUT_CONTENT_URI, null, null, null, null);
        if(workoutCursor.getCount() > 0)
        {
            workoutCursor.moveToLast();
            workoutId = workoutCursor.getInt(workoutCursor.getColumnIndex(WorkoutProvider.WORKOUT_ID));
            long time = workoutCursor.getLong(workoutCursor.getColumnIndex(WorkoutProvider.WORKOUT_TIME));
            float distance = workoutCursor.getFloat(workoutCursor.getColumnIndex(WorkoutProvider.WORKOUT_DISTANCE));

            Cursor c = cr.query(WorkoutProvider.DETAIL_CONTENT_URI, null, WorkoutProvider.DETAIL_WORKOUT_ID + " = ?", new String[]{ "" + workoutId }, null);

            if (c.moveToFirst()) {
                int i = 0;

                points.clear();

                do {
                    //String record = c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_ID))
                    //        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_WORKOUT_ID))
                    //        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_TIME))
                    //        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_STEPCOUNT))
                    //        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LATITUDE))
                    //        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LONGITUDE))
                    //        ;

                    //Log.d(TAG, record);

                    long  logTime = c.getLong(c.getColumnIndex(WorkoutProvider.DETAIL_TIME));
                    float latidue  = c.getFloat(c.getColumnIndex(WorkoutProvider.DETAIL_LATITUDE));
                    float longitude  = c.getFloat(c.getColumnIndex(WorkoutProvider.DETAIL_LONGITUDE));

                    points.add(new LatLng(latidue, longitude));

                    i++;
                } while (c.moveToNext());
            }


            String duration = DataHelper.formatInterval(time);
            textViewDuration.setText(duration);
            textViewDistance.setText(String.format("%.02f", distance));
        }
    }

    private void startTimer() {
        //Get the start time from the service
        if(service != null) {
            try {
                if (service.isRunning()) {
                    startTime = service.getStartTime();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        mTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run() {
                    if (isRunning) {
                        String duration = DataHelper.formatInterval(Calendar.getInstance().getTimeInMillis() - startTime);
                        textViewDuration.setText(duration);
                    }
                    }
                });
            }
        };

        mTimer.schedule(mTimerTask, 0, 500);
    }

    private void redrawLine() {
        //  if(status) {
        Log.e(TAG, "redrawLine call");

        try {
            mMap.clear();  //clears all Markers and Polylines
        } catch (Exception e) {
            e.printStackTrace();
        }
        PolylineOptions options = new PolylineOptions();

        //.width(5).color(Color.BLUE).geodesic(true);
           /* for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                options.add(point);
            }*/
        options.addAll(points);
        options.width(6);
        options.color(Color.BLUE);
        // Drawing polyline in the Google Map for the i-th route
        mMap.addPolyline(options);


        if(points.size() > 0) {
            LatLng currentLoc = points.get(points.size() - 1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLoc.latitude, currentLoc.longitude), 15));

            // For zooming automatically to the location of the marker
            //CameraPosition cameraPosition = new CameraPosition.Builder().target(currentLoc).zoom(16).build();
            //mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            //Draw Start position
            LatLng startPos = points.get(0);
            // Creating MarkerOptions
            MarkerOptions makers = new MarkerOptions();

            // Setting the position of the marker
            makers.position(new LatLng(startPos.latitude, startPos.longitude));

            makers.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            //options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            // Add new marker to the Google Map Android API V2
            mMap.addMarker(makers);

        }
        else
        {
            Location currentLoc = getLocation();
            if(currentLoc != null)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), 15));
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();

        releaseService();

        if(receiver != null)
            getContext().unregisterReceiver(receiver);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    /**
     * Binds this activity to the service.
     */
    private void initRemoteService() {
        connection = new MyServiceConnection();
        Intent i = new Intent();
        i.setClassName("com.example.dinh.alphafitness", StepCounterService.class.getName());

        boolean ret = getContext().bindService(i, connection, Context.BIND_AUTO_CREATE);

        //Log.d(TAG, "initService() bound with " + ret);
    }

    /**
     * Unbinds this activity from the service.
     */
    private void releaseService() {
        getContext().unbindService(connection);
        connection = null;
        Log.d(TAG, "releaseService() unbound.");
    }

    // Method to start the service
    public void startStepCounterService() throws RemoteException {
        if (service == null) {
            getContext().startService(new Intent(getContext(), StepCounterService.class));
        } else {
            if (!service.isServiceStarted())
                getContext().startService(new Intent(getContext(), StepCounterService.class));
        }
    }

    // Method to stop the service
    public void stopStepCounterService() {
        getContext().stopService(new Intent(getContext(), StepCounterService.class));
    }

    /**
     * This class represents the actual service connection. It casts the bound
     * stub implementation of the service to the AIDL interface.
     */
    class MyServiceConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder boundService) {
            service = IMyAidlInterface.Stub.asInterface((IBinder) boundService);
            Log.d(PortraitFragment.TAG, "onServiceConnected() connected");
            //Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_LONG).show();

            try {
                if(service.isRunning()) {
                    startStopWorkoutButton.setText("Stop Workout");
                    startStopWorkoutButton.setBackgroundColor(Color.RED);

                    isRunning = true;

                    startTimer();

                    int steps = service.getStepCount();
                    textViewDistance.setText(String.format("%.02f", DataHelper.getDistance(steps)));
                }
                else
                {
                    updateWorkoutData();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            startStopWorkoutButton.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
            Log.d(PortraitFragment.TAG, "onServiceDisconnected() disconnected");
            //Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }
    }


    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("", "OnReceive() called.");

            final String action = intent.getAction();

            if(MainActivity.ACTION_BORADCAST_STEP_UPDATE.equals(action)) {
                int steps = intent.getIntExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_STEPS, 0);
                double latitude = intent.getDoubleExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_LATITUDE, 0);
                double longitude = intent.getDoubleExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_LONGITUDE, 0);

                //Toast.makeText(context, "stepCount: " + steps, Toast.LENGTH_SHORT).show();
                points.add(new LatLng(latitude, longitude));
                redrawLine();

                textViewDistance.setText(String.format("%.02f", DataHelper.getDistance(steps)));

                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));

                Log.d(TAG, "lat: " + latitude + ", lng: " + longitude);
            }
        }
    }

    /**
     * Get current location
     * @return current location
     */
    public Location getLocation() {
        LocationManager locationManager = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            // Get last known location
            if (ContextCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(lastKnownLocation != null)
                    return lastKnownLocation;
                else
                    return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }

        return null;
    }


    /**
     * Re-calculate workout data
     */
    public void updateWorkoutData()
    {
        //Retrieving the record
        Cursor cursorWorkout = getContext().getContentResolver().query(WorkoutProvider.WORKOUT_CONTENT_URI, null, null, null, null);
        if (cursorWorkout.moveToFirst()) {
            do {
                int id = cursorWorkout.getInt(cursorWorkout.getColumnIndex(WorkoutProvider.WORKOUT_ID));
                long startTime = cursorWorkout.getLong(cursorWorkout.getColumnIndex(WorkoutProvider.WORKOUT_START_TIME));
                float distance = cursorWorkout.getFloat(cursorWorkout.getColumnIndex(WorkoutProvider.WORKOUT_DISTANCE));
                int calories = cursorWorkout.getInt(cursorWorkout.getColumnIndex(WorkoutProvider.WORKOUT_CALORIES));
                long time = cursorWorkout.getLong(cursorWorkout.getColumnIndex(WorkoutProvider.WORKOUT_TIME));

                Log.v(TAG, String.format("Id=%d, start=%d, time=%d", id, startTime, time));

                if(id != workoutId)
                {
                    if(distance == 0 || calories == 0)
                    {
                        // Get data from ContentProvider
                        ContentResolver cr = getContext().getContentResolver();
                        Cursor c = cr.query(WorkoutProvider.DETAIL_CONTENT_URI, null, WorkoutProvider.DETAIL_WORKOUT_ID + " = ?", new String[]{"" + id}, null);

                        if (c.moveToLast()) {
                            int steps = c.getInt(c.getColumnIndex(WorkoutProvider.DETAIL_STEPCOUNT));
                            long stopTime = c.getLong(c.getColumnIndex(WorkoutProvider.DETAIL_TIME));

                            distance = DataHelper.getDistance(steps);
                            calories = DataHelper.getCalories(userWeight, steps);

                            ContentValues updateValues = new ContentValues();

                            updateValues.put(WorkoutProvider.WORKOUT_TIME, stopTime - startTime);
                            updateValues.put(WorkoutProvider.WORKOUT_DISTANCE, distance);
                            updateValues.put(WorkoutProvider.WORKOUT_CALORIES, calories);

                            // testing
                            //if(id == 1) updateValues.put(WorkoutProvider.WORKOUT_TIME,  86500000);

                            Uri workoutUri = Uri.parse(WorkoutProvider.WORKOUT_URI + "/" + id);
                            getContext().getContentResolver().update(workoutUri, updateValues, null, null);

                            Log.v(TAG, String.format("Updated: Id=%d, start=%d, stop=%d, time=%d", id, startTime, stopTime, stopTime - startTime));
                        }
                    }
                }
            } while (cursorWorkout.moveToNext());
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
