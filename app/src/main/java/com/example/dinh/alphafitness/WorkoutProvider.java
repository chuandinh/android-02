package com.example.dinh.alphafitness;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public class WorkoutProvider extends ContentProvider {

    static final String PROVIDER_NAME = "com.example.dinh.myprovider";

    static final String WORKOUT_URI = "content://" + PROVIDER_NAME + "/workouts";
    static final Uri WORKOUT_CONTENT_URI = Uri.parse(WORKOUT_URI);

    static final String DETAIL_URI = "content://" + PROVIDER_NAME + "/details";
    static final Uri DETAIL_CONTENT_URI = Uri.parse(DETAIL_URI);


    static final String WORKOUT_ID = "_id";
    static final String WORKOUT_START_TIME = "startTime";
    static final String WORKOUT_DISTANCE = "distance";
    static final String WORKOUT_TIME = "time";
    static final String WORKOUT_CALORIES = "calories";

    static final String DETAIL_ID = "_id";
    static final String DETAIL_WORKOUT_ID = "workoutId";
    static final String DETAIL_TIME = "recordTime";
    static final String DETAIL_STEPCOUNT = "stepCount";
    static final String DETAIL_LATITUDE = "latitude";
    static final String DETAIL_LONGITUDE = "longitude";

    private static HashMap<String, String> WORKOUTS_PROJECTION_MAP;
    static final int URI_WORKOUTS = 1;
    static final int URI_WORKOUT_ID = 2;

    private static HashMap<String, String> DETAILS_PROJECTION_MAP;
    static final int URI_DETAILS = 3;
    static final int URI_DETAIL_ID = 4;

    static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(PROVIDER_NAME, "workouts", URI_WORKOUTS);
        uriMatcher.addURI(PROVIDER_NAME, "workouts/#", URI_WORKOUT_ID);

        uriMatcher.addURI(PROVIDER_NAME, "details", URI_DETAILS);
        uriMatcher.addURI(PROVIDER_NAME, "details/#", URI_DETAIL_ID);
    }


    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    static final String DATABASE_NAME = "AlphaFitness";
    static final String WORKOUT_TABLE_NAME = "workout";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE_WORKOUT =
            " CREATE TABLE " + WORKOUT_TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " startTime BIGINT NOT NULL, " +
                    " distance FLOAT NOT NULL, " +
                    " time BIGINT NOT NULL, " +
                    " calories INTEGER NOT NULL);";

    static final String DETAIL_TABLE_NAME = "detail";
    static final String CREATE_DB_TABLE_DETAIL =
            " CREATE TABLE " + DETAIL_TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " workoutId INTEGER NOT NULL, " +
                    " recordTime BIGINT NOT NULL, " +
                    " stepCount INTEGER NOT NULL, " +
                    " longitude FLOAT NOT NULL, " +
                    " latitude FLOAT NOT NULL);";

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE_WORKOUT);
            db.execSQL(CREATE_DB_TABLE_DETAIL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + WORKOUT_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DETAIL_TABLE_NAME);
            onCreate(db);
        }
    }

    public WorkoutProvider() {
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();
        return (db == null) ? false : true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = 0;

        switch (uriMatcher.match(uri)) {
            case URI_WORKOUTS:
                // Add a new workout record
                id = db.insert(WORKOUT_TABLE_NAME, "", values);

                // If record is added successfully
                if (id > 0) {
                    Uri _uri = ContentUris.withAppendedId(WORKOUT_CONTENT_URI, id);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                break;

            case URI_DETAILS:
                // Add a new workout record
                id = db.insert(DETAIL_TABLE_NAME, "", values);

                // If record is added successfully
                if (id > 0) {
                    Uri _uri = ContentUris.withAppendedId(DETAIL_CONTENT_URI, id);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                break;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case URI_WORKOUTS:
                qb.setTables(WORKOUT_TABLE_NAME);
                qb.setProjectionMap(WORKOUTS_PROJECTION_MAP);
                break;

            case URI_WORKOUT_ID:
                qb.setTables(WORKOUT_TABLE_NAME);
                qb.appendWhere(WORKOUT_ID + "=" + uri.getPathSegments().get(1));
                break;

            case URI_DETAILS:
                qb.setTables(DETAIL_TABLE_NAME);
                qb.setProjectionMap(WORKOUTS_PROJECTION_MAP);
                break;

            case URI_DETAIL_ID:
                qb.setTables(DETAIL_TABLE_NAME);
                qb.appendWhere(WORKOUT_ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder == "") {
            switch(qb.getTables())
            {
                case WORKOUT_TABLE_NAME:
                    sortOrder = WORKOUT_START_TIME;
                    break;

                case DETAIL_TABLE_NAME:
                    sortOrder = DETAIL_TIME;
                    break;
            }
        }
		
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // register to watch a content URI for changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = "";

        switch (uriMatcher.match(uri)) {
            case URI_WORKOUTS:
                count = db.delete(WORKOUT_TABLE_NAME, selection, selectionArgs);
                break;

            case URI_WORKOUT_ID:
                id = uri.getPathSegments().get(1);
                count = db.delete(WORKOUT_TABLE_NAME, WORKOUT_ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            case URI_DETAILS:
                count = db.delete(DETAIL_TABLE_NAME, selection, selectionArgs);
                break;

            case URI_DETAIL_ID:
                id = uri.getPathSegments().get(1);
                count = db.delete(DETAIL_TABLE_NAME, DETAIL_ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case URI_WORKOUTS:
                count = db.update(WORKOUT_TABLE_NAME, values, selection, selectionArgs);
                break;

            case URI_WORKOUT_ID:
                count = db.update(WORKOUT_TABLE_NAME, values, WORKOUT_ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            case URI_DETAILS:
                count = db.update(DETAIL_TABLE_NAME, values, selection, selectionArgs);
                break;

            case URI_DETAIL_ID:
                count = db.update(DETAIL_TABLE_NAME, values, DETAIL_ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {

            // Get all workout records
            case URI_WORKOUTS:
                return "vnd.android.cursor.dir/vnd.example.workouts";

             // Get a particular workout
            case URI_WORKOUT_ID:
                return "vnd.android.cursor.item/vnd.example.workout";

            // Get all workout records
            case URI_DETAILS:
                return "vnd.android.cursor.dir/vnd.example.details";

            // Get a particular workout
            case URI_DETAIL_ID:
                return "vnd.android.cursor.item/vnd.example.detail";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
