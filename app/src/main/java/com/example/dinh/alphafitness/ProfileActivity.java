package com.example.dinh.alphafitness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;

public class ProfileActivity extends AppCompatActivity {
    final String TAG = "ProfileActivity";

    Button buttonEditProfile;
    TextView textViewName;
    TextView textViewGender;
    TextView textViewWeight;

    TextView textViewWeekDistance;
    TextView textViewWeekTime;
    TextView textViewWeekWorkouts;
    TextView textViewWeekCalories;

    TextView textViewAllDistance;
    TextView textViewAllTime;
    TextView textViewAllWorkouts;
    TextView textViewAllCalories;

    float totalDistance = 0;
    int totalWorkouts = 0;
    int totalCalories = 0;

    long firstWorkoutTime = 0;
    long lastWorkoutTime = 0;
    long totalWorkoutTime = 0;

    float currentDistance = 0;
    long currentTime = 0;
    int curentCalories = 0;

    int userWeight = 0;

    SharedPreferences sharedpreferences;

    //Broadcast receiver
    MyReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //share preferences
        sharedpreferences = getSharedPreferences(MainActivity.SHARE_PREFERENCES, Context.MODE_PRIVATE);
        userWeight = sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100);

        //UI controls
        textViewName = (TextView)findViewById(R.id.textViewName);
        textViewGender = (TextView)findViewById(R.id.textViewGender);
        textViewWeight = (TextView)findViewById(R.id.textViewWeight);

        textViewWeekDistance = (TextView)findViewById(R.id.textViewWeekDistance);
        textViewWeekTime = (TextView)findViewById(R.id.textViewWeekTime);
        textViewWeekWorkouts = (TextView)findViewById(R.id.textViewWeekWorkouts);
        textViewWeekCalories = (TextView)findViewById(R.id.textViewWeekCalories);

        textViewAllDistance = (TextView)findViewById(R.id.textViewAllDistance);
        textViewAllTime = (TextView)findViewById(R.id.textViewAllTime);
        textViewAllWorkouts = (TextView)findViewById(R.id.textViewAllWorkouts);
        textViewAllCalories = (TextView)findViewById(R.id.textViewAllCalories);

        reloadProfile();

        buttonEditProfile = (Button)findViewById(R.id.buttonEditProfile);

        buttonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(ProfileActivity.this);

                //text_entry is an Layout XML file containing two text field to display in alert dialog
                final View textEntryView = factory.inflate(R.layout.profile_edit, null);

                final EditText editTextName = (EditText)textEntryView.findViewById(R.id.editTextName);
                final RadioGroup radioGroupGender = (RadioGroup) textEntryView.findViewById(R.id.radioGender);
                final EditText editTextWeight = (EditText)textEntryView.findViewById(R.id.editTextWeight);

                final RadioButton radioMale = (RadioButton) textEntryView.findViewById(R.id.radioMale);
                final RadioButton radioFemale = (RadioButton) textEntryView.findViewById(R.id.radioFemale);

                editTextName.setText(sharedpreferences.getString(MainActivity.PREFERENCE_NAME, ""));
                if(getString(R.string.radio_male).compareTo(sharedpreferences.getString(MainActivity.PREFERENCE_GENDER, "Male") + "") == 0)
                {
                    radioMale.setChecked(true);
                }
                else
                {
                    radioFemale.setChecked(true);
                }
                editTextWeight.setText(sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100) + "");

                final AlertDialog.Builder alert = new AlertDialog.Builder(ProfileActivity.this);
                //alert.setIcon(R.drawable.icon);
                alert.setTitle("Edit Profile").setView(textEntryView)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // get selected radio button from radioGroup
                                int selectedId = radioGroupGender.getCheckedRadioButtonId();
                                RadioButton selectedRadio = (RadioButton) textEntryView.findViewById(selectedId);

                                String name = editTextName.getText().toString();
                                String gender = selectedRadio.getText().toString();
                                int weight = Integer.parseInt(editTextWeight.getText().toString());

                                SharedPreferences.Editor editor = sharedpreferences.edit();

                                editor.putString(MainActivity.PREFERENCE_NAME, name);
                                editor.putString(MainActivity.PREFERENCE_GENDER, gender);
                                editor.putInt(MainActivity.PREFERENCE_WEIGHT, weight);
                                editor.commit();

                                reloadProfile();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        });
                alert.show();
            }
        });


        //Retrieving the record
        Cursor c = getContentResolver().query(WorkoutProvider.WORKOUT_CONTENT_URI, null, null, null, null);
        if (c.moveToFirst()) {
            totalWorkouts = 1;
            totalDistance = 0;
            totalCalories = 0;
            totalWorkoutTime = 0;
            do {
                int workoutId = c.getInt(c.getColumnIndex(WorkoutProvider.WORKOUT_ID));
                long startTime = c.getLong(c.getColumnIndex(WorkoutProvider.WORKOUT_START_TIME));
                float distance = c.getFloat(c.getColumnIndex(WorkoutProvider.WORKOUT_DISTANCE));
                int calories = c.getInt(c.getColumnIndex(WorkoutProvider.WORKOUT_CALORIES));
                long time = c.getLong(c.getColumnIndex(WorkoutProvider.WORKOUT_TIME));

                Log.v(TAG, String.format("Id= %d, start=%d, time= %d", workoutId, startTime, time));

                if(firstWorkoutTime == 0) firstWorkoutTime = startTime;
                lastWorkoutTime = startTime + time;

                totalWorkoutTime += time;

                totalDistance += distance;
                totalCalories += calories;

                totalWorkouts++;
            } while (c.moveToNext());

            reloadStatistics();
        }

        //Receiver
        IntentFilter filter_stepUpdate = new IntentFilter(MainActivity.ACTION_BORADCAST_STEP_UPDATE);
        receiver = new MyReceiver();
        registerReceiver(receiver, filter_stepUpdate);
    }

    /**
     * Reload statistics from current data
     */
    private void reloadStatistics() {
        int numberOfDays = (int)(lastWorkoutTime - firstWorkoutTime + currentTime)/(24*60*1*60*1000);
        if(numberOfDays < 7) numberOfDays = 7;

        textViewWeekDistance.setText(String.format("%,.02f miles", (totalDistance + currentDistance)/numberOfDays*7));
        textViewWeekTime.setText(DataHelper.formatIntervalFull((totalWorkoutTime + currentTime)/numberOfDays*7));
        textViewWeekWorkouts.setText(String.format("%,d times", (int)Math.ceil((double)totalWorkouts/numberOfDays*7)));
        textViewWeekCalories.setText(String.format("%,d Cal", (totalCalories + curentCalories)/numberOfDays*7));

        textViewAllDistance.setText(String.format("%,.02f miles", totalDistance + currentDistance));
        textViewAllTime.setText(DataHelper.formatIntervalFull((totalWorkoutTime + currentTime)));
        textViewAllWorkouts.setText(String.format("%,d times", totalWorkouts));
        textViewAllCalories.setText(String.format("%,d Cal", totalCalories + curentCalories));
    }

    private void reloadProfile() {
        textViewName.setText(sharedpreferences.getString(MainActivity.PREFERENCE_NAME, "Your Name"));
        textViewGender.setText(sharedpreferences.getString(MainActivity.PREFERENCE_GENDER, "Male"));
        textViewWeight.setText(sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100) + " lbs");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    /**
     * BroadcastReceiver to receive step count change for live update
     */
    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("", "OnReceive() called.");

            final String action = intent.getAction();

            if(MainActivity.ACTION_BORADCAST_STEP_UPDATE.equals(action)) {
                int steps = intent.getIntExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_STEPS, 0);
                long durration = intent.getLongExtra(MainActivity.ACTION_BORADCAST_STEP_UPDATE_PARAM_DURATION, 0);

                currentTime = durration;
                currentDistance = DataHelper.getDistance(steps);
                curentCalories = DataHelper.getCalories(userWeight, steps);

                reloadStatistics();
            }
        }
    }



}
