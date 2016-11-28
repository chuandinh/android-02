package com.example.dinh.alphafitness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.ContentResolver;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Calendar;

public class LandscapeFragment extends Fragment {
    private static final String TAG = "LandscapeFragment";
    private LineChart mChart;
    int workoutId;
    long startTime = 0;
    int userWeight;

    //Charts
    ArrayList<Entry> stepValues = new ArrayList<Entry>();
    ArrayList<Entry> caloryValues = new ArrayList<Entry>();

    private int[] mColors = new int[] {
            ColorTemplate.VORDIPLOM_COLORS[0],
            ColorTemplate.VORDIPLOM_COLORS[1],
            ColorTemplate.VORDIPLOM_COLORS[4]
    };


    //Broadcast receiver
    LandscapeFragment.MyReceiver receiver;
    int lastStepCount = 0;

    public LandscapeFragment() {
        // Required empty public constructor
    }

    //UI Controls
    TextView textViewAvgValue;
    TextView textViewMaxValue;
    TextView textViewMinValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_landscape, container, false);

        //share preferences
        SharedPreferences sharedpreferences = view.getContext().getSharedPreferences(MainActivity.SHARE_PREFERENCES, Context.MODE_PRIVATE);
        userWeight = sharedpreferences.getInt(MainActivity.PREFERENCE_WEIGHT, 100);

        // UI Controls
        textViewAvgValue = (TextView)view.findViewById(R.id.textViewAvgValue);
        textViewMaxValue = (TextView)view.findViewById(R.id.textViewMaxValue);
        textViewMinValue = (TextView)view.findViewById(R.id.textViewMinValue);

        // Get data from ContentProvider
        ContentResolver cr = view.getContext().getContentResolver();

        Cursor workoutCursor = cr.query(WorkoutProvider.WORKOUT_CONTENT_URI, null, null, null, null);
        if(workoutCursor.getCount() > 0)
        {
            workoutCursor.moveToLast();
            workoutId = workoutCursor.getInt(workoutCursor.getColumnIndex(WorkoutProvider.WORKOUT_ID));
            startTime = workoutCursor.getInt(workoutCursor.getColumnIndex(WorkoutProvider.WORKOUT_START_TIME));

            Cursor c = cr.query(WorkoutProvider.DETAIL_CONTENT_URI, null, WorkoutProvider.DETAIL_WORKOUT_ID + " = ?", new String[]{ "" + workoutId }, null);

            if (c.moveToFirst()) {
                int i = 0;

                do {
                    String record = c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_ID))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_WORKOUT_ID))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_TIME))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_STEPCOUNT))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LATITUDE))
                            + ", " + c.getString(c.getColumnIndex(WorkoutProvider.DETAIL_LONGITUDE))
                            ;

                    Log.d(TAG, record);

                    int steps = c.getInt(c.getColumnIndex(WorkoutProvider.DETAIL_STEPCOUNT));

                    stepValues.add(new Entry(i, steps - lastStepCount));
                    caloryValues.add(new Entry(i, DataHelper.getCalories(userWeight, steps - lastStepCount)));

                    lastStepCount = steps;
                    i++;
                } while (c.moveToNext());
            }
        }


        mChart = (LineChart) view.findViewById(R.id.chart);
        //mChart.setOnChartValueSelectedListener(this);

        mChart.setDrawGridBackground(false);
        //mChart.getDescription().setEnabled(false);
        mChart.setDrawBorders(false);

        mChart.getAxisLeft().setEnabled(false);
        mChart.getAxisRight().setDrawAxisLine(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getXAxis().setDrawAxisLine(false);
        mChart.getXAxis().setDrawGridLines(false);

        // enable touch gestures
        //mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        Legend l = mChart.getLegend();
        //l.setEnabled(false);
        //l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);

        XAxis x = mChart.getXAxis();
        x.setEnabled(false);

        // no description text
        mChart.getDescription().setEnabled(false);

        redrawChart();

        //mChart.notifyDataSetChanged();

        //Receiver
        IntentFilter filter_stepUpdate = new IntentFilter(MainActivity.ACTION_BORADCAST_STEP_UPDATE);
        receiver = new LandscapeFragment.MyReceiver();
        view.getContext().registerReceiver(receiver, filter_stepUpdate);

        return view;
    }

    private void redrawChart() {
        mChart.resetTracking();

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();

        //Step Count
        LineDataSet d = new LineDataSet(stepValues, String.format("Steps per %d seconds", StepCounterService.RECORD_DURATION_SECOND));
        d.setLineWidth(2.5f);
        d.setCircleRadius(4f);

        int color = mColors[0 % mColors.length];
        d.setColor(color);
        //d.setCircleColor(color);
        d.setDrawCircles(false);

        d.setDrawValues(false);
        d.setDrawFilled(true);

        dataSets.add(d);

        //Calories Burnt
        LineDataSet d1 = new LineDataSet(caloryValues, "Calories Burnt");
        d1.setLineWidth(2.5f);
        d1.setCircleRadius(4f);

        int color1 = mColors[1 % mColors.length];
        d1.setColor(color1);
        //d1.setCircleColor(color);
        d1.setDrawCircles(false);

        d1.setDrawValues(false);
        d1.setDrawFilled(true);

        dataSets.add(d1);

        // make the first DataSet dashed
        //((LineDataSet) dataSets.get(0)).enableDashedLine(10, 10, 0);
        //((LineDataSet) dataSets.get(0)).setColors(ColorTemplate.VORDIPLOM_COLORS);
        //((LineDataSet) dataSets.get(0)).setCircleColors(ColorTemplate.VORDIPLOM_COLORS);


        LineData data = new LineData(dataSets);
        mChart.setData(data);
        mChart.invalidate();

        calculateStatistics();
    }


    private void calculateStatistics() {
        if(stepValues.size() == 0) return;

        int avg = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;

        int totalSteps = 0;

        for(Entry e : stepValues)
        {
            int steps = (int)e.getY();

            if(steps >= max) max = steps;
            if(steps <= min) min = steps;

            totalSteps += steps;
        }
        avg = totalSteps * 1000 / stepValues.size();

        long mseconds = Calendar.getInstance().getTimeInMillis();

        //int avgSpeed = (int)((double)(mseconds - startTime)/1000 / DataHelper.getDistance(totalSteps));
        int avgSpeed = (int)((double)StepCounterService.RECORD_DURATION_SECOND * 1000 * 1000/ DataHelper.getDistance(avg));
        int maxSpeed = (int)((double)StepCounterService.RECORD_DURATION_SECOND * 1000 / DataHelper.getDistance(max));
        int minSpeed = (int)((double)StepCounterService.RECORD_DURATION_SECOND * 1000 / DataHelper.getDistance(min));

        textViewAvgValue.setText(DataHelper.formatIntervalMinutes(avgSpeed));
        textViewMaxValue.setText(DataHelper.formatIntervalMinutes(maxSpeed));
        textViewMinValue.setText(DataHelper.formatIntervalMinutes(minSpeed));

        Log.d(TAG, String.format("Steps: %d, avg: %d, max=%d, min=%d", totalSteps, avg, max, min) );
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
    public void onDestroy() {
        super.onDestroy();

        getContext().unregisterReceiver(receiver);
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

                //Toast.makeText(context, "stepCount: " + steps, Toast.LENGTH_SHORT).show();

                stepValues.add(new Entry(stepValues.size(), steps - lastStepCount));
                caloryValues.add(new Entry(caloryValues.size(), DataHelper.getCalories(userWeight, steps - lastStepCount)));

                lastStepCount = steps;

                redrawChart();
            }
        }
    }
}
