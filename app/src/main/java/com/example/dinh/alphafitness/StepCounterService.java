package com.example.dinh.alphafitness;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;

public class StepCounterService extends Service implements SensorEventListener {

    IMyAidlInterface.Stub mBinder;

    //Notification barNotif;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    private double mLastX, mLastY, mLastZ;
    private int stepCount = 0;
    private int simulatedStepCount = 0;

    //Another step counter
    private int mLimit = 30;
    private float mLastValues[] = new float[3 * 2];
    private float mScale[] = new float[2];
    private float mYOffset;

    private float mLastDirections[] = new float[3 * 2];
    private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;

    //Service tracking
    private boolean isServiceStarted = false;
    private boolean isRunning = false;
    private boolean isLocationChanged = false;

    //Locaition Service
    private static final String TAG = "StepCounterService";
    private static final int LOCATION_PERMISSION_ID = 102;
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };


    //Content Provider
    public static final double STEPS_PER_SECOND = 2.166;  //2.708 for simulator
    public static final int RECORD_DURATION_SECOND = 5;  //every 5 seconds
    Uri workoutUri;
    int workoutId = 0;
    long workoutStartTime = 0;
    Location mLastLocation;
    long lastRecordTime = 0;

    //SharePreferences
    SharedPreferences sharedpreferences;
    int userWeight = 0;


    public StepCounterService() {
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        sharedpreferences = getSharedPreferences(MainActivity.SHARE_PREFERENCES, Context.MODE_PRIVATE);
        userWeight = sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100);

        mBinder = new IMyAidlInterface.Stub() {
            @Override
            public boolean isServiceStarted() throws RemoteException {
                Log.d(TAG, "isRunning called");

                return isRunning;
            }

            @Override
            public boolean isRunning() throws RemoteException {
                Log.d(TAG, "isRunning called");

                return isRunning;
            }

            @Override
            public int getStepCount() throws RemoteException {
                Log.d(TAG, "getStepCount called");

                if(DataHelper.isEmulator())
                    return simulatedStepCount; //simulator
                else
                    return stepCount;  //Real device
            }

            @Override
            public void startWorkout() throws RemoteException {
                Log.d(TAG, "startWorkout called");

                if(!isRunning) {

                    //to keep track if the location has changed since first start
                    isLocationChanged = false;

                    long mseconds = Calendar.getInstance().getTimeInMillis();

                    // Add a new record
                    ContentValues values = new ContentValues();

                    values.put(WorkoutProvider.WORKOUT_START_TIME, mseconds);
                    values.put(WorkoutProvider.WORKOUT_DISTANCE, 0);
                    values.put(WorkoutProvider.WORKOUT_TIME, 0);
                    values.put(WorkoutProvider.WORKOUT_CALORIES, 0);

                    Uri uri = getContentResolver().insert(
                            WorkoutProvider.WORKOUT_CONTENT_URI, values);

                    if(uri != null) {
                        stepCount = 0;
                        simulatedStepCount = 0;

                        isRunning = true;
                        workoutStartTime = mseconds;

                        workoutUri = uri;
                        workoutId = Integer.parseInt(uri.getLastPathSegment());

                        Log.d(TAG, "New workoutId: " + workoutId);

                        workoutStartTime = mseconds;
                    }
                }
            }

            @Override
            public void stopWorkout() throws RemoteException {
                Log.d(TAG, "stopWorkout called");

                if(isRunning) {
                    long mseconds = Calendar.getInstance().getTimeInMillis();
                    int steps = DataHelper.isEmulator() ? simulatedStepCount : stepCount;

                    float totalDistance = DataHelper.getDistance(steps);
                    int calories = DataHelper.getCalories(userWeight, steps);

                    ContentValues updateValues = new ContentValues();
                    updateValues.put(WorkoutProvider.WORKOUT_TIME, mseconds - workoutStartTime);
                    updateValues.put(WorkoutProvider.WORKOUT_DISTANCE, totalDistance);
                    updateValues.put(WorkoutProvider.WORKOUT_CALORIES, calories);

                    getContentResolver().update(workoutUri, updateValues, null, null);

                    //Test retriving the record
                    Cursor c = getContentResolver().query(workoutUri, null, null, null, null);
                    if (c.moveToFirst()) {

                        Log.d(TAG,
                                c.getString(c.getColumnIndex(WorkoutProvider.WORKOUT_ID))
                                        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.WORKOUT_TIME))
                                        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.WORKOUT_DISTANCE))
                                        + ", " + c.getString(c.getColumnIndex(WorkoutProvider.WORKOUT_CALORIES))
                        );
                    }

                    stepCount = 0;
                    simulatedStepCount = 0;
                    workoutStartTime = 0;

                    isRunning = false;
                    isLocationChanged = false;
                }
            }

            @Override
            public long getCurrentWorkoutId() throws RemoteException {
                Log.d(TAG, "getCurrentWorkoutId called");
                //stepCount = 0;
                return workoutId;
            }

            @Override
            public long getStartTime() throws RemoteException {
                Log.d(TAG, "getCurrentWorkoutId called");
                //stepCount = 0;
                return workoutStartTime;
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isServiceStarted) {
            //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Service Started");

            // Start sensor service to keep track step count
            startSensorService();

            // Start location service
            startLocationService();

            isServiceStarted = true;
        }

        startForegroundNotification();
        return START_STICKY;
    }

    private void startForegroundNotification() {
        Notification notification = new NotificationCompat.Builder(this)
                .setOngoing(false)
                .setSmallIcon(android.R.color.transparent)
                //.setSmallIcon(R.drawable.picture)
                .build();
        startForeground(101,  notification);
    }


    private void startSensorService() {
        // Initialize Accelerometer sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Sensor tracking
        int h = 480;
        mYOffset = h * 0.5f;
        mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onDestroy() {//here u should unregister sensor
        //Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        Log.e(TAG, "Service Stopped");

        if(isServiceStarted) {
            mSensorManager.unregisterListener(this);
            stopLocationService();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Toast.makeText(this, "Sensor changed", Toast.LENGTH_SHORT).show();

        int j = (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) ? 1 : 0;
        if (j == 0) {
            //Toast.makeText(this, "Sensor Magnetic changed", Toast.LENGTH_SHORT).show();

            float vSum = 0;
            for (int i = 0; i < 3; i++) {
                final float v = mYOffset + event.values[i] * mScale[j];
                vSum += v;
            }
            int k = 0;
            float v = vSum / 3;

            float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
            if (direction == -mLastDirections[k]) {
                // Direction changed
                int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                mLastExtremes[extType][k] = mLastValues[k];
                float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                if (diff > mLimit) {

                    boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                    boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                    boolean isNotContra = (mLastMatch != 1 - extType);

                    if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {

                        if(isRunning) {
                            stepCount = stepCount + 1;

                            //Log.d(TAG, "Steps: " + stepCount);
                            //Toast.makeText(StepCounterService.this, "" + stepsCount, Toast.LENGTH_LONG).show();

                            recordData();
                        }

                        mLastMatch = extType;
                    } else {
                        mLastMatch = -1;
                    }
                }
                mLastDiff[k] = diff;
            }
            mLastDirections[k] = direction;
            mLastValues[k] = v;
        }

        // stop the service
        //stopSelf();
    }

    /**
     * Record step count and location to the database
     */
    private void recordData()
    {
        // if location doesn't change, do not record yet.
        // if(!isLocationChanged) return;
        if(!isRunning) return;

        //Record the time
        long mseconds = Calendar.getInstance().getTimeInMillis();

        //Log.d(TAG, "Time: " +  mseconds + ", " + lastRecordTime);

        if (mseconds - lastRecordTime >= RECORD_DURATION_SECOND * 1000 && mLastLocation != null) {
            if (mLastLocation.getLatitude() != 0.0 && mLastLocation.getLongitude() != 0.0) {

                ContentValues values = new ContentValues();

                values.put(WorkoutProvider.DETAIL_WORKOUT_ID, workoutId);
                values.put(WorkoutProvider.DETAIL_TIME, mseconds);

                simulatedStepCount += (int)(STEPS_PER_SECOND * RECORD_DURATION_SECOND)
                        + new Random().nextInt((int)(STEPS_PER_SECOND * RECORD_DURATION_SECOND * 0.40));

                //Simulate number of steps in simulator
                if(DataHelper.isEmulator())
                    values.put(WorkoutProvider.DETAIL_STEPCOUNT, simulatedStepCount); //simulator
                else
                    values.put(WorkoutProvider.DETAIL_STEPCOUNT, stepCount);  //Real device

                values.put(WorkoutProvider.DETAIL_LATITUDE, mLastLocation.getLatitude());
                values.put(WorkoutProvider.DETAIL_LONGITUDE, mLastLocation.getLongitude());

                Uri uri = getContentResolver().insert(WorkoutProvider.DETAIL_CONTENT_URI, values);

                Log.d(TAG, "WorkoutId: " + workoutId + ", ms: " + mseconds + ", steps: " + values.get(WorkoutProvider.DETAIL_STEPCOUNT)
                        + ", latitude: " + mLastLocation.getLatitude()
                        + ", longitude: " + mLastLocation.getLongitude()
                        + ", Id=" + uri.getLastPathSegment());

                lastRecordTime = mseconds;


                //Send Broadcast
                Intent intent = new Intent();
                intent.setAction(MainActivity.ACTION_BORADCAST_STEP_UPDATE);
                intent.putExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_STEPS,
                        (int)values.get(WorkoutProvider.DETAIL_STEPCOUNT));

                intent.putExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_LATITUDE, mLastLocation.getLatitude());
                intent.putExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_LONGITUDE, mLastLocation.getLongitude());

                intent.putExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_DURATION, mseconds - workoutStartTime);

                sendBroadcast(intent);
            }
        }
    }

    private void startLocationService()
    {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);

        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

        // Get last known location
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(lastKnownLocation != null)
                mLastLocation = lastKnownLocation;
            else
                mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    private void stopLocationService()
    {
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mLocationManager.removeUpdates(mLocationListeners[i]);
                    }
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    /**
     * Location Listener
     */
    private class LocationListener implements android.location.LocationListener
    {
        //Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            isLocationChanged = true;
            /*
            if(isLocationChanged == false) {
                isLocationChanged = true;
                workoutStartTime = Calendar.getInstance().getTimeInMillis();

                stepCount = 0;
                simulatedStepCount = 0;
            }
            */

            recordData();
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

}
