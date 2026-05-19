package com.example.rook;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvTotalRequests, tvSuccessRate, tvAvgLatency;
    private RecentActivityAdapter recentAdapter;
    private CollectionAdapter collectionAdapter;
    private ReportAdapter reportAdapter;
    private final List<ApiActivity> activityList = new ArrayList<>();
    private final List<CollectionItem> collectionList = new ArrayList<>();
    private final List<ReportItem> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        NavigationUtils.setupAppChrome(this, getString(R.string.app_name), false);

        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvAvgLatency = findViewById(R.id.tvAvgLatency);

        RecyclerView rvRecentActivity = findViewById(R.id.rvRecentActivity);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentActivityAdapter(activityList, this::openApiDetails);
        rvRecentActivity.setAdapter(recentAdapter);

        RecyclerView rvCollections = findViewById(R.id.rvCollections);
        rvCollections.setLayoutManager(new LinearLayoutManager(this));
        collectionAdapter = new CollectionAdapter(collectionList, this::openCollectionDetails, collection -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Collection")
                    .setMessage("Are you sure you want to delete '" + collection.name + "'? All endpoints in this collection will be lost.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            dbHelper.deleteProject(collection.id);
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                loadDashboardData();
                                android.widget.Toast.makeText(this, "Collection deleted", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvCollections.setAdapter(collectionAdapter);

        RecyclerView rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(reportList, report -> openReports());
        rvReports.setAdapter(reportAdapter);

        View openReports = findViewById(R.id.tvOpenReports);
        if (openReports != null) openReports.setOnClickListener(v -> openReports());

        View viewAllRecent = findViewById(R.id.tvViewAllRecent);
        if (viewAllRecent != null) viewAllRecent.setOnClickListener(v -> openReports());

        View viewAllCollections = findViewById(R.id.tvViewAllCollections);
        if (viewAllCollections != null) {
            viewAllCollections.setOnClickListener(v -> startActivity(new Intent(this, MyApisActivity.class)));
        }

        View performanceCard = findViewById(R.id.cardPerformance);
        if (performanceCard != null) performanceCard.setOnClickListener(v -> openReports());

        MaterialButton btnAddCollection = findViewById(R.id.btnAddCollection);
        if (btnAddCollection != null) {
            btnAddCollection.setOnClickListener(v -> startActivity(new Intent(this, CreateProjectActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int testResultCount = dbHelper.getTestResultCount();
            int count = testResultCount > 0 ? testResultCount : dbHelper.getHistoryCount();
            float successRate = dbHelper.getSuccessRate();
            int avgLatency = dbHelper.getAverageLatency();
            
            int projectCount = dbHelper.getProjectCount();
            int endpointCount = dbHelper.getEndpointCount();
            int failedCount = dbHelper.getFailedHistoryCount();

            AppExecutors.getInstance().mainThread().execute(() -> {
                tvTotalRequests.setText(formatCompactCount(count));
                tvSuccessRate.setText(String.format(Locale.US, "%.1f%%", successRate));
                tvAvgLatency.setText(String.format(Locale.US, "Avg response: %dms", avgLatency));

                loadRecentTests();
                loadCollections();
                
                reportList.clear();
                reportList.add(new ReportItem("Test volume", formatCompactCount(count), "Stored SQLite executions", R.drawable.ic_nav_reports));
                reportList.add(new ReportItem("Collections", String.valueOf(projectCount), endpointCount + " saved endpoints", R.drawable.ic_nav_collections));
                reportList.add(new ReportItem("Failures", String.valueOf(failedCount), String.format(Locale.US, "%.1f%% success rate", successRate), R.drawable.ic_nav_reports));
                reportList.add(new ReportItem("Latency", avgLatency + "ms", "Average response time", R.drawable.ic_nav_reports));
                reportAdapter.notifyDataSetChanged();
            });
        });
    }

    private void loadRecentTests() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ApiActivity> newItems = new ArrayList<>();
            if (dbHelper.getTestResultCount() > 0) {
                Cursor cursor = dbHelper.getRecentTestResults(5);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_METHOD));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_REQUEST_URL));
                        String apiPath = cursor.getString(cursor.getColumnIndexOrThrow("api_path"));
                        int statusCode = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_STATUS_CODE));
                        String resultStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_STATUS));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_TIMESTAMP));
                        String label = !isEmpty(apiPath) ? apiPath : url;
                        String status = statusCode > 0 ? statusCode + " " + resultStatus : "ERROR";
                        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                        newItems.add(new ApiActivity(method, url, method + " " + label, timeAgo.toString(), status, colorsFor(method, status)));
                    }
                    cursor.close();
                }
            } else {
                Cursor cursor = dbHelper.getRecentHistory(5);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_METHOD));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_PATH));
                        String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_STATUS));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_TIMESTAMP));
                        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                        newItems.add(new ApiActivity(method, path, method + " " + path, timeAgo.toString(), status, colorsFor(method, status)));
                    }
                    cursor.close();
                }
            }
            AppExecutors.getInstance().mainThread().execute(() -> {
                activityList.clear();
                activityList.addAll(newItems);
                recentAdapter.notifyDataSetChanged();
            });
        });
    }

    private void loadCollections() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<CollectionItem> newItems = new ArrayList<>();
            Cursor cursor = dbHelper.getAllProjects();
            int shown = 0;
            if (cursor != null) {
                while (cursor.moveToNext() && shown < 4) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_DESCRIPTION));
                    int endpointCount = dbHelper.getEndpointCountForProject(id);
                    newItems.add(new CollectionItem(id, name, description, endpointCount));
                    shown++;
                }
                cursor.close();
            }
            AppExecutors.getInstance().mainThread().execute(() -> {
                collectionList.clear();
                collectionList.addAll(newItems);
                collectionAdapter.notifyDataSetChanged();
            });
        });
    }

    private void openApiDetails(ApiActivity activity) {
        Intent intent = new Intent(this, ApiLabActivity.class);
        intent.putExtra("METHOD", activity.method);
        intent.putExtra("PATH", activity.path);
        intent.putExtra("URL", guessUrlForPath(activity.path));
        startActivity(intent);
    }

    private String guessUrlForPath(String path) {
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            return path;
        }
        return "";
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void openCollectionDetails(CollectionItem collection) {
        Intent intent = new Intent(this, ProjectDetailsActivity.class);
        intent.putExtra("PROJECT_ID", collection.id);
        startActivity(intent);
    }

    private void openReports() {
        startActivity(new Intent(this, ReportsActivity.class));
    }

    private String formatCompactCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.US, "%.1fk", count / 1000f);
        }
        return String.valueOf(count);
    }

    private StatusColors colorsFor(String method, String status) {
        String badgeColor = "#a8e6cf";
        String textColor = "#2c6957";
        String dotColor = "#2c6956";

        if (status.startsWith("4") || status.startsWith("5") || status.equalsIgnoreCase("ERROR")) {
            badgeColor = "#ffdad6";
            textColor = "#93000a";
            dotColor = "#ba1a1a";
        } else if (status.startsWith("3")) {
            badgeColor = "#fdd1b4";
            textColor = "#785841";
            dotColor = "#785741";
        } else if (status.startsWith("2") && method.equalsIgnoreCase("POST")) {
            badgeColor = "#e2dcfd";
            textColor = "#635f7b";
            dotColor = "#5f5b77";
        }
        return new StatusColors(badgeColor, textColor, dotColor);
    }

    public static class ApiActivity {
        String method;
        String path;
        String endpoint;
        String time;
        String status;
        StatusColors colors;

        public ApiActivity(String method, String path, String endpoint, String time, String status, StatusColors colors) {
            this.method = method;
            this.path = path;
            this.endpoint = endpoint;
            this.time = time;
            this.status = status;
            this.colors = colors;
        }
    }

    private static class StatusColors {
        String badgeColor;
        String textColor;
        String dotColor;

        StatusColors(String badgeColor, String textColor, String dotColor) {
            this.badgeColor = badgeColor;
            this.textColor = textColor;
            this.dotColor = dotColor;
        }
    }

    private static class CollectionItem {
        long id;
        String name;
        String description;
        int endpointCount;

        CollectionItem(long id, String name, String description, int endpointCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.endpointCount = endpointCount;
        }
    }

    private static class ReportItem {
        String title;
        String value;
        String subtitle;
        int iconRes;

        ReportItem(String title, String value, String subtitle, int iconRes) {
            this.title = title;
            this.value = value;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }

    public interface OnRecentClickListener {
        void onItemClick(ApiActivity activity);
    }

    private interface OnCollectionClickListener {
        void onItemClick(CollectionItem collection);
    }

    private interface OnCollectionLongClickListener {
        void onLongClick(CollectionItem collection);
    }

    private interface OnReportClickListener {
        void onItemClick(ReportItem report);
    }

    public static class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
        private final List<ApiActivity> items;
        private final OnRecentClickListener listener;

        public RecentActivityAdapter(List<ApiActivity> items, OnRecentClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApiActivity item = items.get(position);
            holder.tvEndpoint.setText(item.endpoint);
            holder.tvTime.setText(item.time);
            holder.tvStatusCode.setText(item.status);
            holder.tvStatusCode.setTextColor(Color.parseColor(item.colors.textColor));
            holder.vStatusIndicator.setBackgroundColor(Color.parseColor(item.colors.badgeColor));
            holder.cvStatusBadge.setCardBackgroundColor(Color.parseColor(item.colors.badgeColor));
            holder.cvStatusDot.setCardBackgroundColor(Color.parseColor(item.colors.dotColor));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEndpoint, tvTime, tvStatusCode;
            View vStatusIndicator;
            MaterialCardView cvStatusBadge, cvStatusDot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEndpoint = itemView.findViewById(R.id.tvEndpoint);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvStatusCode = itemView.findViewById(R.id.tvStatusCode);
                vStatusIndicator = itemView.findViewById(R.id.vStatusIndicator);
                cvStatusBadge = itemView.findViewById(R.id.cvStatusBadge);
                cvStatusDot = itemView.findViewById(R.id.cvStatusDot);
            }
        }
    }

    private static class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {
        private final List<CollectionItem> items;
        private final OnCollectionClickListener listener;
        private final OnCollectionLongClickListener longClickListener;

        CollectionAdapter(List<CollectionItem> items, OnCollectionClickListener listener, OnCollectionLongClickListener longClickListener) {
            this.items = items;
            this.listener = listener;
            this.longClickListener = longClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_collection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CollectionItem item = items.get(position);
            holder.name.setText(item.name);
            holder.description.setText(item.description != null && !item.description.isEmpty() ? item.description : "No description yet.");
            holder.meta.setText(item.endpointCount == 1 ? "1 endpoint" : item.endpointCount + " endpoints");
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onLongClick(item);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, description, meta;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvCollectionName);
                description = itemView.findViewById(R.id.tvCollectionDescription);
                meta = itemView.findViewById(R.id.tvCollectionMeta);
            }
        }
    }

    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
        private final List<ReportItem> items;
        private final OnReportClickListener listener;

        ReportAdapter(List<ReportItem> items, OnReportClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReportItem item = items.get(position);
            holder.title.setText(item.title);
            holder.value.setText(item.value);
            holder.subtitle.setText(item.subtitle);
            holder.icon.setImageResource(item.iconRes);
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title, value, subtitle;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.imgReportIcon);
                title = itemView.findViewById(R.id.tvReportTitle);
                value = itemView.findViewById(R.id.tvReportValue);
                subtitle = itemView.findViewById(R.id.tvReportSubtitle);
            }
        }
    }
}
