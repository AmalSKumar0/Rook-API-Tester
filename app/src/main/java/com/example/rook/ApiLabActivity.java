package com.example.rook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiLabActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private OkHttpClient client;
    private long projectId = -1;
    private long apiId = -1;
    private String selectedMethod = "GET";
    private String selectedAuthType = "None";
    private Call currentCall;
    
    private EditText etApiPath, etApiUrl, etApiDescription, etHeaders, etBody;
    private MaterialButton btnGet, btnPost, btnPut, btnDelete;
    private MaterialButton btnSaveApi;
    private TabLayout tabLayout;
    
    // Project Selection
    private View layoutProjectSelection;
    private Spinner spinnerProject;
    private final java.util.List<ProjectItem> projectList = new java.util.ArrayList<>();
    private java.util.List<String> projectNames = new java.util.ArrayList<>();
    
    // Auth Views
    private LinearLayout layoutAuth, layoutBasicAuth;
    private Spinner spinnerAuthType;
    private EditText etAuthToken, etAuthUsername, etAuthPassword;
    private TextView tvNoAuth;
    
    private TextView tvResponseStatus, tvResponseTime, tvResponseSize, tvResponseContent;
    private TextView tvHistoryCount, tvFavoritesCount;
    private View vStatusIndicator;
    private ProgressBar responseProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_lab);

        dbHelper = new DatabaseHelper(this);
        client = new OkHttpClient();
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);
        apiId = getIntent().getLongExtra("API_ID", -1);

        NavigationUtils.setupAppChrome(this, apiId == -1 ? "API Lab" : "Edit API", true);
        
        TextView headerSubtitle = findViewById(R.id.headerSubtitle);
        if (headerSubtitle != null) headerSubtitle.setText(apiId == -1 ? "Request Builder" : "Modify Details");

        initViews();
        setupMethodButtons();
        setupTabs();
        setupAuthTypeSpinner();
        setupProjectSpinner();

        btnSaveApi = findViewById(R.id.btnSaveApi);
        if (apiId != -1) {
            btnSaveApi.setText("Update");
        }
        btnSaveApi.setOnClickListener(v -> saveApi());
        
        findViewById(R.id.btnSendRequest).setOnClickListener(v -> sendRequest());
        findViewById(R.id.btnCopyResponse).setOnClickListener(v -> copyResponseToClipboard());
        
        if (apiId != -1) {
            loadApiData();
        } else {
            // Handle one-off tests from history/analytics
            String method = getIntent().getStringExtra("METHOD");
            String url = getIntent().getStringExtra("URL");
            String path = getIntent().getStringExtra("PATH");
            if (method != null) selectMethod(method);
            if (url != null) etApiUrl.setText(url);
            if (path != null) etApiPath.setText(path);
        }
        loadStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    private void loadApiData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            android.database.Cursor cursor = dbHelper.getEndpoint(apiId);
            if (cursor != null && cursor.moveToFirst()) {
                String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_METHOD));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_PATH));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_DESCRIPTION));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_URL));
                String headers = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_HEADERS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_BODY));
                String authType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_TYPE));
                String authToken = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_TOKEN));
                String authUsername = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_USERNAME));
                String authPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_PASSWORD));
                cursor.close();

                AppExecutors.getInstance().mainThread().execute(() -> {
                    selectMethod(method);
                    etApiPath.setText(path);
                    etApiUrl.setText(url);
                    etApiDescription.setText(desc);
                    etHeaders.setText(headers);
                    etBody.setText(body);
                    
                    // Auth selection
                    int authPos = 0;
                    if ("Bearer Token".equals(authType)) authPos = 1;
                    else if ("Basic Auth".equals(authType)) authPos = 2;
                    spinnerAuthType.setSelection(authPos);
                    
                    etAuthToken.setText(authToken);
                    etAuthUsername.setText(authUsername);
                    etAuthPassword.setText(authPassword);
                });
            }
        });
    }

    private void initViews() {
        etApiPath = findViewById(R.id.etApiPath);
        etApiUrl = findViewById(R.id.etApiUrl);
        etApiDescription = findViewById(R.id.etApiDescription);
        etHeaders = findViewById(R.id.etHeaders);
        etBody = findViewById(R.id.etBody);
        
        btnGet = findViewById(R.id.btnGet);
        btnPost = findViewById(R.id.btnPost);
        btnPut = findViewById(R.id.btnPut);
        btnDelete = findViewById(R.id.btnDelete);
        
        tabLayout = findViewById(R.id.tabLayout);
        
        layoutAuth = findViewById(R.id.layoutAuth);
        layoutBasicAuth = findViewById(R.id.layoutBasicAuth);
        spinnerAuthType = findViewById(R.id.spinnerAuthType);
        etAuthToken = findViewById(R.id.etAuthToken);
        etAuthUsername = findViewById(R.id.etAuthUsername);
        etAuthPassword = findViewById(R.id.etAuthPassword);
        tvNoAuth = findViewById(R.id.tvNoAuth);
        
        tvResponseStatus = findViewById(R.id.tvResponseStatus);
        tvResponseTime = findViewById(R.id.tvResponseTime);
        tvResponseSize = findViewById(R.id.tvResponseSize);
        tvResponseContent = findViewById(R.id.tvResponseContent);
        vStatusIndicator = findViewById(R.id.vStatusIndicator);
        responseProgressBar = findViewById(R.id.responseProgressBar);
        
        tvHistoryCount = findViewById(R.id.tvHistoryCount);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        
        layoutProjectSelection = findViewById(R.id.layoutProjectSelection);
        spinnerProject = findViewById(R.id.spinnerProject);
    }

    private void setupProjectSpinner() {
        if (projectId != -1) {
            layoutProjectSelection.setVisibility(View.GONE);
            return;
        }

        layoutProjectSelection.setVisibility(View.VISIBLE);
        AppExecutors.getInstance().diskIO().execute(() -> {
            android.database.Cursor cursor = dbHelper.getAllProjects();
            projectList.clear();
            projectNames.clear();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                    projectList.add(new ProjectItem(id, name));
                    projectNames.add(name);
                }
                cursor.close();
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (projectList.isEmpty()) {
                    projectNames.add("No Collections Found");
                    spinnerProject.setEnabled(false);
                } else {
                    spinnerProject.setEnabled(true);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerProject.setAdapter(adapter);
            });
        });
    }

    private static class ProjectItem {
        long id;
        String name;
        ProjectItem(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private void setupAuthTypeSpinner() {
        String[] authTypes = {"None", "Bearer Token", "Basic Auth"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, authTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAuthType.setAdapter(adapter);

        spinnerAuthType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAuthType = authTypes[position];
                updateAuthFieldsVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateAuthFieldsVisibility() {
        tvNoAuth.setVisibility(selectedAuthType.equals("None") ? View.VISIBLE : View.GONE);
        etAuthToken.setVisibility(selectedAuthType.equals("Bearer Token") ? View.VISIBLE : View.GONE);
        layoutBasicAuth.setVisibility(selectedAuthType.equals("Basic Auth") ? View.VISIBLE : View.GONE);
    }

    private void loadStats() {
        if (tvHistoryCount != null) {
            int count = dbHelper.getHistoryCount();
            tvHistoryCount.setText(count + (count == 1 ? " request today" : " requests today"));
        }
        if (tvFavoritesCount != null) {
            int count = dbHelper.getEndpointCount();
            tvFavoritesCount.setText(count + (count == 1 ? " saved endpoint" : " saved endpoints"));
        }
    }

    private void setupMethodButtons() {
        btnGet.setOnClickListener(v -> selectMethod("GET"));
        btnPost.setOnClickListener(v -> selectMethod("POST"));
        btnPut.setOnClickListener(v -> selectMethod("PUT"));
        btnDelete.setOnClickListener(v -> selectMethod("DELETE"));
        
        selectMethod("GET"); // Default
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                etHeaders.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                etBody.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                layoutAuth.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void selectMethod(String method) {
        selectedMethod = method;
        
        // Reset all to default
        resetButtonStyle(btnGet);
        resetButtonStyle(btnPost);
        resetButtonStyle(btnPut);
        resetButtonStyle(btnDelete);

        // Highlight selected
        switch (method) {
            case "GET":
                setSelectedButtonStyle(btnGet, R.color.primary_container, R.color.on_primary_container);
                break;
            case "POST":
                setSelectedButtonStyle(btnPost, R.color.secondary_container, R.color.on_secondary_container);
                break;
            case "PUT":
                setSelectedButtonStyle(btnPut, R.color.tertiary_container, R.color.on_tertiary_container);
                break;
            case "DELETE":
                setSelectedButtonStyle(btnDelete, R.color.error_container, R.color.on_error_container);
                break;
        }
    }

    private void resetButtonStyle(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_container_lowest)));
        btn.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.stroke)));
    }

    private void setSelectedButtonStyle(MaterialButton btn, int bgColorRes, int textColorRes) {
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes)));
        btn.setTextColor(ContextCompat.getColor(this, textColorRes));
        btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.stroke)));
    }

    private void sendRequest() {
        String url = etApiUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            etApiUrl.setError("URL is required");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        final String finalUrl = url;
        final String headersStr = etHeaders.getText().toString().trim();
        final String bodyStr = etBody.getText().toString().trim();

        Request.Builder builder = new Request.Builder().url(finalUrl);
        
        // Add custom headers
        if (!TextUtils.isEmpty(headersStr)) {
            String[] lines = headersStr.split("\n");
            for (String line : lines) {
                int index = line.indexOf(":");
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if (!key.isEmpty()) builder.addHeader(key, value);
                }
            }
        }

        // Add Auth header
        applyAuth(builder);

        // Add body if applicable
        if (!selectedMethod.equals("GET")) {
            RequestBody body = RequestBody.create(bodyStr, MediaType.parse("application/json; charset=utf-8"));
            builder.method(selectedMethod, body);
        } else {
            builder.get();
        }

        responseProgressBar.setVisibility(View.VISIBLE);
        tvResponseContent.setText("Executing request...");
        final long startTime = System.currentTimeMillis();

        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        currentCall = client.newCall(builder.build());

        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    responseProgressBar.setVisibility(View.GONE);
                    updateResponseUI(0, "ERROR", System.currentTimeMillis() - startTime, e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                final int code = response.code();
                final String message = response.message();
                final long duration = System.currentTimeMillis() - startTime;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    responseProgressBar.setVisibility(View.GONE);
                    updateResponseUI(code, message, duration, responseBody);
                    
                    // Add to history on background thread
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        dbHelper.addHistory(selectedMethod, etApiPath.getText().toString(), code + " " + message, (int)duration, responseBody);
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                loadStats();
                            }
                        });
                    });
                });
            }
        });
    }

    private void applyAuth(Request.Builder builder) {
        if ("Bearer Token".equals(selectedAuthType)) {
            String token = etAuthToken.getText().toString().trim();
            if (!token.isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
        } else if ("Basic Auth".equals(selectedAuthType)) {
            String username = etAuthUsername.getText().toString().trim();
            String password = etAuthPassword.getText().toString().trim();
            String credentials = username + ":" + password;
            String auth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), android.util.Base64.NO_WRAP);
            builder.header("Authorization", auth);
        }
    }

    private void updateResponseUI(int code, String message, long duration, String body) {
        tvResponseStatus.setText(code + " " + message);
        tvResponseTime.setText(duration + "ms");
        
        if (body != null) {
            String trimmedBody = body.trim();
            tvResponseSize.setText(String.format(java.util.Locale.US, "%.2f KB", body.length() / 1024f));
            try {
                // Try to format JSON
                if (trimmedBody.startsWith("{")) {
                    tvResponseContent.setText(new JSONObject(trimmedBody).toString(2));
                } else if (trimmedBody.startsWith("[")) {
                    tvResponseContent.setText(new JSONArray(trimmedBody).toString(2));
                } else {
                    tvResponseContent.setText(body);
                }
            } catch (JSONException e) {
                tvResponseContent.setText(body);
            }
        } else {
            tvResponseSize.setText("0 KB");
            tvResponseContent.setText("(No Content)");
        }

        // Update status indicator color
        if (code >= 200 && code < 300) {
            vStatusIndicator.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            tvResponseStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            vStatusIndicator.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            tvResponseStatus.setTextColor(ContextCompat.getColor(this, R.color.error));
        }
    }

    private void saveApi() {
        String path = etApiPath.getText().toString().trim();
        String url = etApiUrl.getText().toString().trim();
        String desc = etApiDescription.getText().toString().trim();
        String headers = etHeaders.getText().toString().trim();
        String body = etBody.getText().toString().trim();
        
        String authToken = etAuthToken.getText().toString().trim();
        String authUser = etAuthUsername.getText().toString().trim();
        String authPass = etAuthPassword.getText().toString().trim();

        if (TextUtils.isEmpty(path)) {
            etApiPath.setError("Path is required");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            etApiUrl.setError("URL is required");
            return;
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        long targetProjectId = projectId;
        if (targetProjectId == -1) {
            if (projectList.isEmpty()) {
                Toast.makeText(this, "Create a collection first!", Toast.LENGTH_LONG).show();
                return;
            }
            targetProjectId = projectList.get(spinnerProject.getSelectedItemPosition()).id;
        }

        final long finalProjectId = targetProjectId;
        final String finalPath = path;
        AppExecutors.getInstance().diskIO().execute(() -> {
            long result;
            if (apiId == -1) {
                result = dbHelper.addEndpoint(finalProjectId, selectedMethod, finalPath, desc, url, headers, body,
                        selectedAuthType, authToken, authUser, authPass);
            } else {
                result = dbHelper.updateEndpoint(apiId, selectedMethod, finalPath, desc, url, headers, body,
                        selectedAuthType, authToken, authUser, authPass);
            }
            
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (result != -1) {
                    Toast.makeText(this, apiId == -1 ? "API Saved to Collection" : "API Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save API", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void copyResponseToClipboard() {
        String content = tvResponseContent.getText().toString();
        if (!TextUtils.isEmpty(content)) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("API Response", content);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Response copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
