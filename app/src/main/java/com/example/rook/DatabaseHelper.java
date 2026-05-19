package com.example.rook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rook_api_lab.db";
    private static final int DATABASE_VERSION = 5;

    // Table names
    public static final String TABLE_PROJECTS = "projects";
    public static final String TABLE_ENDPOINTS = "endpoints";
    public static final String TABLE_HISTORY = "history";
    public static final String TABLE_TEST_RESULTS = "test_results";

    // Common columns
    public static final String KEY_ID = "id";

    // Projects table columns
    public static final String KEY_PROJECT_NAME = "name";
    public static final String KEY_PROJECT_DESCRIPTION = "description";
    public static final String KEY_PROJECT_TEMPLATE = "template";
    public static final String KEY_PROJECT_CREATED_AT = "created_at";

    // Endpoints table columns
    public static final String KEY_ENDPOINT_PROJECT_ID = "project_id";
    public static final String KEY_ENDPOINT_METHOD = "method";
    public static final String KEY_ENDPOINT_PATH = "path";
    public static final String KEY_ENDPOINT_DESCRIPTION = "description";
    public static final String KEY_ENDPOINT_URL = "url";
    public static final String KEY_ENDPOINT_HEADERS = "headers";
    public static final String KEY_ENDPOINT_BODY = "body";
    public static final String KEY_ENDPOINT_AUTH_TYPE = "auth_type";
    public static final String KEY_ENDPOINT_AUTH_TOKEN = "auth_token";
    public static final String KEY_ENDPOINT_AUTH_USERNAME = "auth_username";
    public static final String KEY_ENDPOINT_AUTH_PASSWORD = "auth_password";

    // History table columns
    public static final String KEY_HISTORY_METHOD = "method";
    public static final String KEY_HISTORY_PATH = "path";
    public static final String KEY_HISTORY_STATUS = "status";
    public static final String KEY_HISTORY_LATENCY = "latency";
    public static final String KEY_HISTORY_RESPONSE = "response_body";
    public static final String KEY_HISTORY_TIMESTAMP = "timestamp";

    // Test results table columns
    public static final String KEY_RESULT_API_ID = "api_id";
    public static final String KEY_RESULT_COLLECTION_ID = "collection_id";
    public static final String KEY_RESULT_REQUEST_URL = "request_url";
    public static final String KEY_RESULT_METHOD = "method";
    public static final String KEY_RESULT_HEADERS = "headers";
    public static final String KEY_RESULT_REQUEST_BODY = "request_body";
    public static final String KEY_RESULT_RESPONSE_BODY = "response_body";
    public static final String KEY_RESULT_STATUS_CODE = "status_code";
    public static final String KEY_RESULT_RESPONSE_TIME = "response_time";
    public static final String KEY_RESULT_STATUS = "result_status";
    public static final String KEY_RESULT_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Projects Table
        String CREATE_PROJECTS_TABLE = "CREATE TABLE " + TABLE_PROJECTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PROJECT_NAME + " TEXT NOT NULL,"
                + KEY_PROJECT_DESCRIPTION + " TEXT,"
                + KEY_PROJECT_TEMPLATE + " TEXT,"
                + KEY_PROJECT_CREATED_AT + " INTEGER NOT NULL" + ")";
        db.execSQL(CREATE_PROJECTS_TABLE);

        // Create Endpoints Table
        String CREATE_ENDPOINTS_TABLE = "CREATE TABLE " + TABLE_ENDPOINTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ENDPOINT_PROJECT_ID + " INTEGER NOT NULL,"
                + KEY_ENDPOINT_METHOD + " TEXT NOT NULL,"
                + KEY_ENDPOINT_PATH + " TEXT NOT NULL,"
                + KEY_ENDPOINT_DESCRIPTION + " TEXT,"
                + KEY_ENDPOINT_URL + " TEXT NOT NULL,"
                + KEY_ENDPOINT_HEADERS + " TEXT,"
                + KEY_ENDPOINT_BODY + " TEXT,"
                + KEY_ENDPOINT_AUTH_TYPE + " TEXT,"
                + KEY_ENDPOINT_AUTH_TOKEN + " TEXT,"
                + KEY_ENDPOINT_AUTH_USERNAME + " TEXT,"
                + KEY_ENDPOINT_AUTH_PASSWORD + " TEXT,"
                + "FOREIGN KEY(" + KEY_ENDPOINT_PROJECT_ID + ") REFERENCES " + TABLE_PROJECTS + "(" + KEY_ID + ") ON DELETE CASCADE" + ")";
        db.execSQL(CREATE_ENDPOINTS_TABLE);

        // Create History Table
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_HISTORY_METHOD + " TEXT NOT NULL,"
                + KEY_HISTORY_PATH + " TEXT NOT NULL,"
                + KEY_HISTORY_STATUS + " TEXT NOT NULL,"
                + KEY_HISTORY_LATENCY + " INTEGER NOT NULL,"
                + KEY_HISTORY_RESPONSE + " TEXT,"
                + KEY_HISTORY_TIMESTAMP + " INTEGER NOT NULL" + ")";
        db.execSQL(CREATE_HISTORY_TABLE);

        createTestResultsTable(db);

        // New installs start empty. All app content is created by the user.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PROJECTS + " ADD COLUMN " + KEY_PROJECT_CREATED_AT + " INTEGER NOT NULL DEFAULT 0");
            ContentValues values = new ContentValues();
            values.put(KEY_PROJECT_CREATED_AT, System.currentTimeMillis());
            db.update(TABLE_PROJECTS, values, KEY_PROJECT_CREATED_AT + "=0", null);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_HEADERS + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_BODY + " TEXT");
            createTestResultsTable(db);
        }
        if (oldVersion < 4) {
            removeSeedData(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_AUTH_TYPE + " TEXT DEFAULT 'None'");
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_AUTH_TOKEN + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_AUTH_USERNAME + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_ENDPOINTS + " ADD COLUMN " + KEY_ENDPOINT_AUTH_PASSWORD + " TEXT DEFAULT ''");
        }
    }

    private void createTestResultsTable(SQLiteDatabase db) {
        String CREATE_TEST_RESULTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TEST_RESULTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RESULT_API_ID + " INTEGER,"
                + KEY_RESULT_COLLECTION_ID + " INTEGER NOT NULL,"
                + KEY_RESULT_REQUEST_URL + " TEXT NOT NULL,"
                + KEY_RESULT_METHOD + " TEXT NOT NULL,"
                + KEY_RESULT_HEADERS + " TEXT,"
                + KEY_RESULT_REQUEST_BODY + " TEXT,"
                + KEY_RESULT_RESPONSE_BODY + " TEXT,"
                + KEY_RESULT_STATUS_CODE + " INTEGER NOT NULL,"
                + KEY_RESULT_RESPONSE_TIME + " INTEGER NOT NULL,"
                + KEY_RESULT_STATUS + " TEXT NOT NULL,"
                + KEY_RESULT_TIMESTAMP + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_RESULT_API_ID + ") REFERENCES " + TABLE_ENDPOINTS + "(" + KEY_ID + ") ON DELETE SET NULL,"
                + "FOREIGN KEY(" + KEY_RESULT_COLLECTION_ID + ") REFERENCES " + TABLE_PROJECTS + "(" + KEY_ID + ") ON DELETE CASCADE" + ")";
        db.execSQL(CREATE_TEST_RESULTS_TABLE);
    }

    private void removeSeedData(SQLiteDatabase db) {
        String[] seedProjectNames = {
                "Main Dashboard API",
                "Auth Service",
                "Payment Gateway",
                "Inventory Endpoints"
        };
        for (String projectName : seedProjectNames) {
            Cursor cursor = db.query(TABLE_PROJECTS, new String[]{KEY_ID}, KEY_PROJECT_NAME + "=?", new String[]{projectName}, null, null, null);
            while (cursor.moveToNext()) {
                long projectId = cursor.getLong(0);
                db.delete(TABLE_TEST_RESULTS, KEY_RESULT_COLLECTION_ID + "=?", new String[]{String.valueOf(projectId)});
                db.delete(TABLE_ENDPOINTS, KEY_ENDPOINT_PROJECT_ID + "=?", new String[]{String.valueOf(projectId)});
                db.delete(TABLE_PROJECTS, KEY_ID + "=?", new String[]{String.valueOf(projectId)});
            }
            cursor.close();
        }

        db.delete(TABLE_HISTORY, KEY_HISTORY_RESPONSE + " IN (?, ?, ?)", new String[]{
                "{\"token\":\"jwt_bearer_token_xyz\"}",
                "{\"error\":\"User not found\"}",
                "{\"status\":\"updated\"}"
        });
    }

    // --- Public Operations ---

    public long addProject(String name, String description, String template) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_PROJECT_NAME, name);
        cv.put(KEY_PROJECT_DESCRIPTION, description);
        cv.put(KEY_PROJECT_TEMPLATE, template);
        cv.put(KEY_PROJECT_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_PROJECTS, null, cv);
    }

    public int updateProject(long projectId, String name, String description, String template) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_PROJECT_NAME, name);
        cv.put(KEY_PROJECT_DESCRIPTION, description);
        cv.put(KEY_PROJECT_TEMPLATE, template);
        return db.update(TABLE_PROJECTS, cv, KEY_ID + "=?", new String[]{String.valueOf(projectId)});
    }

    public void deleteProject(long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PROJECTS, KEY_ID + "=?", new String[]{String.valueOf(projectId)});
    }

    public void deleteEndpoint(long endpointId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENDPOINTS, KEY_ID + "=?", new String[]{String.valueOf(endpointId)});
    }

    public long addEndpoint(long projectId, String method, String path, String description, String url) {
        return addEndpoint(projectId, method, path, description, url, "", "", "None", "", "", "");
    }

    public long addEndpoint(long projectId, String method, String path, String description, String url, String headers, String body) {
        return addEndpoint(projectId, method, path, description, url, headers, body, "None", "", "", "");
    }

    public long addEndpoint(long projectId, String method, String path, String description, String url, String headers, String body,
                            String authType, String authToken, String authUsername, String authPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_ENDPOINT_PROJECT_ID, projectId);
        cv.put(KEY_ENDPOINT_METHOD, method);
        cv.put(KEY_ENDPOINT_PATH, path);
        cv.put(KEY_ENDPOINT_DESCRIPTION, description);
        cv.put(KEY_ENDPOINT_URL, url);
        cv.put(KEY_ENDPOINT_HEADERS, headers);
        cv.put(KEY_ENDPOINT_BODY, body);
        cv.put(KEY_ENDPOINT_AUTH_TYPE, authType);
        cv.put(KEY_ENDPOINT_AUTH_TOKEN, authToken);
        cv.put(KEY_ENDPOINT_AUTH_USERNAME, authUsername);
        cv.put(KEY_ENDPOINT_AUTH_PASSWORD, authPassword);
        return db.insert(TABLE_ENDPOINTS, null, cv);
    }

    public int updateEndpoint(long endpointId, String method, String path, String description, String url, String headers, String body,
                               String authType, String authToken, String authUsername, String authPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_ENDPOINT_METHOD, method);
        cv.put(KEY_ENDPOINT_PATH, path);
        cv.put(KEY_ENDPOINT_DESCRIPTION, description);
        cv.put(KEY_ENDPOINT_URL, url);
        cv.put(KEY_ENDPOINT_HEADERS, headers);
        cv.put(KEY_ENDPOINT_BODY, body);
        cv.put(KEY_ENDPOINT_AUTH_TYPE, authType);
        cv.put(KEY_ENDPOINT_AUTH_TOKEN, authToken);
        cv.put(KEY_ENDPOINT_AUTH_USERNAME, authUsername);
        cv.put(KEY_ENDPOINT_AUTH_PASSWORD, authPassword);
        return db.update(TABLE_ENDPOINTS, cv, KEY_ID + "=?", new String[]{String.valueOf(endpointId)});
    }

    public long addTestResult(long apiId, long collectionId, String requestUrl, String method, String headers,
                              String requestBody, String responseBody, int statusCode, int responseTime,
                              String resultStatus, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (apiId > 0) {
            cv.put(KEY_RESULT_API_ID, apiId);
        }
        cv.put(KEY_RESULT_COLLECTION_ID, collectionId);
        cv.put(KEY_RESULT_REQUEST_URL, requestUrl);
        cv.put(KEY_RESULT_METHOD, method);
        cv.put(KEY_RESULT_HEADERS, headers);
        cv.put(KEY_RESULT_REQUEST_BODY, requestBody);
        cv.put(KEY_RESULT_RESPONSE_BODY, responseBody);
        cv.put(KEY_RESULT_STATUS_CODE, statusCode);
        cv.put(KEY_RESULT_RESPONSE_TIME, responseTime);
        cv.put(KEY_RESULT_STATUS, resultStatus);
        cv.put(KEY_RESULT_TIMESTAMP, timestamp);
        return db.insert(TABLE_TEST_RESULTS, null, cv);
    }

    public long addHistory(String method, String path, String status, int latency, String responseBody) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_HISTORY_METHOD, method);
        cv.put(KEY_HISTORY_PATH, path);
        cv.put(KEY_HISTORY_STATUS, status);
        cv.put(KEY_HISTORY_LATENCY, latency);
        cv.put(KEY_HISTORY_RESPONSE, responseBody);
        cv.put(KEY_HISTORY_TIMESTAMP, System.currentTimeMillis());
        return db.insert(TABLE_HISTORY, null, cv);
    }

    public Cursor getAllProjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PROJECTS, null, null, null, null, null, KEY_PROJECT_CREATED_AT + " DESC, " + KEY_ID + " DESC");
    }

    public Cursor getProject(long projectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PROJECTS, null, KEY_ID + "=?", new String[]{String.valueOf(projectId)}, null, null, null, "1");
    }

    public Cursor getEndpointsForProject(long projectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ENDPOINTS, null, KEY_ENDPOINT_PROJECT_ID + "=?", new String[]{String.valueOf(projectId)}, null, null, KEY_ID + " ASC");
    }

    public Cursor getEndpoint(long endpointId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ENDPOINTS, null, KEY_ID + "=?", new String[]{String.valueOf(endpointId)}, null, null, null, "1");
    }

    public Cursor getRecentHistory(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, null, null, null, null, KEY_HISTORY_TIMESTAMP + " DESC", String.valueOf(limit));
    }

    public Cursor getRecentTestResults(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT tr.*, p." + KEY_PROJECT_NAME + " AS collection_name, e." + KEY_ENDPOINT_PATH + " AS api_path "
                + "FROM " + TABLE_TEST_RESULTS + " tr "
                + "LEFT JOIN " + TABLE_PROJECTS + " p ON tr." + KEY_RESULT_COLLECTION_ID + "=p." + KEY_ID + " "
                + "LEFT JOIN " + TABLE_ENDPOINTS + " e ON tr." + KEY_RESULT_API_ID + "=e." + KEY_ID + " "
                + "ORDER BY tr." + KEY_RESULT_TIMESTAMP + " DESC LIMIT ?";
        return db.rawQuery(sql, new String[]{String.valueOf(limit)});
    }

    public int getTestResultCount() {
        return getCount("SELECT COUNT(*) FROM " + TABLE_TEST_RESULTS, null);
    }

    public int getHistoryCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public float getSuccessRate() {
        int testResultCount = getTestResultCount();
        if (testResultCount > 0) {
            int success = getCount("SELECT COUNT(*) FROM " + TABLE_TEST_RESULTS + " WHERE " + KEY_RESULT_STATUS + "='SUCCESS'", null);
            return ((float) success / testResultCount) * 100.0f;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        Cursor cSuccess = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY + " WHERE " + KEY_HISTORY_STATUS + " LIKE '2%'", null);
        
        int total = 0;
        int success = 0;
        
        if (cTotal.moveToFirst()) {
            total = cTotal.getInt(0);
        }
        if (cSuccess.moveToFirst()) {
            success = cSuccess.getInt(0);
        }
        
        cTotal.close();
        cSuccess.close();
        
        if (total == 0) return 100.0f;
        return ((float) success / total) * 100.0f;
    }

    public int getAverageLatency() {
        int testResultCount = getTestResultCount();
        if (testResultCount > 0) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT AVG(" + KEY_RESULT_RESPONSE_TIME + ") FROM " + TABLE_TEST_RESULTS, null);
            int avg = 0;
            if (c.moveToFirst()) {
                avg = c.getInt(0);
            }
            c.close();
            return avg;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT AVG(" + KEY_HISTORY_LATENCY + ") FROM " + TABLE_HISTORY, null);
        int avg = 0;
        if (c.moveToFirst()) {
            avg = c.getInt(0);
        }
        c.close();
        return avg;
    }


    public int getProjectCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PROJECTS, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int getEndpointCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ENDPOINTS, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int getEndpointCountForProject(long projectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ENDPOINTS + " WHERE " + KEY_ENDPOINT_PROJECT_ID + "=?", new String[]{String.valueOf(projectId)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int getFailedHistoryCount() {
        int testResultCount = getTestResultCount();
        if (testResultCount > 0) {
            return getCount("SELECT COUNT(*) FROM " + TABLE_TEST_RESULTS + " WHERE " + KEY_RESULT_STATUS + "='FAILURE'", null);
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY + " WHERE " + KEY_HISTORY_STATUS + " LIKE '4%' OR " + KEY_HISTORY_STATUS + " LIKE '5%' OR " + KEY_HISTORY_STATUS + "='ERROR'", null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    private int getCount(String sql, String[] args) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, args);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

}
