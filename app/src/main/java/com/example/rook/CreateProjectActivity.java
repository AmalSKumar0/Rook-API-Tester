package com.example.rook;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CreateProjectActivity extends AppCompatActivity {

    private TextInputEditText etProjectName, etDescription;
    private DatabaseHelper dbHelper;
    private String selectedTemplate = "Empty";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        dbHelper = new DatabaseHelper(this);

        etProjectName = findViewById(R.id.etProjectName);
        etDescription = findViewById(R.id.etDescription);

        NavigationUtils.setupAppChrome(this, "New Collection", true);

        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        MaterialButton btnCreateProject = findViewById(R.id.btnCreateProject);

        MaterialButton chipEmpty = findViewById(R.id.chipEmpty);
        MaterialButton chipRest = findViewById(R.id.chipRest);
        MaterialButton chipGraphql = findViewById(R.id.chipGraphql);

        // Chip selection logic for template
        chipEmpty.setOnClickListener(v -> {
            selectedTemplate = "Empty";
            updateChipStyles(chipEmpty, chipRest, chipGraphql);
        });

        chipRest.setOnClickListener(v -> {
            selectedTemplate = "REST API";
            updateChipStyles(chipRest, chipEmpty, chipGraphql);
        });

        chipGraphql.setOnClickListener(v -> {
            selectedTemplate = "GraphQL";
            updateChipStyles(chipGraphql, chipEmpty, chipRest);
        });

        btnCancel.setOnClickListener(v -> finish());

        btnCreateProject.setOnClickListener(v -> saveProject());
    }

    private void updateChipStyles(MaterialButton active, MaterialButton inactive1, MaterialButton inactive2) {
        active.setBackgroundTintList(getResources().getColorStateList(R.color.tertiary_container));
        active.setTextColor(getResources().getColor(R.color.on_tertiary_container));
        active.setStrokeWidth(4);

        // Inactive chips use outline style
        inactive1.setBackgroundTintList(getResources().getColorStateList(R.color.surface_container_lowest));
        inactive1.setTextColor(getResources().getColor(R.color.on_surface_variant));
        inactive1.setStrokeWidth(2);

        inactive2.setBackgroundTintList(getResources().getColorStateList(R.color.surface_container_lowest));
        inactive2.setTextColor(getResources().getColor(R.color.on_surface_variant));
        inactive2.setStrokeWidth(2);
    }

    private void saveProject() {
        String name = etProjectName.getText() != null ? etProjectName.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etProjectName.setError("Collection name is required.");
            etProjectName.requestFocus();
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            long projectId = dbHelper.addProject(name, desc, selectedTemplate);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (projectId != -1) {
                    Toast.makeText(this, "Collection created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ProjectDetailsActivity.class);
                    intent.putExtra("PROJECT_ID", projectId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to create Collection.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
