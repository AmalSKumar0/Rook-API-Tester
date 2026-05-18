package com.example.rook;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProjectDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private OkHttpClient client;
    private long projectId = -1;
    private TextView tvProjectTitle, tvProjectSubtext, tvCollectionMeta;
    private RecyclerView rvEndpoints;
    private EndpointsAdapter adapter;
    private final List<Endpoint> endpointList = new ArrayList<>();
    private final List<Endpoint> filteredList = new ArrayList<>();
    private TextInputEditText etSearch;
    private View emptyApiState;
    private String collectionName = "";
    private String collectionDescription = "";
    private String collectionTemplate = "Empty";
    private long collectionCreatedAt = 0L;
    private MaterialButton btnTestCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        dbHelper = new DatabaseHelper(this);
        client = new OkHttpClient();
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);

        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        tvProjectSubtext = findViewById(R.id.tvProjectSubtext);
        tvCollectionMeta = findViewById(R.id.tvCollectionMeta);
        etSearch = findViewById(R.id.etSearch);
        rvEndpoints = findViewById(R.id.rvEndpoints);
        emptyApiState = findViewById(R.id.emptyApiState);

        rvEndpoints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EndpointsAdapter(filteredList, endpoint -> {
            Intent intent = new Intent(ProjectDetailsActivity.this, TestApiActivity.class);
            intent.putExtra("API_ID", endpoint.id);
            intent.putExtra("PROJECT_ID", projectId);
            intent.putExtra("URL", endpoint.url);
            intent.putExtra("METHOD", endpoint.method);
            intent.putExtra("HEADERS", endpoint.headers);
            intent.putExtra("BODY", endpoint.body);
            intent.putExtra("AUTH_TYPE", endpoint.authType);
            intent.putExtra("AUTH_TOKEN", endpoint.authToken);
            intent.putExtra("AUTH_USERNAME", endpoint.authUsername);
            intent.putExtra("AUTH_PASSWORD", endpoint.authPassword);
            startActivity(intent);
        }, endpoint -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete API")
                    .setMessage("Are you sure you want to delete this API endpoint?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            dbHelper.deleteEndpoint(endpoint.id);
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                loadEndpoints();
                                Toast.makeText(this, "API deleted", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvEndpoints.setAdapter(adapter);

        btnTestCollection = findViewById(R.id.btnTestCollection);
        if (btnTestCollection != null) {
            btnTestCollection.setOnClickListener(v -> testEntireCollection());
        }

        MaterialButton btnAddApi = findViewById(R.id.btnAddApi);
        if (btnAddApi != null) {
            btnAddApi.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        MaterialButton btnEmptyAddApi = findViewById(R.id.btnEmptyAddApi);
        if (btnEmptyAddApi != null) {
            btnEmptyAddApi.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        MaterialButton btnEditCollection = findViewById(R.id.btnEditCollection);
        if (btnEditCollection != null) {
            btnEditCollection.setOnClickListener(v -> showEditCollectionDialog());
        }

        FloatingActionButton fabAddEndpoint = findViewById(R.id.fabAddEndpoint);
        if (fabAddEndpoint != null) {
            fabAddEndpoint.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEndpoints(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        NavigationUtils.setupAppChrome(this, "Collection Details", true);
        loadProjectDetails();
    }

    private void loadProjectDetails() {
        if (projectId == -1) {
            Toast.makeText(this, "Collection not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Cursor pCursor = dbHelper.getProject(projectId);
        if (pCursor != null) {
            if (pCursor.moveToFirst()) {
                collectionName = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                collectionDescription = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_DESCRIPTION));
                collectionTemplate = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_TEMPLATE));
                collectionCreatedAt = pCursor.getLong(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_CREATED_AT));

                if (TextUtils.isEmpty(collectionTemplate)) {
                    collectionTemplate = "Empty";
                }
                bindCollectionHeader();
            } else {
                Toast.makeText(this, "Collection not found.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            pCursor.close();
        }

        loadEndpoints();
    }

    private void loadEndpoints() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Endpoint> newItems = new ArrayList<>();
            Cursor cursor = dbHelper.getEndpointsForProject(projectId);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
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
                    newItems.add(new Endpoint(id, method, path, desc, url, headers, body, authType, authToken, authUsername, authPassword));
                }
                cursor.close();
            }
            AppExecutors.getInstance().mainThread().execute(() -> {
                endpointList.clear();
                endpointList.addAll(newItems);
                filterEndpoints(etSearch.getText() != null ? etSearch.getText().toString() : "");
            });
        });
    }

    private void filterEndpoints(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(endpointList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Endpoint e : endpointList) {
                if (e.path.toLowerCase().contains(lowerQuery) || 
                    (e.description != null && e.description.toLowerCase().contains(lowerQuery)) ||
                    e.method.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateCollectionMeta();
    }

    private void bindCollectionHeader() {
        tvProjectTitle.setText(collectionName);
        TextView headerTitle = findViewById(R.id.headerTitle);
        if (headerTitle != null) {
            headerTitle.setText(collectionName);
        }
        tvProjectSubtext.setText(!TextUtils.isEmpty(collectionDescription)
                ? collectionDescription
                : "Manage your collection APIs.");
        updateCollectionMeta();
    }

    private void updateEmptyState() {
        boolean isEmptyCollection = endpointList.isEmpty();
        if (emptyApiState != null) {
            emptyApiState.setVisibility(isEmptyCollection ? View.VISIBLE : View.GONE);
        }
        rvEndpoints.setVisibility(isEmptyCollection ? View.GONE : View.VISIBLE);
    }

    private void updateCollectionMeta() {
        if (tvCollectionMeta == null) return;
        String endpointLabel = endpointList.size() == 1 ? "1 API" : endpointList.size() + " APIs";
        String created = collectionCreatedAt > 0
                ? DateUtils.getRelativeTimeSpanString(collectionCreatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                : "recently";
        tvCollectionMeta.setText(collectionTemplate + " collection · " + endpointLabel + " · Created " + created);
    }

    private void testEntireCollection() {
        if (endpointList.isEmpty()) {
            Toast.makeText(this, "Add an API before testing this collection.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnTestCollection != null) {
            btnTestCollection.setEnabled(false);
            btnTestCollection.setText("Testing 0/" + endpointList.size());
        }
        testEndpointAtIndex(0, 0, 0);
    }

    private void testEndpointAtIndex(int index, int successes, int failures) {
        if (index >= endpointList.size()) {
            int total = successes + failures;
            if (btnTestCollection != null) {
                btnTestCollection.setEnabled(true);
                btnTestCollection.setText("Test Collection");
            }
            Toast.makeText(this, "Collection test complete: " + successes + "/" + total + " passed.", Toast.LENGTH_LONG).show();
            return;
        }

        Endpoint endpoint = endpointList.get(index);
        runOnUiThread(() -> {
            if (btnTestCollection != null) {
                btnTestCollection.setText("Testing " + (index + 1) + "/" + endpointList.size());
            }
        });

        String requestUrl = normalizeUrl(endpoint.url);
        if (TextUtils.isEmpty(requestUrl)) {
            long timestamp = System.currentTimeMillis();
            dbHelper.addTestResult(endpoint.id, projectId, "", endpoint.method, endpoint.headers, endpoint.body,
                    "Missing request URL", 0, 0, "FAILURE", timestamp);
            dbHelper.addHistory(endpoint.method, endpoint.path, "ERROR", 0, "Missing request URL");
            testEndpointAtIndex(index + 1, successes, failures + 1);
            return;
        }

        Request.Builder builder = new Request.Builder().url(requestUrl);
        applyHeaders(builder, endpoint.headers);
        applyAuth(builder, endpoint);

        String method = endpoint.method.toUpperCase();
        String body = endpoint.body != null ? endpoint.body : "";
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));
        switch (method) {
            case "POST":
                builder.post(requestBody);
                break;
            case "PUT":
                builder.put(requestBody);
                break;
            case "DELETE":
                if (body.isEmpty()) {
                    builder.delete();
                } else {
                    builder.delete(requestBody);
                }
                break;
            default:
                builder.get();
                break;
        }

        long startedAt = System.currentTimeMillis();
        try {
            client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                long responseTime = System.currentTimeMillis() - startedAt;
                long timestamp = System.currentTimeMillis();
                dbHelper.addTestResult(endpoint.id, projectId, requestUrl, endpoint.method, endpoint.headers, body,
                        e.getMessage(), 0, (int) responseTime, "FAILURE", timestamp);
                dbHelper.addHistory(endpoint.method, endpoint.path, "ERROR", (int) responseTime, e.getMessage());
                runOnUiThread(() -> testEndpointAtIndex(index + 1, successes, failures + 1));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                long responseTime = System.currentTimeMillis() - startedAt;
                int statusCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                String resultStatus = statusCode >= 200 && statusCode < 300 ? "SUCCESS" : "FAILURE";
                long timestamp = System.currentTimeMillis();

                dbHelper.addTestResult(endpoint.id, projectId, requestUrl, endpoint.method, endpoint.headers, body,
                        responseBody, statusCode, (int) responseTime, resultStatus, timestamp);
                dbHelper.addHistory(endpoint.method, endpoint.path, statusCode + " " + response.message(), (int) responseTime, responseBody);

                int nextSuccesses = "SUCCESS".equals(resultStatus) ? successes + 1 : successes;
                int nextFailures = "SUCCESS".equals(resultStatus) ? failures : failures + 1;
                runOnUiThread(() -> testEndpointAtIndex(index + 1, nextSuccesses, nextFailures));
            }
            });
        } catch (IllegalArgumentException e) {
            long responseTime = System.currentTimeMillis() - startedAt;
            dbHelper.addTestResult(endpoint.id, projectId, requestUrl, endpoint.method, endpoint.headers, body,
                    e.getMessage(), 0, (int) responseTime, "FAILURE", System.currentTimeMillis());
            dbHelper.addHistory(endpoint.method, endpoint.path, "ERROR", (int) responseTime, e.getMessage());
            testEndpointAtIndex(index + 1, successes, failures + 1);
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private void applyHeaders(Request.Builder builder, String headers) {
        if (TextUtils.isEmpty(headers)) return;
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

    private void applyAuth(Request.Builder builder, Endpoint endpoint) {
        if ("Bearer Token".equalsIgnoreCase(endpoint.authType) && !TextUtils.isEmpty(endpoint.authToken)) {
            builder.header("Authorization", "Bearer " + endpoint.authToken);
        } else if ("Basic Auth".equalsIgnoreCase(endpoint.authType)) {
            String credentials = endpoint.authUsername + ":" + endpoint.authPassword;
            String auth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), android.util.Base64.NO_WRAP);
            builder.header("Authorization", auth);
        }
    }

    private void showEditCollectionDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);

        EditText etName = new EditText(this);
        etName.setHint("Collection name");
        etName.setSingleLine(true);
        etName.setText(collectionName);
        container.addView(etName);

        EditText etDescription = new EditText(this);
        etDescription.setHint("Description");
        etDescription.setMinLines(2);
        etDescription.setText(collectionDescription != null ? collectionDescription : "");
        container.addView(etDescription);

        Spinner spinnerTemplate = new Spinner(this);
        String[] templates = {"Empty", "REST API", "GraphQL"};
        ArrayAdapter<String> templateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, templates);
        spinnerTemplate.setAdapter(templateAdapter);
        for (int i = 0; i < templates.length; i++) {
            if (templates[i].equalsIgnoreCase(collectionTemplate)) {
                spinnerTemplate.setSelection(i);
                break;
            }
        }
        container.addView(spinnerTemplate);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Collection")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String template = spinnerTemplate.getSelectedItem().toString();

                if (TextUtils.isEmpty(name)) {
                    etName.setError("Collection name is required.");
                    return;
                }

                int updated = dbHelper.updateProject(projectId, name, description, template);
                if (updated > 0) {
                    collectionName = name;
                    collectionDescription = description;
                    collectionTemplate = template;
                    bindCollectionHeader();
                    Toast.makeText(this, "Collection updated.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to update collection.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Endpoint Model Class
    public static class Endpoint {
        long id;
        String method;
        String path;
        String description;
        String url;
        String headers;
        String body;
        String authType;
        String authToken;
        String authUsername;
        String authPassword;

        public Endpoint(long id, String method, String path, String description, String url, String headers, String body,
                        String authType, String authToken, String authUsername, String authPassword) {
            this.id = id;
            this.method = method;
            this.path = path;
            this.description = description;
            this.url = url;
            this.headers = headers;
            this.body = body;
            this.authType = authType;
            this.authToken = authToken;
            this.authUsername = authUsername;
            this.authPassword = authPassword;
        }
    }

    public interface OnEndpointTestClickListener {
        void onTestClick(Endpoint endpoint);
    }

    public interface OnEndpointDeleteListener {
        void onDeleteClick(Endpoint endpoint);
    }

    // RecyclerView Adapter
    public static class EndpointsAdapter extends RecyclerView.Adapter<EndpointsAdapter.ViewHolder> {

        private final List<Endpoint> items;
        private final OnEndpointTestClickListener listener;
        private final OnEndpointDeleteListener deleteListener;

        public EndpointsAdapter(List<Endpoint> items, OnEndpointTestClickListener listener, OnEndpointDeleteListener deleteListener) {
            this.items = items;
            this.listener = listener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_endpoint_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Endpoint item = items.get(position);
            holder.tvPath.setText(item.path);
            holder.tvDescription.setText(item.description != null && !item.description.isEmpty() 
                    ? item.description 
                    : "No description provided.");
            holder.tvMethodBadge.setText(item.method);

            // Dynamically apply Brutalist background badges + stripes + path text colors
            int accentColor = Color.parseColor("#a8e6cf"); // Green default
            int badgeBg = R.drawable.bg_badge_get;
            int badgeTextColor = Color.parseColor("#2c6957");

            switch (item.method.toUpperCase()) {
                case "POST":
                    accentColor = Color.parseColor("#e2dcfd"); // Lavender
                    badgeBg = R.drawable.bg_badge_post;
                    badgeTextColor = Color.parseColor("#635f7b");
                    break;
                case "PUT":
                    accentColor = Color.parseColor("#fdd1b4"); // Orange
                    badgeBg = R.drawable.bg_badge_put;
                    badgeTextColor = Color.parseColor("#785841");
                    break;
                case "DELETE":
                    accentColor = Color.parseColor("#ffdad6"); // Peach Red
                    badgeBg = R.drawable.bg_badge_delete;
                    badgeTextColor = Color.parseColor("#93000a");
                    break;
            }

            holder.vAccentStripe.setBackgroundColor(accentColor);
            holder.tvMethodBadge.setBackgroundResource(badgeBg);
            holder.tvMethodBadge.setTextColor(badgeTextColor);
            holder.tvPath.setTextColor(badgeTextColor);

            holder.btnTest.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTestClick(item);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(item);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMethodBadge, tvVersion, tvPath, tvDescription;
            MaterialButton btnTest;
            View vAccentStripe;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMethodBadge = itemView.findViewById(R.id.tvMethodBadge);
                tvVersion = itemView.findViewById(R.id.tvVersion);
                tvPath = itemView.findViewById(R.id.tvPath);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                btnTest = itemView.findViewById(R.id.btnTest);
                vAccentStripe = itemView.findViewById(R.id.vAccentStripe);
            }
        }
    }
}
