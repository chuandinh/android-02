package com.example.dinh.alphafitness;

import android.*;
import android.Manifest;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String ACTION_BORADCAST_STEP_UPDATE = "com.example.dinh.alphafitness.broadcast.stepUpdate";
    public static final String ACTION_BORADCAST_LOCATION_UPDATE = "com.example.dinh.alphafitness.broadcast.stepLocation";

    public static final String ACTION_BORADCAST_STEP_UPDATE_PARAM_STEPS = "com.example.dinh.alphafitness.broadcast.stepUpdate.steps";
    public static final String ACTION_BORADCAST_STEP_UPDATE_PARAM_LATITUDE = "com.example.dinh.alphafitness.broadcast.stepUpdate.latitude";
    public static final String ACTION_BORADCAST_STEP_UPDATE_PARAM_LONGITUDE = "com.example.dinh.alphafitness.broadcast.stepUpdate.longitude";
    public static final String ACTION_BORADCAST_STEP_UPDATE_PARAM_DURATION = "com.example.dinh.alphafitness.broadcast.stepUpdate.duration";

    public static final String SHARE_PREFERENCES = "com.example.dinh.alphafitness.references" ;
    public static final String PREFERENCE_NAME = "name";
    public static final String PREFERENCE_GENDER = "gender";
    public static final String PREFERENCE_WEIGHT = "weight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration config = getResources().getConfiguration();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LandscapeFragment landscapeFragment = new LandscapeFragment();
            fragmentTransaction.replace(android.R.id.content, landscapeFragment);
        } else {
            PortraitFragment portraitFragment = new PortraitFragment();
            fragmentTransaction.replace(android.R.id.content, portraitFragment);
        }

        fragmentTransaction.commit();
    }

    /**
     * Save data to text files
     */
    public void saveData() {
        //Save data to file
        //File path = this.getFilesDir();
        File path = this.getExternalFilesDir(null);
        File file = new File(path, "my-file-name.txt");

        FileOutputStream stream = null;

        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            //stream.write("text-to-write".getBytes());
            // Retrieve student records
            String URL = WorkoutProvider.DETAIL_URI;
            Uri details = Uri.parse(URL);

            Cursor c = getContentResolver().query(details, null, null, null, null);

            if (c.moveToFirst()) {
                do {
                    /*
                    static final String DETAIL_ID = "_id";
                    static final String DETAIL_WORKOUT_ID = "workoutId";
                    static final String DETAIL_TIME = "recordTime";
                    static final String DETAIL_STEPCOUNT = "stepCount";
                    static final String DETAIL_LONGITUDE = "longitude";
                    static final String DETAIL_LATITUDE = "latitude";*/

                    String record = c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_ID))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_WORKOUT_ID))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_TIME))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_STEPCOUNT))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LATITUDE))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LONGITUDE))
                            + "\n\r";

                    Log.d(TAG, record);

                    //write to file
                    stream.write(record.getBytes());

                } while (c.moveToNext());
            }


            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();


    }
}
