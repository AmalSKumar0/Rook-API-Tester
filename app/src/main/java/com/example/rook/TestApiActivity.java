package com.example.rook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestApiActivity extends AppCompatActivity {

    private OkHttpClient client;
    private DatabaseHelper dbHelper;
    private TextInputEditText etUrl;
    private TextView tvStatusCode, tvLatency, tvResponseBody, tvSize;
    private String selectedMethod = "GET";
    private String currentResponseBody = "";
    private long apiId = -1;
    private long projectId = -1;
    private String requestHeaders = "";
    private String requestBody = "";
    private String authType = "None";
    private String authToken = "";
    private String authUsername = "";
    private String authPassword = "";

    // Method views
    private TextView methodGet, methodPost, methodPut, methodDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_api);

        client = new OkHttpClient();
        dbHelper = new DatabaseHelper(this);
        NavigationUtils.setupAppChrome(this, "API Details", true);

        if (getIntent() != null) {
            apiId = getIntent().getLongExtra("API_ID", -1);
            projectId = getIntent().getLongExtra("PROJECT_ID", -1);
            requestHeaders = getIntent().getStringExtra("HEADERS") != null ? getIntent().getStringExtra("HEADERS") : "";
            requestBody = getIntent().getStringExtra("BODY") != null ? getIntent().getStringExtra("BODY") : "";
            authType = getIntent().getStringExtra("AUTH_TYPE") != null ? getIntent().getStringExtra("AUTH_TYPE") : "None";
            authToken = getIntent().getStringExtra("AUTH_TOKEN") != null ? getIntent().getStringExtra("AUTH_TOKEN") : "";
            authUsername = getIntent().getStringExtra("AUTH_USERNAME") != null ? getIntent().getStringExtra("AUTH_USERNAME") : "";
            authPassword = getIntent().getStringExtra("AUTH_PASSWORD") != null ? getIntent().getStringExtra("AUTH_PASSWORD") : "";
        }

        etUrl = findViewById(R.id.etUrl);
        tvStatusCode = findViewById(R.id.tvStatusCode);
        tvLatency = findViewById(R.id.tvLatency);
        tvResponseBody = findViewById(R.id.tvResponseBody);
        tvSize = findViewById(R.id.tvSize);

        // Pre-fill URL if passed as intent extra
        if (getIntent() != null && getIntent().hasExtra("URL")) {
            etUrl.setText(getIntent().getStringExtra("URL"));
        }

        // Method selector pills
        methodGet = findViewById(R.id.methodGet);
        methodPost = findViewById(R.id.methodPost);
        methodPut = findViewById(R.id.methodPut);
        methodDelete = findViewById(R.id.methodDelete);

        // Load pre-selected method if passed
        String initialMethod = "GET";
        if (getIntent() != null && getIntent().hasExtra("METHOD")) {
            initialMethod = getIntent().getStringExtra("METHOD");
        }
        setActiveMethod(initialMethod);

        methodGet.setOnClickListener(v -> setActiveMethod("GET"));
        methodPost.setOnClickListener(v -> setActiveMethod("POST"));
        methodPut.setOnClickListener(v -> setActiveMethod("PUT"));
        methodDelete.setOnClickListener(v -> setActiveMethod("DELETE"));
        bindRequestTabs();

        // Send button
        MaterialButton btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> executeRequest());
        }

        // Copy JSON button
        LinearLayout btnCopy = findViewById(R.id.btnCopyJson);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                if (currentResponseBody.isEmpty()) {
                    Toast.makeText(this, "Nothing to copy yet!", Toast.LENGTH_SHORT).show();
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("JSON Response", currentResponseBody);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setActiveMethod(String method) {
        selectedMethod = method;

        int inactiveText = ContextCompat.getColor(this, R.color.on_surface_variant);
        int inactiveBg = R.drawable.bg_badge_neutral;

        // Reset all to inactive
        methodGet.setTextColor(inactiveText);
        methodGet.setBackgroundResource(inactiveBg);
        methodPost.setTextColor(inactiveText);
        methodPost.setBackgroundResource(inactiveBg);
        methodPut.setTextColor(inactiveText);
        methodPut.setBackgroundResource(inactiveBg);
        methodDelete.setTextColor(inactiveText);
        methodDelete.setBackgroundResource(inactiveBg);

        // Highlight the selected method
        switch (method.toUpperCase()) {
            case "GET":
                methodGet.setBackgroundResource(R.drawable.bg_badge_get);
                methodGet.setTextColor(ContextCompat.getColor(this, R.color.on_primary_container));
                break;
            case "POST":
                methodPost.setBackgroundResource(R.drawable.bg_badge_post);
                methodPost.setTextColor(ContextCompat.getColor(this, R.color.on_secondary_container));
                break;
            case "PUT":
                methodPut.setBackgroundResource(R.drawable.bg_badge_put);
                methodPut.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
                break;
            case "DELETE":
                methodDelete.setBackgroundResource(R.drawable.bg_badge_delete);
                methodDelete.setTextColor(ContextCompat.getColor(this, R.color.on_tertiary_container));
                break;
        }
    }

    private void bindRequestTabs() {
        TextView tabHeaders = findViewById(R.id.tabHeaders);
        TextView tabBody = findViewById(R.id.tabBody);
        TextView tabAuth = findViewById(R.id.tabAuth);

        if (tabHeaders != null) {
            tabHeaders.setOnClickListener(v -> Toast.makeText(this, "Default JSON headers are active.", Toast.LENGTH_SHORT).show());
        }
        if (tabBody != null) {
            tabBody.setOnClickListener(v -> Toast.makeText(this, "Empty request body selected.", Toast.LENGTH_SHORT).show());
        }
        if (tabAuth != null) {
            tabAuth.setOnClickListener(v -> Toast.makeText(this, "No auth token attached.", Toast.LENGTH_SHORT).show());
        }
    }

    private void executeRequest() {
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        tvStatusCode.setText("Loading...");
        tvLatency.setText("...");
        tvResponseBody.setText("Executing request...");
        tvSize.setText("Size: -- KB");

        long startTime = System.currentTimeMillis();
        final String finalUrl = url;

        // Extract path for history logs
        String extractedPath = "/";
        try {
            java.net.URL parsed = new java.net.URL(finalUrl);
            extractedPath = parsed.getPath();
            if (extractedPath == null || extractedPath.isEmpty()) {
                extractedPath = "/";
            }
        } catch (Exception ignored) {}

        final String finalExtractedPath = extractedPath;

        Request.Builder builder = new Request.Builder().url(finalUrl);
        applyHeaders(builder, requestHeaders);
        applyAuth(builder);
        
        RequestBody body = RequestBody.create(requestBody != null ? requestBody : "", MediaType.parse("application/json; charset=utf-8"));
        switch (selectedMethod.toUpperCase()) {
            case "POST":
                builder.post(body);
                break;
            case "PUT":
                builder.put(body);
                break;
            case "DELETE":
                if (requestBody == null || requestBody.isEmpty()) {
                    builder.delete();
                } else {
                    builder.delete(body);
                }
                break;
            default:
                builder.get();
                break;
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                long latency = System.currentTimeMillis() - startTime;
                
                // Write failure to SQLite
                dbHelper.addHistory(selectedMethod, finalExtractedPath, "ERROR", (int) latency, e.getMessage());
                if (apiId > 0 && projectId > 0) {
                    dbHelper.addTestResult(apiId, projectId, finalUrl, selectedMethod, requestHeaders, requestBody,
                            e.getMessage(), 0, (int) latency, "FAILURE", System.currentTimeMillis());
                }

                runOnUiThread(() -> {
                    tvStatusCode.setText("ERROR");
                    tvStatusCode.setTextColor(ContextCompat.getColor(TestApiActivity.this, R.color.error));
                    tvResponseBody.setText(e.getMessage());
                    tvLatency.setText(latency + "ms");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                long latency = System.currentTimeMillis() - startTime;
                int code = response.code();
                String body = response.body() != null ? response.body().string() : "No response body";
                float sizeKb = body.getBytes().length / 1024f;

                // Write success/failure to SQLite
                dbHelper.addHistory(selectedMethod, finalExtractedPath, code + " " + response.message(), (int) latency, body);
                if (apiId > 0 && projectId > 0) {
                    dbHelper.addTestResult(apiId, projectId, finalUrl, selectedMethod, requestHeaders, requestBody,
                            body, code, (int) latency, (code >= 200 && code < 300) ? "SUCCESS" : "FAILURE", System.currentTimeMillis());
                }

                // Pretty-print JSON
                String display = body;
                String trimmedBody = body.trim();
                try {
                    if (trimmedBody.startsWith("{")) {
                        display = new org.json.JSONObject(trimmedBody).toString(4);
                    } else if (trimmedBody.startsWith("[")) {
                        display = new org.json.JSONArray(trimmedBody).toString(4);
                    }
                } catch (Exception ignored) {}

                final String finalDisplay = display;
                currentResponseBody = display;

                runOnUiThread(() -> {
                    tvStatusCode.setText(code + " " + response.message());
                    tvLatency.setText(latency + "ms");
                    tvStatusCode.setTextColor(ContextCompat.getColor(
                            TestApiActivity.this,
                            (code >= 200 && code < 300) ? R.color.primary : R.color.error));
                    tvResponseBody.setText(finalDisplay);
                    tvSize.setText(String.format("Size: %.2f KB", sizeKb));
                });
            }
        });
    }

    private void applyHeaders(Request.Builder builder, String headers) {
        if (headers == null || headers.trim().isEmpty()) return;
        String[] lines = headers.split("\\n");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) continue;
            String name = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!name.isEmpty() && !value.isEmpty()) {
                builder.addHeader(name, value);
            }
        }
    }

    private void applyAuth(Request.Builder builder) {
        if ("Bearer Token".equalsIgnoreCase(authType) && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
        } else if ("Basic Auth".equalsIgnoreCase(authType)) {
            String credentials = authUsername + ":" + authPassword;
            String auth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), android.util.Base64.NO_WRAP);
            builder.header("Authorization", auth);
        }
    }
}
