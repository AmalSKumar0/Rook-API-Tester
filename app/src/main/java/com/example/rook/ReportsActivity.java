package com.example.rook;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<HistoryItem> historyItems = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);
        NavigationUtils.setupAppChrome(this, "Reports", false);

        RecyclerView rvReportHistory = findViewById(R.id.rvReportHistory);
        rvReportHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyItems, this::openApiDetails);
        rvReportHistory.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindReportData();
    }

    private void bindReportData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int testResultCount = dbHelper.getTestResultCount();
            int total = testResultCount > 0 ? testResultCount : dbHelper.getHistoryCount();
            float successRate = dbHelper.getSuccessRate();
            int failures = dbHelper.getFailedHistoryCount();
            int latency = dbHelper.getAverageLatency();
            int collectionsCount = dbHelper.getProjectCount();
            int endpointsCount = dbHelper.getEndpointCount();

            List<HistoryItem> newItems = new ArrayList<>();
            if (testResultCount > 0) {
                Cursor cursor = dbHelper.getRecentTestResults(50);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_METHOD));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_REQUEST_URL));
                        String apiPath = cursor.getString(cursor.getColumnIndexOrThrow("api_path"));
                        String collectionName = cursor.getString(cursor.getColumnIndexOrThrow("collection_name"));
                        int statusCode = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_STATUS_CODE));
                        int responseTime = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_RESPONSE_TIME));
                        String resultStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_STATUS));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RESULT_TIMESTAMP));
                        String timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
                        String status = statusCode > 0 ? statusCode + " " + resultStatus : "ERROR";
                        String label = !isEmpty(apiPath) ? apiPath : url;
                        String details = (isEmpty(collectionName) ? "Collection" : collectionName) + " · " + responseTime + "ms · " + timeAgo;
                        newItems.add(new HistoryItem(method, url, method + " " + label, status, details, colorsFor(method, status)));
                    }
                    cursor.close();
                }
            } else {
                Cursor cursor = dbHelper.getRecentHistory(20);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_METHOD));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_PATH));
                        String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_STATUS));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_HISTORY_TIMESTAMP));
                        String timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
                        newItems.add(new HistoryItem(method, path, method + " " + path, status, timeAgo, colorsFor(method, status)));
                    }
                    cursor.close();
                }
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                bindMetric(R.id.reportRequests, "REQUESTS", formatCompactCount(total), failures + " failed");
                bindMetric(R.id.reportSuccess, "SUCCESS RATE", String.format(Locale.US, "%.1f%%", successRate), "Across all tests");
                bindMetric(R.id.reportLatency, "AVG LATENCY", latency + "ms", "Mean response time");
                bindMetric(R.id.reportCollections, "COLLECTIONS", String.valueOf(collectionsCount), endpointsCount + " endpoints");

                historyItems.clear();
                historyItems.addAll(newItems);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void bindMetric(int rootId, String label, String value, String subtext) {
        View root = findViewById(rootId);
        if (root == null) return;
        ((TextView) root.findViewById(R.id.tvMetricLabel)).setText(label);
        ((TextView) root.findViewById(R.id.tvMetricValue)).setText(value);
        ((TextView) root.findViewById(R.id.tvMetricSubtext)).setText(subtext);
    }

    private void openApiDetails(HistoryItem item) {
        Intent intent = new Intent(this, TestApiActivity.class);
        intent.putExtra("METHOD", item.method);
        intent.putExtra("PATH", item.path);
        intent.putExtra("URL", guessUrlForPath(item.path));
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

    private static class HistoryItem {
        String method;
        String path;
        String endpoint;
        String status;
        String time;
        StatusColors colors;

        HistoryItem(String method, String path, String endpoint, String status, String time, StatusColors colors) {
            this.method = method;
            this.path = path;
            this.endpoint = endpoint;
            this.status = status;
            this.time = time;
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

    private interface OnHistoryClickListener {
        void onItemClick(HistoryItem item);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryItem> items;
        private final OnHistoryClickListener listener;

        HistoryAdapter(List<HistoryItem> items, OnHistoryClickListener listener) {
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
            HistoryItem item = items.get(position);
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

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEndpoint, tvTime, tvStatusCode;
            View vStatusIndicator;
            MaterialCardView cvStatusBadge, cvStatusDot;

            ViewHolder(@NonNull View itemView) {
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
}
