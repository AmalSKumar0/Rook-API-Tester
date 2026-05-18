package com.example.rook;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyApisActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvCollectionSummary;
    private RecyclerView rvCollections;
    private View emptyCollectionState;
    private CollectionAdapter adapter;
    private final List<CollectionItem> collections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_apis);

        dbHelper = new DatabaseHelper(this);
        NavigationUtils.setupAppChrome(this, "Collections", false);

        tvCollectionSummary = findViewById(R.id.tvCollectionSummary);
        rvCollections = findViewById(R.id.rvCollections);
        emptyCollectionState = findViewById(R.id.emptyCollectionState);

        rvCollections.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CollectionAdapter(collections, this::launchProjectDetails, collection -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Collection")
                    .setMessage("Are you sure you want to delete '" + collection.name + "'? All endpoints in this collection will be lost.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            dbHelper.deleteProject(collection.id);
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                bindProjectsFromDatabase();
                                android.widget.Toast.makeText(this, "Collection deleted", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvCollections.setAdapter(adapter);

        View btnCreateColl = findViewById(R.id.btnCreateCollection);
        if (btnCreateColl != null) {
            btnCreateColl.setOnClickListener(v -> startActivity(new Intent(this, CreateProjectActivity.class)));
        }

        ExtendedFloatingActionButton fab = findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setOnClickListener(v -> startActivity(new Intent(this, CreateProjectActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProjectsFromDatabase();
    }

    private void bindProjectsFromDatabase() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<CollectionItem> newItems = new ArrayList<>();
            Cursor cursor = dbHelper.getAllProjects();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_DESCRIPTION));
                    int endpointCount = dbHelper.getEndpointCountForProject(id);
                    newItems.add(new CollectionItem(id, name, description, endpointCount));
                }
                cursor.close();
            }

            int apiCount = dbHelper.getEndpointCount();
            
            AppExecutors.getInstance().mainThread().execute(() -> {
                collections.clear();
                collections.addAll(newItems);
                tvCollectionSummary.setText(String.format(Locale.US, "%d collections · %d APIs", collections.size(), apiCount));
                boolean isEmpty = collections.isEmpty();
                rvCollections.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyCollectionState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void launchProjectDetails(CollectionItem collection) {
        Intent intent = new Intent(this, ProjectDetailsActivity.class);
        intent.putExtra("PROJECT_ID", collection.id);
        startActivity(intent);
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

    private interface OnCollectionClickListener {
        void onItemClick(CollectionItem collection);
    }

    private interface OnCollectionLongClickListener {
        void onLongClick(CollectionItem collection);
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
            holder.description.setText(item.description != null && !item.description.trim().isEmpty()
                    ? item.description
                    : "No description yet.");
            holder.meta.setText(item.endpointCount == 1 ? "1 API" : item.endpointCount + " APIs");
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(item);
                }
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
}
