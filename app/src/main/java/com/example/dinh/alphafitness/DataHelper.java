package com.example.dinh.alphafitness;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by chuandinh on 10/21/16.
 */

public class DataHelper {

    /**
     * Calculate calories from weight and number of steps
     * https://www.verywell.com/pedometer-steps-to-calories-converter-3882595
     * @param weight
     * @param steps
     * @return
     */
    public static int getCalories(int weight, int steps)
    {
        return (int)(0.00977555*(double)weight*(double)steps);
    }

    /**
     * Calculate distance in miles from number of steps using average running speed 5 miles/hour
     * https://www.verywell.com/pedometer-steps-to-calories-converter-3882595
     * @param steps
     * @return
     */
    public static float getDistance(int steps)
    {
        return (float)steps / 1950;
    }

    /**
     * Return time as string from miliseconds
     *
     * @param milliSeconds
     * @param dateFormat
     * @return
     */
    public static String getTime(long milliSeconds, String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();

        int offset = calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
        calendar.setTimeInMillis(milliSeconds + offset);

        return formatter.format(calendar.getTime());
    }

    /**
     * Check if the app running in simulator or not
     * @return true or false
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }


    public static String formatInterval(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));

        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    public static String formatIntervalMinutes(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));

        return String.format("%02d:%02d", min, sec);
    }

    public static String formatIntervalFull(final long l) {
        final long day = TimeUnit.MILLISECONDS.toDays(l);

        final long hr  = TimeUnit.MILLISECONDS.toHours(l - TimeUnit.DAYS.toMillis(day));
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms  = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));

        String s = "";
        if(sec > 0 || min > 0 || hr > 0 || day > 0) s = String.format("%d sec", sec);
        if(min > 0 || hr > 0 || day > 0) s = String.format("%d min ", min) + s;
        if(hr > 0 || day > 0) s = String.format("%d hr ", hr) + s;
        if(day > 0) s = String.format("%d day ", day) + s;

        return s;
    }
}


/*
Walking 20 minutes per mile (3 miles per hour): 2250 steps per mile
Walking 15 minutes per mile (4 miles per hour): 1950 steps per mile
Running 12 minutes per mile (5 miles per hour): 1950 steps per mile
Running 10 minutes per mile (6 miles per hour): 1700 steps per mile
Running 8 minutes per mile (7.5 miles per hour): 1400 steps per mile
*/